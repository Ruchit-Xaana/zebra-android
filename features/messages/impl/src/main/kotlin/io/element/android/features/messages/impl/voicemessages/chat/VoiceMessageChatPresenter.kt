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
    private var webSocket: WebSocket? = null
    private val audioStreamer = AudioStreamer()
    private val audioTrack = AudioTrack()


    @Composable
    override fun present(): VoiceMessageChatState {
        val micPermissionState = micPermissionPresenter.present()
        var canStartSession: Boolean by remember { mutableStateOf(true) }
        var audioSessionId: Int? by remember { mutableStateOf(null) }
        var isRecording: Boolean by remember { mutableStateOf(true) }
        var toast: String? by remember { mutableStateOf(null) }

        fun handleEvents(event: VoiceChatEvents) {
            when (event) {
                VoiceChatEvents.Connect -> {
                    Timber.v("Beginning Voice chat session")
                    val permissionGranted = micPermissionState.permissionGranted
                    when {
                        permissionGranted -> {
                            try{
                                canStartSession=false
                                webSocket=connectWebSocket{message->toast=message}
                                audioStreamer.initAudioRecord()
                                audioTrack.initAudioTrack(listener = object : AudioTrackPlaybackListener {
                                    override fun onAudioPlaying() {
                                        audioStreamer.setMicSilence(true)
                                        audioSessionId=audioTrack.audioSessionId
                                        isRecording=false
                                    }

                                    override fun onSilenceDetected() {
                                        audioStreamer.setMicSilence(false)
                                        audioSessionId=null
                                        isRecording=true
                                    }
                                })
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
                    Timber.v("Exiting Voice chat session")
                    webSocket?.close(1000, "Normal Closure")
                    webSocket=null
                    Timber.v("Mic stop recording")
                    audioTrack.stopPlayback()
                    audioStreamer.stopRecording()
                    canStartSession=true
                    audioSessionId=null
                    isRecording=true
                    toast=null
                }
            }
        }
        return VoiceMessageChatState(
            canStartSession = canStartSession,
            audioSessionId = audioSessionId,
            isRecording = isRecording,
            toastMessage = toast,
            eventSink = { handleEvents(it) },
        )
    }
    private fun connectWebSocket(setToast: (String?) -> Unit):WebSocket {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://3.106.211.173:32340/ws/audio")
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
                setToast("WebSocket error: ${t.message}")
                Timber.e("WebSocket error: ${t.message}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Timber.i("WebSocket closing: $code / $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                setToast("WebSocket closed with code: $code")
                Timber.i("WebSocket closed: $code / $reason")
            }
        })
    }
}


