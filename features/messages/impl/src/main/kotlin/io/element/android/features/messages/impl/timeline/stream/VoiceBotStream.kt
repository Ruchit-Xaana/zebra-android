/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.stream

import android.util.Base64
import android.util.Log
import androidx.activity.result.launch
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
    fun startJsonStream(url: String, payload: JSONObject) {
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
    fun processJsonStream(reader: BufferedReader, onJsonObjectReceived: (JSONObject) -> Unit) {
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
