/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.stream

import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.core.text.getSpans
import androidx.core.text.util.LinkifyCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.TimeUnit

interface AudioChunkListener {
    fun onAudioChunkReceived(audioData: ByteArray)
}




class BotStream {
    private val client: OkHttpClient =
        OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).protocols(
            listOf(Protocol.HTTP_1_1)
        ).build()
    private val streamingClient: OkHttpClient =
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS) .readTimeout(0, TimeUnit.MILLISECONDS).writeTimeout(30, TimeUnit.SECONDS).protocols(
            listOf(Protocol.HTTP_1_1)
        ).build()

    private val audioQueue: Queue<String> = LinkedList()
    private var mediaPlayer: MediaPlayer? = null
    private var isStreamEnded = false
    private var audioChunkListener: AudioChunkListener? = null

    fun setAudioChunkListener(listener: AudioChunkListener) {
        this.audioChunkListener = listener
    }


    companion object {
        private val functionMutex = Mutex()
        private val processedEventIds = mutableSetOf<String>()
    }

    suspend fun sendPostRequest(url: String, payload: JSONObject, eventId: String): Flow<Spanned> =
        flow {
            functionMutex.withLock {
                if (eventId !in processedEventIds) {
                    processedEventIds.add(eventId)
                    try {
                        Log.d("Web Search", "Sending request to $url with payload: $payload")
                        // Create the request body
                        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                        val requestBody: RequestBody = payload.toString().toRequestBody(mediaType)

                        // Build the POST request
                        val request = Request.Builder()
                            .addHeader("content-type", "application/json")
                            .addHeader("Origin", "null")
                            .url(url)
                            .post(requestBody)
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) throw IOException("Unexpected code $response")
                            val reader = response.body?.charStream()?.buffered() ?: throw IOException("Empty response body")

                            var buffer = StringBuilder()
                            val customDelimiter = "$" + "_" + "$" // Define your custom delimiter
                            var firstChunk = true

                            reader.useLines { linesSequence ->
                                linesSequence.forEach { line ->
                                    if (line.startsWith(customDelimiter)) {
                                        if (buffer.isNotEmpty()) {
                                            if (firstChunk) {
                                                emit(SpannableString(buffer.toString()))
                                                firstChunk = false
                                            } else {
                                                if (buffer.startsWith(customDelimiter)) buffer.delete(0, customDelimiter.length)
                                                emit(convertHtmlToPlainText(buffer.toString()))  // Emit the accumulated block
                                            }
                                            buffer = StringBuilder() // Start a new block
                                        }
                                    }

                                    buffer.appendLine(line) // Accumulate lines into the buffer
                                }
                                // Process any remaining data in the buffer after the loop
                                if (buffer.isNotEmpty()) {
                                    if (buffer.startsWith(customDelimiter)) buffer.delete(0, customDelimiter.length)
                                    // Implement your logic to handle each chunk here
                                    emit(convertHtmlToPlainText(buffer.toString()))  // Emit or process each chunk as needed
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("Web Search", "$e+${e.message} + ${e.stackTrace}")
                        emit(SpannableString(e.message ?: "Unknown error"))
                    }
                }
            }
        }

    private fun convertHtmlToPlainText(htmlString: String): Spanned {

        val spanned = HtmlCompat.fromHtml(htmlString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        if (spanned is Spannable) {
            // Store existing URLSpans
            val oldURLSpans = spanned.getSpans<URLSpan>(0, spanned.length).associateWith {
                val start = spanned.getSpanStart(it)
                val end = spanned.getSpanEnd(it)
                Pair(start, end)
            }
            // Use LinkifyCompat to add links
            LinkifyCompat.addLinks(spanned, Linkify.WEB_URLS)
            // Restore old spans and remove conflicts
            for ((urlSpan, location) in oldURLSpans) {
                val (start, end) = location
                val addedSpans = spanned.getSpans<URLSpan>(start, end).orEmpty()
                if (addedSpans.isNotEmpty()) {
                    for (span in addedSpans) {
                        spanned.removeSpan(span)
                    }
                }
                spanned.setSpan(urlSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spanned
    }
    /**
     * Experimental
     */
    fun startJsonStream(url: String, payload: JSONObject) {
         isStreamEnded = false
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody: RequestBody = payload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .addHeader("content-type", "application/json")
            .addHeader("Origin", "null")
            .url(url)
            .post(requestBody)
            .build()

        streamingClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("BotStream", "Failed request")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        // Create a reader for the stream
                        val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                        Log.e("BotStream", "Created Reader")
                        processJsonStream(reader){ jsonObject ->
                            // Process the JSON object incrementally
                            processJsonObject(jsonObject)
                            Log.e("BotStream", "Sending to process")
                        }
                        isStreamEnded = true
                        Log.e("BotStream", "Stream Ended")
                    }
                } else {
                    // Handle the error
                    Log.e("BotStream", "Unexpected code $response")
                }
            }
        })
    }

    // Function to process the JSON stream
    fun processJsonStream(reader: BufferedReader,onJsonObjectReceived: (JSONObject) -> Unit) {
        val partialChunk = StringBuilder()


        try {
            var line: String?

            // Read each line as soon as it's available without waiting for all lines
            while (reader.readLine().also { line = it } != null) {
                Log.e("BotStream", "Got Line: ${line}")
                partialChunk.append(line)

                // Optional: Handle boundary between JSON objects (e.g., "}{")
                var boundaryIndex: Int
                while (partialChunk.indexOf("}{").also { boundaryIndex = it } != -1) {
                    val jsonChunk = partialChunk.substring(0, boundaryIndex + 1)
                    partialChunk.delete(0, boundaryIndex + 1)

                    // Parse JSON chunk
                    try {
                        val jsonObject = JSONObject(jsonChunk)
                        Log.d("BotStream", "Sent chunk")
                        // Process the JSON object (e.g., "audio", "text", etc.)
                        onJsonObjectReceived(jsonObject)
                    } catch (e: Exception) {
                        Log.d("BotStream", "JSON error ${e.message}")// Handle JSON parsing error
                    }
                }
            }

            // Handle any remaining data after stream ends
            if (partialChunk.isNotEmpty()) {
                try {
                    val jsonObject = JSONObject(partialChunk.toString())
                    Log.d("BotStream", "Sent final chunk")
                    onJsonObjectReceived(jsonObject)
                } catch (e: Exception) {
                    Log.e("BotStream", "Partial chunk exception : ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("BotStream", "Process Json Stream Exception : ${e.message}")
        }
    }

    private fun processJsonObject(jsonObject: JSONObject) {
        val type = jsonObject.getString("type")
        val data = jsonObject.getString("data")

        when (type) {
            "audio" -> {
                Log.d("BotStream", "Received audio data: $data")
                audioQueue.offer(data)
                Log.d("BotStream", "Audio queue size: ${audioQueue.size}")
                if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
                    Log.d("BotStream", "Starting audio playback")
                    playNextAudio()
                } else {
                    Log.d("BotStream", "Audio player is already playing, queuing audio")
                }
            }
            "text" -> {
                Log.d("BotStream", "Received text: $data")
            }
        }
    }

    private fun playNextAudio() {
        if (audioQueue.isEmpty()) {
            Log.d("playbackAudio", "Audio queue is empty")
            if (isStreamEnded) {
                Log.d("playbackAudio", "Releasing MediaPlayer")
                mediaPlayer?.release()
                mediaPlayer = null
            }
            return
        }

        try {
            val base64Audio = audioQueue.poll()
            Log.d("playbackAudio", "Starting playback of audio data: $base64Audio")
            val audioData = android.util.Base64.decode(base64Audio, android.util.Base64.DEFAULT)
            Log.d("playbackAudio", "Decoded audio data size: ${audioData.size} bytes")
            val tempFile = createTempFile(suffix = ".mp3")
            Log.d("playbackAudio", "Created temp file: ${tempFile.absolutePath}")
            tempFile.writeBytes(audioData)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                Log.d("playbackAudio", "Audio playback started")
                setOnCompletionListener {
                    Log.d("playbackAudio", "Audio playback completed")
                    playNextAudio()
                }
            }
        }catch (e: Exception) {
            Log.e("playbackAudio", "Error starting media player: ${e.message}")
        }
    }

}









