/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import android.os.Environment
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.mediapickers.api.PickerProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileSelectorPresenter @AssistedInject constructor(
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
    private val allowedExtensions = listOf(".pdf", ".docx", ".doc", ".xlsx", ".xls", ".odt", ".rtf", ".csv", ".ods")

    private fun isAllowedFileType(fileName: String): Boolean {
        return allowedExtensions.any { fileName.endsWith(it, ignoreCase = true) }
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
                Log.e( "FileSelectorPresenter", "Error saving file $fileName", e)
            }
        }
    }
    @Composable
    override fun present(): FileSelectorState {
        val coroutineScope = rememberCoroutineScope()
        var documents by remember { mutableStateOf<List<MatrixFile>>(emptyList()) }
        var downloadComplete by remember { mutableStateOf(false) }
        val filesPicker = mediaPickerProvider.registerFilePicker(Any) { uri ->
            Log.d("FileSelectorPresenter", "Selected file: $uri")
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
                        downloadComplete = true
                    }
                }
                FileSelectorEvents.ResetDownloadComplete -> {
                    downloadComplete = false
                }
                FileSelectorEvents.UploadFiles -> {
                    filesPicker.launch()
                }
                FileSelectorEvents.DeleteFiles -> TODO()
            }
        }

        return FileSelectorState(
            documents = documents,
            downloadComplete = downloadComplete,
            eventSink = { handleEvents(it) },
        )
    }
}
