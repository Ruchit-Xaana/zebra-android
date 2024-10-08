/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.voicemessages.chat

import android.Manifest
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.appconfig.AuthenticationConfig.BOT_API_URL
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.features.messages.impl.timeline.stream.VoiceBotStream
import io.element.android.features.messages.impl.timeline.stream.VoiceBotStreamListener
import io.element.android.features.messages.impl.voicemessages.VoiceMessageException
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.permissions.api.PermissionsEvents
import io.element.android.libraries.permissions.api.PermissionsPresenter
import io.element.android.libraries.voicerecorder.api.AudioPlaybackListener
import io.element.android.libraries.voicerecorder.api.SpeechRecognitionListener
import io.element.android.libraries.voicerecorder.impl.DefaultAudioPlayer
import io.element.android.libraries.voicerecorder.impl.DefaultAudioRecorder
import io.element.android.services.analytics.api.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@SingleIn(RoomScope::class)
class VoiceMessageChatPresenter @Inject constructor(
    private val room: MatrixRoom,
    private val analyticsService: AnalyticsService,
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val timelineController: TimelineController,
    private val audioRecorder: DefaultAudioRecorder,
    private val audioSpeaker : DefaultAudioPlayer
) : Presenter<VoiceMessageChatState> {
    private val micPermissionPresenter = permissionsPresenterFactory.create(Manifest.permission.RECORD_AUDIO)


    @Composable
    override fun present(): VoiceMessageChatState {
        val localCoroutineScope = rememberCoroutineScope()
        val micPermissionState = micPermissionPresenter.present()
        var enableRecording: Boolean by remember { mutableStateOf(true) }
        var audioSessionId: Int? by remember { mutableStateOf(null) }
        var rmsDB: Float? by remember { mutableStateOf(null) }
        var isReady: Boolean by remember { mutableStateOf(false) }
        var error: String? by remember { mutableStateOf(null) }

        fun handleEvents(event: VoiceChatEvents) {
            when (event) {
                VoiceChatEvents.Start -> {
                    Timber.v("Voice chat Started")
                    val permissionGranted = micPermissionState.permissionGranted
                    when {
                        permissionGranted -> {
                            localCoroutineScope.startRecording(object : SpeechRecognitionListener {

                                override fun onRmsChanged(rmsdBValue: Float) {
                                    rmsDB = rmsdBValue
                                }

                                override fun onReadyForSpeech() {
                                    isReady = true // Speech input started, show "Speak now" message
                                    error = null
                                }

                                override fun onTextRecognized(recognizedText: String) {
                                    enableRecording = false
                                    isReady = false
                                    sendMessageToRoom(room, recognizedText,object : VoiceBotStreamListener {
                                        override fun onStreamError(errorMessage: String) {
                                            error = errorMessage
                                            enableRecording = true
                                        }
                                    }){ flag, newAudioSessionId ->
                                        enableRecording = flag
                                        if (newAudioSessionId != null) {
                                            audioSessionId = newAudioSessionId
                                        }
                                    }
                                }
                                override fun onEndOfSpeech() {
                                    isReady = false  // Speech input ended, hide "Speak now" message
                                }
                                override fun onError(error: Int) {
                                    enableRecording = true
                                    isReady = false
                                }
                            })
                        }
                        else -> {
                            Timber.i("Microphone permission needed")
                            micPermissionState.eventSink(PermissionsEvents.RequestPermissions)
                        }
                    }
                }
            }
        }
        return VoiceMessageChatState(
            enableRecording = enableRecording,
            audioSessionId = audioSessionId,
            rmsDB = rmsDB,
            errorMessage = error,
            isReady = isReady,
            eventSink = { handleEvents(it) },
        )
    }

    private fun CoroutineScope.startRecording(callback: SpeechRecognitionListener) = launch {
        try {
            audioRecorder.startRecording(object : SpeechRecognitionListener {
                override fun onReadyForSpeech() {
                    callback.onReadyForSpeech()  // Send the RMS change to the callback
                }
                override fun onTextRecognized(recognizedText: String) {
                    // Handle the recognized text here
                    callback.onTextRecognized(recognizedText)
                    audioRecorder.stopRecording()
                }

                override fun onError(error: Int) {
                    // Handle error here
                    callback.onError(error)
                    Timber.e("Speech recognition error: $error")
                    audioRecorder.stopRecording()
                }

                override fun onRmsChanged(rmsDB: Float) {
                    callback.onRmsChanged(rmsDB)  // Send the RMS change to the callback
                }

                override fun onEndOfSpeech() {
                    callback.onEndOfSpeech()
                }
            })
        } catch (e: SecurityException) {
            Timber.e(e, "Audio Capture error")
            analyticsService.trackError(VoiceMessageException.PermissionMissing("Expected permission to record but none", e))
        }
    }

    private fun sendMessageToRoom(room: MatrixRoom, recognizedText: String,streamListener: VoiceBotStreamListener,onPlaybackCompleted: (flag:Boolean, audioSessionId: Int?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                room.sendMessage("Init from homepage...", "<p>${recognizedText}</p>", emptyList())
                val currentUserId = room.sessionId
                val currentRoomId = room.roomId
                val messageEventId = waitForEventId("<p>${recognizedText}</p>")
                if(messageEventId!=null) {
                    val payload = JSONObject().apply {
                        put("query", recognizedText)
                        put("eventId", messageEventId)
                        put("user_id", currentUserId)
                    }
                    val botApiUrl = "$BOT_API_URL/stream_audio/$currentRoomId"
                    val zebraStream = VoiceBotStream(audioSpeaker)
                    zebraStream.startJsonStream(botApiUrl, payload,object : AudioPlaybackListener {
                        override fun onPlaying(audioSessionId: Int) {
                            onPlaybackCompleted(false,audioSessionId)
                        }

                        override fun onPlaybackCompleted() {
                            Log.d("BotStream","Playback completed in voice message presenter")
                            onPlaybackCompleted(true,null)
                        }

                        override fun onPlaybackError(errorMessage: String) {
                            Log.d("BotStream","Error Got in voice message presenter")
                            streamListener.onStreamError("Playback error: $errorMessage")
                            onPlaybackCompleted(true,null)
                        }
                    },streamListener)
                }
            } catch (e: Exception) {
                Timber.e(e.message)
            }
        }
    }
    private suspend fun waitForEventId(formattedBody: String): String? {
        return withTimeoutOrNull(5000L) {
            var eventId: String? = null
            while (eventId == null) {
                eventId = timelineController.getEventIdFromFormattedBody(formattedBody)
                if (eventId == null) {
                    delay(500)
                }
            }
            eventId
        }
    }
}


