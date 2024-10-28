/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

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
import kotlinx.coroutines.launch
import timber.log.Timber

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

    @Composable
    override fun present(): FileSelectorState {
        val coroutineScope = rememberCoroutineScope()
        var documents by remember { mutableStateOf<List<File>>(emptyList()) }
        val filesPicker = mediaPickerProvider.registerFilePicker(Any) { uri ->
           Log.d("FileSelectorPresenter", "Selected file: $uri")
        }

        fun handleEvents(event: FileSelectorEvents) {
            when (event) {
                is FileSelectorEvents.PickAttachmentSource.FromFiles -> {
                    filesPicker.launch()
                }
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
            }
        }

        return FileSelectorState(
            documents = documents,
            eventSink = { handleEvents(it) },
        )
    }
}
