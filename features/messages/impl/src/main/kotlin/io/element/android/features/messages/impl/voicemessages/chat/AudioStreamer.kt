/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.voicemessages.chat

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.annotation.RequiresPermission
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import okhttp3.WebSocket
import okio.ByteString.Companion.toByteString
import timber.log.Timber
import kotlin.concurrent.thread

@SingleIn(RoomScope::class)
class AudioStreamer {
    private val sampleRate = 24000
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private var audioRecord : AudioRecord?=null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var setSilence: Boolean = false
    private var isRecording = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initAudioRecord() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        val audioSessionId = audioRecord?.audioSessionId ?: -1

        // Initialize and enable Acoustic Echo Canceler (AEC)
        if (AcousticEchoCanceler.isAvailable() && audioSessionId != -1) {
            echoCanceler = AcousticEchoCanceler.create(audioSessionId)
            echoCanceler?.enabled = true
            Timber.i("Acoustic Echo Canceler enabled")
        } else {
            Timber.i("Acoustic Echo Canceler not available")
        }

        // Initialize and enable Noise Suppression (NS)
        if (NoiseSuppressor.isAvailable() && audioSessionId != -1) {
            noiseSuppressor = NoiseSuppressor.create(audioSessionId)
            noiseSuppressor?.enabled = true
            Timber.i("Noise Suppressor enabled")
        } else {
            Timber.i("Noise Suppressor not available")
        }
    }

        fun startRecording(webSocket: WebSocket) {
        isRecording = true
        audioRecord?.startRecording()

            thread {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    if(setSilence){
                        Log.d("AudioStreamer","Sending silence")
                        val silenceChunk = ByteArray(bufferSize)
                        val silenceFlag =webSocket.send(silenceChunk.toByteString(0, silenceChunk.size))
                        Log.d("Flag","Silence ${silenceFlag}")
                    }
                    else {
                        Log.d("AudioStreamer","Sending audio")
                        val readSize = audioRecord?.read(buffer, 0, buffer.size)
                        if (readSize != null) {
                            if (readSize > 0) {
                                val audioFlag = webSocket.send(buffer.toByteString(0, readSize))
                                Log.d("Flag","Audio ${audioFlag}")
                            }
                        }
                    }
                }
            }
        }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
    }
    fun setMicSilence(isSilence: Boolean) {
        setSilence = isSilence
    }
}
