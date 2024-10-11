/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.voicemessages.chat


import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AudioTrack(
    private val sampleRate: Int = 24000,
    private val channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
) {
    var audioSessionId: Int? = null
    private var listener: AudioTrackPlaybackListener? = null
    private var audioTrack: AudioTrack? = null
    private var minBufferSize: Int = 0
    private val audioQueue: BlockingQueue<ByteArray> = LinkedBlockingQueue()
    private var isPlaying = false


    fun initAudioTrack(listener: AudioTrackPlaybackListener) {
        this.listener = listener
        minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)*4
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(AudioFormat.Builder()
            .setEncoding(audioFormat)
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .build())
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        audioSessionId = audioTrack?.audioSessionId
        isPlaying = false
        audioQueue.clear()
    }

    fun playAudioChunk(audioChunk: ByteArray) {
        audioQueue.offer(audioChunk)
        if (!isPlaying) {
            isPlaying = true
            Thread { processAudioQueue() }.start()
        }
    }
    private fun processAudioQueue() {
        while (isPlaying) {
            val audioChunk = audioQueue.poll(1, TimeUnit.SECONDS)
            if (audioChunk == null) {
                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    listener?.onSilenceDetected()
                    audioTrack?.pause()
                }
            } else {

                val chunks = if (audioChunk.size > minBufferSize) {
                    audioChunk.toList().chunked(minBufferSize).map { it.toByteArray() }
                } else {
                    listOf(audioChunk)
                }

                for (chunk in chunks) {
                    if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                        audioTrack?.write(chunk, 0, chunk.size)
                    }
                }
                if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    listener?.onAudioPlaying()
                    audioTrack?.play()
                }
            }
        }
    }
    fun stopPlayback() {
        isPlaying = false
        audioQueue.clear()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}
