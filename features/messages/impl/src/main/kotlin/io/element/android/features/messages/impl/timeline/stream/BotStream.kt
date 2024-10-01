/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.stream

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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BotStream {
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).protocols(listOf(Protocol.HTTP_1_1)).build()
    companion object {
        private val functionMutex = Mutex()
        private val processedEventIds = mutableSetOf<String>()
    }
    suspend fun sendPostRequest(url: String, payload: JSONObject,eventId:String): Flow<Spanned> =
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

                            // Obtain the BufferedReader from the response body
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

    suspend fun sendVoiceChatPostRequest(url: String, payload: JSONObject): Flow<String> = flow {
        functionMutex.withLock {
                try {
                    Log.d("BotStream", "Sending request to $url with payload: $payload")

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

                        val contentType = response.header("Content-Type")
                        val reader = response.body?.charStream()?.buffered() ?: throw IOException("Empty response body")

                        // Handle different content types
                        when (contentType) {
                            "text/event-stream; charset=utf-8" -> {
                                reader.useLines { lines ->
                                    lines.forEach { line ->
                                        Log.d("BotStream", "Stream data: $line")
                                        emit(line) // Emit each line of the stream
                                    }
                                }
                            }
                            "application/json" -> {
                                reader.use { it.readText() }.let { jsonResponse ->
                                    Log.d("BotStream", "JSON response: $jsonResponse")
                                    emit(jsonResponse)
                                }
                            }
                            else -> {
                                throw IOException("Unsupported content type: $contentType")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("BotStream", "Error: ${e.message}")
                    emit("Error: ${e.message}")
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
}
