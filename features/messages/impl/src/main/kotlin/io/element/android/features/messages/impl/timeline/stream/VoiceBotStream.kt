/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.stream

import android.util.Base64
import android.util.Log
import io.element.android.libraries.voicerecorder.api.AudioPlaybackListener
import io.element.android.libraries.voicerecorder.impl.DefaultAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class VoiceBotStream(private val audioSpeaker: DefaultAudioPlayer) {
    private val streamingClient: OkHttpClient =
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS) .readTimeout(0, TimeUnit.MILLISECONDS).writeTimeout(30, TimeUnit.SECONDS).protocols(
            listOf(Protocol.HTTP_1_1)
        ).build()

    /**
     * Experimental
     */
    fun startJsonStream(url: String, payload: JSONObject, callback: AudioPlaybackListener,streamListener: VoiceBotStreamListener) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody: RequestBody = payload.toString().toRequestBody(mediaType)

        audioSpeaker.playbackCallback = callback

        val request = Request.Builder()
            .addHeader("content-type", "application/json")
            .addHeader("Origin", "null")
            .url(url)
            .post(requestBody)
            .build()

        Log.d("BotStream", "Sending Request")
        streamingClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("BotStream", "Failed request")
                streamListener.onStreamError("Failed request: ${e.message}")
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        // Create a reader for the stream
                        val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                        Log.e("BotStream", "Created Reader")
                        processJsonStream(reader,streamListener){ jsonObject ->
                            // Process the JSON object incrementally
                            processJsonObject(jsonObject)
                            Log.e("BotStream", "Sending to process")
                        }
                        Log.e("BotStream", "Stream Ended")
                    }
                } else {
                    // Handle the error
                    Log.e("BotStream", "Unexpected code $response")
                    streamListener.onStreamError("Unexpected response code: ${response.code}")
                }
            }
        })
    }

    // Function to process the JSON stream
    fun processJsonStream(reader: BufferedReader,streamListener: VoiceBotStreamListener, onJsonObjectReceived: (JSONObject) -> Unit) {
        val buffer = CharArray(1024) // Adjust buffer size as needed
        val partialChunk = StringBuilder()
        try {
            var bytesRead: Int

            while (reader.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                val data = String(buffer, 0, bytesRead)
                partialChunk.append(data)

                // Attempt to parse complete JSON objects
                var jsonStartIndex = 0
                var jsonEndIndex = partialChunk.indexOf("}")
                while (jsonEndIndex != -1) {
                    val jsonChunk = partialChunk.substring(jsonStartIndex, jsonEndIndex + 1)

                    try {
                        val jsonObject = JSONObject(jsonChunk)
                        onJsonObjectReceived(jsonObject)
                    } catch (e: JSONException) {
                        // Handle JSON parsing error (likely incomplete object)
                        Log.d("BotStream", "JSON error: ${e.message}")
                        streamListener.onStreamError("JSON error: ${e.message}")
                    }

                    jsonStartIndex = jsonEndIndex + 1
                    jsonEndIndex = partialChunk.indexOf("}", jsonStartIndex)
                }
                // Remove processed chunks
                if (jsonStartIndex > 0) {
                    partialChunk.delete(0, jsonStartIndex)
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
                    streamListener.onStreamError("Partial chunk exception : ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("BotStream", "Process Json Stream Exception : ${e.message}")
            streamListener.onStreamError("Process Json Stream Exception : ${e.message}")
        }
    }

    private fun processJsonObject(jsonObject: JSONObject) {
        val type = jsonObject.getString("type")
        val data = jsonObject.getString("data")

        when (type) {
            "audio" -> {
                Log.d("BotStream", "Received audio data: $data")
                val audioData = Base64.decode(data, Base64.DEFAULT)
                CoroutineScope(Dispatchers.Main).launch {
                    audioSpeaker.playAudioFromBytes(audioData)
                }
            }
            "text" -> {
                Log.d("BotStream", "Received text: $data")
            }
        }
    }
}
