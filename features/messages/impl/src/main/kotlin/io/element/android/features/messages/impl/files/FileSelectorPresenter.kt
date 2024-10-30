/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.mimetype.MimeTypes.Any
import io.element.android.libraries.designsystem.utils.snackbar.SnackbarDispatcher
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.mediapickers.api.PickerProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileSelectorPresenter @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val room: MatrixRoom,
    @Assisted private val inputs: Inputs,
    private val mediaPickerProvider: PickerProvider,
    private val fileOpsHandler: FileOpsHandler,
    private val snackbarDispatcher: SnackbarDispatcher,
) : Presenter<FileSelectorState> {
    data class Inputs(
        val senderId: UserId,
    )

    @AssistedFactory
    interface Factory {
        fun create(inputs: Inputs): FileSelectorPresenter
    }

    @Composable
    override fun present(): FileSelectorState {
        val coroutineScope = rememberCoroutineScope()
        var documents by remember { mutableStateOf<List<MatrixFile>>(emptyList()) }
        var completedFileOp by remember { mutableStateOf(false) }
        var isBusy by remember { mutableStateOf(false) }
        var progressInt by remember { mutableIntStateOf(0) }
        val filesPicker = mediaPickerProvider.registerFilePicker(Any){ uri ->
            if (uri == null) return@registerFilePicker
            val file = convertUriToFile(uri)
            if (file != null) {
                coroutineScope.launch {
                    try {
                        uploadFileViaWebSocket(file, onProgressUpdate = { progress -> progressInt = progress }, onSetBusy = { flag ->
                            isBusy = flag
                            if (flag) { progressInt = 0 }
                        }, onError = { e-> Timber.tag("FileSelectorPresenter").d("Error: %s", e)}, onCompleted = {completedFileOp = true})
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to upload file: " + file.name)
                    }
                }
            } else {
                Timber.e("Failed to convert URI to file.")
            }
        }

        fun handleEvents(event: FileSelectorEvents) {
            when (event) {
                FileSelectorEvents.FetchFiles -> {
                    coroutineScope.launch {
                        try {
                            val files = fileOpsHandler.listFiles(userId = inputs.senderId.toString())
                            Timber.tag("FileSelectorPresenter").d("Fetched files: %s", files)
                            val fetchedFiles = files.map { fileDto ->
                                fileOpsHandler.dtoToFileAdapters(fileDto, inputs.senderId.toString())
                            }
                            documents= fetchedFiles.filter { f -> f.mimetype != null && !f.mimetype.startsWith("image/") }
                        } catch (e: Exception) {
                            Timber.tag("FileSelectorPresenter").e(e, "Failed to fetch files")
                        }
                    }
                }
                is FileSelectorEvents.DownloadFiles -> {
                    val filesToDownload = event.matrixFiles
                    val userId = inputs.senderId.toString()

                    coroutineScope.launch {
                        filesToDownload.forEach { file ->
                            try {
                                downloadFile(file.mediaId, userId, file.name)
                            } catch (e: Exception) {
                                Timber.tag("FileSelectorPresenter").e(e, "Failed to download file: ${file.name}")
                            }
                        }
                        completedFileOp = true
                    }
                }
                is FileSelectorEvents.DeleteFiles -> {
                    val filesToDelete = event.matrixFiles

                    coroutineScope.launch {
                        filesToDelete.forEach { file ->
                            try {
                                deleteFile(file)
                            } catch (e: Exception) {
                                Timber.tag("FileSelectorPresenter").e(e, "Failed to download file: ${file.name}")
                            }
                        }
                        completedFileOp = true
                    }

                }
                FileSelectorEvents.ResetSelection -> {
                    completedFileOp = false
                }
                FileSelectorEvents.UploadFiles -> {
                    filesPicker.launch()
                }
            }
        }

        return FileSelectorState(
            documents = documents,
            completedFileOp = completedFileOp,
            isBusy = isBusy,
            progress = progressInt,
            eventSink = { handleEvents(it) },
        )
    }

    private suspend fun deleteFile(file: MatrixFile) {
        val userId = inputs.senderId.toString()
        try {
            val deleteResult = fileOpsHandler.deleteFile(file.mediaId, userId)
            val deleteMessage = deleteResult.optString("message")
            if (deleteMessage == "File deleted successfully from Local DB and S3") {
                val roomId = file.roomId
                val eventId = file.event
                if (roomId != null && eventId != null) {
                    Timber.tag("FileSelectorPresenter").d("Deleting file from room: $roomId, event: $eventId")
                }
            }
        } catch (e: IOException) {
            Timber.tag("FileSelectorPresenter").e(e, "Failed to delete file with IOException: ${e.message}")
        }
        catch (e: Exception) {
            Timber.tag("FileSelectorPresenter").e(e, "Failed to delete file: ${file.name}")
        }

    }

    private suspend fun downloadFile(mediaId: String, userId: String, fileName: String) {
        val fileBytes = fileOpsHandler.getFile(mediaId, userId)

        withContext(Dispatchers.IO) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) ?: return@withContext
            val file = File(downloadsDir, fileName)

            try {
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(fileBytes)
                }
            } catch (e: IOException) {
                Timber.tag("FileSelectorPresenter").e(e, "Error saving file %s", fileName)
            }
        }
    }

    private suspend fun uploadFileViaWebSocket(file: File,onProgressUpdate: (Int) -> Unit, onSetBusy: (Boolean) -> Unit, onError: (String) -> Unit, onCompleted: () -> Unit) {
        val userId = inputs.senderId.toString()

        fileOpsHandler.uploadFilesWebSocket(
            userId = userId,
            files = listOf(file),
            onProgressUpdate = onProgressUpdate,
            onSetBusy = onSetBusy,
            onError = onError,
            onCompleted = onCompleted
        )
    }

    /**
     * Convert Uri to File
     */
    private fun convertUriToFile(uri: Uri): File? {
        return try {
            val fileName = getFileName(uri)
            val originalFile = File(context.cacheDir, fileName)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { fileDescriptor ->
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                FileOutputStream(originalFile).use { output ->
                    inputStream.copyTo(output)
                }
                originalFile
            }
        } catch (e: Exception) {
            Timber.tag("FileSelectorPresenter").e(e, "Error converting URI to file")
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name.ifEmpty { "uploaded_file" }
    }

}
