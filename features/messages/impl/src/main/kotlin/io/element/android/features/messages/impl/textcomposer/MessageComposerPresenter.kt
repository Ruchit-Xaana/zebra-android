/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.messages.impl.textcomposer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.media3.common.MimeTypes
import io.element.android.features.messages.impl.attachments.Attachment
import io.element.android.features.messages.impl.media.local.LocalMediaFactory
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.core.data.StableCharSequence
import io.element.android.libraries.core.data.toStableCharSequence
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.featureflag.api.FeatureFlagService
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.mediapickers.PickerProvider
import io.element.android.libraries.textcomposer.MessageComposerMode
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@SingleIn(RoomScope::class)
class MessageComposerPresenter @Inject constructor(
    private val appCoroutineScope: CoroutineScope,
    private val room: MatrixRoom,
    private val mediaPickerProvider: PickerProvider,
    private val featureFlagService: FeatureFlagService,
    private val localMediaFactory: LocalMediaFactory,
) : Presenter<MessageComposerState> {

    @SuppressLint("UnsafeOptInUsageError")
    @Composable
    override fun present(): MessageComposerState {
        val localCoroutineScope = rememberCoroutineScope()

        val attachmentsState = remember {
            mutableStateOf<AttachmentsState>(AttachmentsState.None)
        }

        fun handlePickedMedia(uri: Uri?, mimeType: String? = null) {
            val localMedia = localMediaFactory.createFromUri(uri, mimeType)
            attachmentsState.value = if (localMedia == null) {
                AttachmentsState.None
            } else {
                val mediaAttachment = Attachment.Media(localMedia)
                AttachmentsState.Previewing(persistentListOf(mediaAttachment))
            }
        }

        val galleryMediaPicker = mediaPickerProvider.registerGalleryPicker(onResult = { handlePickedMedia(it) })
        val filesPicker = mediaPickerProvider.registerFilePicker(onResult = { handlePickedMedia(it) })
        val cameraPhotoPicker = mediaPickerProvider.registerCameraPhotoPicker(onResult = { handlePickedMedia(it, MimeTypes.IMAGE_JPEG) }, deleteAfter = false)
        val cameraVideoPicker = mediaPickerProvider.registerCameraVideoPicker(onResult = { handlePickedMedia(it, MimeTypes.VIDEO_MP4) }, deleteAfter = false)

        val isFullScreen = rememberSaveable {
            mutableStateOf(false)
        }
        val text: MutableState<StableCharSequence> = remember {
            mutableStateOf(StableCharSequence(""))
        }
        val composerMode: MutableState<MessageComposerMode> = rememberSaveable {
            mutableStateOf(MessageComposerMode.Normal(""))
        }

        var attachmentSourcePicker: AttachmentSourcePicker? by remember { mutableStateOf(null) }

        LaunchedEffect(composerMode.value) {
            when (val modeValue = composerMode.value) {
                is MessageComposerMode.Edit -> text.value = modeValue.defaultContent.toStableCharSequence()
                else -> Unit
            }
        }

        fun handleEvents(event: MessageComposerEvents) {
            when (event) {
                MessageComposerEvents.ToggleFullScreenState -> isFullScreen.value = !isFullScreen.value
                is MessageComposerEvents.UpdateText -> text.value = event.text.toStableCharSequence()
                MessageComposerEvents.CloseSpecialMode -> {
                    text.value = "".toStableCharSequence()
                    composerMode.setToNormal()
                }

                is MessageComposerEvents.SendMessage -> appCoroutineScope.sendMessage(event.message, composerMode, text)
                is MessageComposerEvents.SetMode -> composerMode.value = event.composerMode
                MessageComposerEvents.AddAttachment -> localCoroutineScope.ifMediaPickersEnabled {
                    attachmentSourcePicker = AttachmentSourcePicker.AllMedia
                }
                MessageComposerEvents.DismissAttachmentMenu -> attachmentSourcePicker = null
                MessageComposerEvents.PickAttachmentSource.FromGallery -> localCoroutineScope.ifMediaPickersEnabled {
                    attachmentSourcePicker = null
                    galleryMediaPicker.launch()
                }
                MessageComposerEvents.PickAttachmentSource.FromFiles -> localCoroutineScope.ifMediaPickersEnabled {
                    attachmentSourcePicker = null
                    filesPicker.launch()
                }
                MessageComposerEvents.PickAttachmentSource.FromCamera -> localCoroutineScope.ifMediaPickersEnabled {
                    attachmentSourcePicker = AttachmentSourcePicker.Camera
                }
                MessageComposerEvents.PickCameraAttachmentSource.Photo -> localCoroutineScope.ifMediaPickersEnabled {
                    attachmentSourcePicker = null
                    cameraPhotoPicker.launch()
                }
                MessageComposerEvents.PickCameraAttachmentSource.Video -> localCoroutineScope.ifMediaPickersEnabled {
                    attachmentSourcePicker = null
                    cameraVideoPicker.launch()
                }
            }
        }

        return MessageComposerState(
            text = text.value,
            isFullScreen = isFullScreen.value,
            mode = composerMode.value,
            attachmentSourcePicker = attachmentSourcePicker,
            attachmentsState = attachmentsState.value,
            eventSink = ::handleEvents
        )
    }

    private fun CoroutineScope.ifMediaPickersEnabled(action: suspend () -> Unit) = launch {
        if (featureFlagService.isFeatureEnabled(FeatureFlags.ShowMediaUploadingFlow)) {
            action()
        }
    }

    private fun MutableState<MessageComposerMode>.setToNormal() {
        value = MessageComposerMode.Normal("")
    }

    private fun CoroutineScope.sendMessage(text: String, composerMode: MutableState<MessageComposerMode>, textState: MutableState<StableCharSequence>) =
        launch {
            val capturedMode = composerMode.value
            // Reset composer right away
            textState.value = "".toStableCharSequence()
            composerMode.setToNormal()
            when (capturedMode) {
                is MessageComposerMode.Normal -> room.sendMessage(text)
                is MessageComposerMode.Edit -> room.editMessage(
                    capturedMode.eventId,
                    text
                )

                is MessageComposerMode.Quote -> TODO()
                is MessageComposerMode.Reply -> room.replyMessage(
                    capturedMode.eventId,
                    text
                )
            }
        }
}
