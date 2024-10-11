/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.voicemessages.chat

import android.Manifest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.element.android.features.messages.impl.timeline.TimelineController
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.permissions.api.PermissionsEvents
import io.element.android.libraries.permissions.api.PermissionsPresenter
import io.element.android.libraries.voicerecorder.impl.DefaultAudioPlayer
import io.element.android.libraries.voicerecorder.impl.DefaultAudioRecorder
import io.element.android.services.analytics.api.AnalyticsService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber
import javax.inject.Inject

@SingleIn(RoomScope::class)
class VoiceMessageChatPresenter @Inject constructor(
    private val room: MatrixRoom,
    private val analyticsService: AnalyticsService,
    permissionsPresenterFactory: PermissionsPresenter.Factory,
    private val timelineController: TimelineController,
    private val audioRecorder: DefaultAudioRecorder,
    private val audioSpeaker : DefaultAudioPlayer,
) : Presenter<VoiceMessageChatState> {
    private val micPermissionPresenter = permissionsPresenterFactory.create(Manifest.permission.RECORD_AUDIO)
    private var audioStreamer = AudioStreamer()
    private val audioTrack = AudioTrack(listener = object : AudioTrackPlaybackListener {
        override fun onAudioPlaying() {
            audioStreamer.setMicSilence(true)
        }

        override fun onSilenceDetected() {
            audioStreamer.setMicSilence(false)
        }
    })
    private var webSocket: WebSocket? = null

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
                VoiceChatEvents.Connect -> {
                    if(webSocket!=null)webSocket=connectWebSocket()
                    val permissionGranted = micPermissionState.permissionGranted
                    when {
                        permissionGranted -> {
                            try{
                                audioStreamer.initAudioRecord()
                                audioStreamer.startRecording(webSocket!!)
                            }
                            catch (e:SecurityException){
                                Timber.e(e, "Mic permissions not added")
                            }

                        }
                        else -> {
                            Timber.i("Microphone permission needed")
                            micPermissionState.eventSink(PermissionsEvents.RequestPermissions)
                        }
                    }
                }
                VoiceChatEvents.Disconnect -> {
                    webSocket?.close(1000, "Normal Closure")
                    webSocket=null
                    audioStreamer.stopRecording()
                    audioTrack.stopPlayback()
                }
                VoiceChatEvents.Exit -> {
                    webSocket?.close(1000, "Normal Closure")
                    webSocket=null
                }
                VoiceChatEvents.Start -> {
                    Timber.v("Voice chat Started")
                    val permissionGranted = micPermissionState.permissionGranted
                    when {
                        permissionGranted -> {
                            try{
                                webSocket=connectWebSocket()
                                audioStreamer.initAudioRecord()
                                audioStreamer.startRecording(webSocket!!)
                            }
                            catch (e:SecurityException){
                                Timber.e(e, "Mic permissions not added")
                            }

                        }
                        else -> {
                            Timber.i("Microphone permission needed")
                            micPermissionState.eventSink(PermissionsEvents.RequestPermissions)
                        }
                    }
                }
                VoiceChatEvents.Stop -> {
                    Timber.v("Voice chat Stopped")
                    audioStreamer.stopRecording()
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

//    private fun CoroutineScope.startRecording(callback: SpeechRecognitionListener) = launch {
//        try {
//            audioRecorder.startRecording(object : SpeechRecognitionListener {
//                override fun onReadyForSpeech() {
//                    callback.onReadyForSpeech()  // Send the RMS change to the callback
//                }
//                override fun onTextRecognized(recognizedText: String) {
//                    // Handle the recognized text here
//                    callback.onTextRecognized(recognizedText)
//                    audioRecorder.stopRecording()
//                }
//
//                override fun onError(error: Int) {
//                    // Handle error here
//                    callback.onError(error)
//                    Timber.e("Speech recognition error: $error")
//                    audioRecorder.stopRecording()
//                }
//
//                override fun onRmsChanged(rmsDB: Float) {
//                    callback.onRmsChanged(rmsDB)  // Send the RMS change to the callback
//                }
//
//                override fun onEndOfSpeech() {
//                    callback.onEndOfSpeech()
//                }
//            })
//        } catch (e: SecurityException) {
//            Timber.e(e, "Audio Capture error")
//            analyticsService.trackError(VoiceMessageException.PermissionMissing("Expected permission to record but none", e))
//        }
//    }
//
//    private fun sendMessageToRoom(room: MatrixRoom, recognizedText: String,streamListener: VoiceBotStreamListener,onPlaybackCompleted: (flag:Boolean, audioSessionId: Int?) -> Unit) {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                room.sendMessage("Init from homepage...", "<p>${recognizedText}</p>", emptyList())
//                val currentUserId = room.sessionId
//                val currentRoomId = room.roomId
//                val messageEventId = waitForEventId("<p>${recognizedText}</p>")
//                if(messageEventId!=null) {
//                    val payload = JSONObject().apply {
//                        put("query", recognizedText)
//                        put("eventId", messageEventId)
//                        put("user_id", currentUserId)
//                    }
//                    val botApiUrl = "$BOT_API_URL/stream_audio/$currentRoomId"
//                    val zebraStream = VoiceBotStream(audioSpeaker)
//                    zebraStream.startJsonStream(botApiUrl, payload,object : AudioPlaybackListener {
//                        override fun onPlaying(audioSessionId: Int) {
//                            onPlaybackCompleted(false,audioSessionId)
//                        }
//
//                        override fun onPlaybackCompleted() {
//                            Log.d("BotStream","Playback completed in voice message presenter")
//                            onPlaybackCompleted(true,null)
//                        }
//
//                        override fun onPlaybackError(errorMessage: String) {
//                            Log.d("BotStream","Error Got in voice message presenter")
//                            streamListener.onStreamError("Playback error: $errorMessage")
//                            onPlaybackCompleted(true,null)
//                        }
//                    },streamListener)
//                }
//            } catch (e: Exception) {
//                Timber.e(e.message)
//            }
//        }
//    }
//    private suspend fun waitForEventId(formattedBody: String): String? {
//        return withTimeoutOrNull(5000L) {
//            var eventId: String? = null
//            while (eventId == null) {
//                eventId = timelineController.getEventIdFromFormattedBody(formattedBody)
//                if (eventId == null) {
//                    delay(500)
//                }
//            }
//            eventId
//        }
//    }
    private fun connectWebSocket():WebSocket {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://127.0.0.1:8888/ws/audio")
            .build()

        Timber.d("Connecting to WebSocket")
        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.i("WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Timber.i("Received audio data rom the backend of size: ${bytes.size} bytes")
                audioTrack.playAudioChunk(bytes.toByteArray())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e("WebSocket error: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Timber.i("WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.i("WebSocket closed: $code / $reason")
            }
        })
    }
}


