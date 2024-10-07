/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */
package io.element.android.libraries.voicerecorder.impl

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import io.element.android.libraries.di.ApplicationContext
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.voicerecorder.api.AudioPlaybackListener
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject

@SingleIn(RoomScope::class)
class DefaultAudioPlayer @OptIn(UnstableApi::class)
@Inject constructor(@ApplicationContext private val context: Context) {

    private lateinit var exoPlayer: ExoPlayer
    private val audioQueue: Queue<ByteArray> = LinkedList()
    private var chunkNumber =1
    var playbackCallback: AudioPlaybackListener? = null

    init {
        initializeExoPlayer()
        chunkNumber = 1
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    Log.d("AudioPlayerDEF", "Playback ended, checking for more audio in the queue.")
                    if (audioQueue.isNotEmpty()) {
                        playBufferedAudio() // Play next audio chunk
                    } else {
                        Log.d("AudioPlayerDEF", "Queue is empty, stopping playback.")
                        exoPlayer.stop()
                        playbackCallback?.onPlaybackCompleted()
                    }
                }
                else{
                    if(playbackState == Player.STATE_READY&&exoPlayer.playWhenReady){
                        Log.d("AudioPlayerDEF", "Now Playing")
                        playbackCallback?.onPlaying(exoPlayer.audioSessionId)
                    }
                }
            }
        })
    }
    private fun initializeExoPlayer() {
        exoPlayer = ExoPlayer.Builder(context).build()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer.setAudioAttributes(audioAttributes, true)
        exoPlayer.playWhenReady = true
    }

    // Base64 audio data playback using ExoPlayer
    fun playAudioFromBytes(audioData: ByteArray) {
        audioQueue.offer(audioData)
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            playBufferedAudio() // Start playback if player is idle
        }
    }
    private fun playBufferedAudio() {
        if (audioQueue.isEmpty()) {
            Log.d("AudioPlayerDEF", "Audio queue is empty, nothing to play.")
            return
        }

        val audio = audioQueue.poll()!!

        // Create a temp file with the audio data
        val tempFile = createTempFile(suffix = ".mp3")
        tempFile.writeBytes(audio)

        // Prepare and play the next audio chunk
        val mediaItem = MediaItem.fromUri(tempFile.absolutePath)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        Log.d("AudioPlayerDEF", "Playing chunk number ${chunkNumber++}")
        exoPlayer.play()
    }
}



