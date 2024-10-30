/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import io.element.android.appconfig.AuthenticationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class FileOpsHandler @Inject constructor() {
    private val client: OkHttpClient = OkHttpClient()
    private val reportsApiUrl: String = AuthenticationConfig.REPORTS_API_URL
    private val webSocketReportsApiUrl: String = AuthenticationConfig.WEBSOCKET_REPORTS_API_URL

    /**
     * Function to generate a random primary key
     */
    @Suppress("SameParameterValue")
    private fun generatePrimaryKey(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { characters.random() }
            .joinToString("")
    }

    /**
     * Helper function to encode ByteArray to Base64
     */
    private fun ByteArray.encodeToBase64(): String = Base64.getEncoder().encodeToString(this)

    /**
     * WebSocket file upload method
     */
    suspend fun uploadFilesWebSocket(
        userId: String,
        files: List<File>,
        onProgressUpdate: (Int) -> Unit,
        onSetBusy: (Boolean) -> Unit,
        onError: (String) -> Unit,
        onCompleted: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val completedConnections = AtomicInteger(0)
        val progressArray = IntArray(files.size) { 0 }

        files.forEachIndexed { index, file ->
            val apiUrl = "$webSocketReportsApiUrl/pdf_upload"
            val request = Request.Builder().url(apiUrl).build()
            val mxcUrl = generatePrimaryKey(18)

            client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    onSetBusy(true)
                    val fileMetadata = JSONObject().apply {
                        put("media_id", mxcUrl)
                        put("room_id", null)
                        put("event_id", null)
                        put("user_id", userId)
                        put("sender_id", userId)
                        put("media_type", file.extension)
                    }
                    webSocket.send(fileMetadata.toString())

                    val encodedContent = file.readBytes().encodeToBase64()

                    val filePayload = JSONObject().apply {
                        put("filename", JSONArray().put(file.name))
                        put("content", JSONArray().put(encodedContent))
                    }
                    webSocket.send(filePayload.toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Timber.tag("FileSelectorPresenter").i("Received message: %s", text)
                    if (text.startsWith("success")) {
                        progressArray[index] += 1
                        onProgressUpdate(progressArray.minOrNull() ?: 0)
                        if (progressArray[index] == 3) {
                            completedConnections.incrementAndGet()
                            if (completedConnections.get() == files.size) {
                                onSetBusy(false)
                            }
                        }
                    } else if (text.startsWith("fail")) {
                        onError("Upload failed for file: ${file.name} with message: $text")
                        completedConnections.incrementAndGet()
                        if (completedConnections.get() == files.size) onSetBusy(false)
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.tag("FileSelectorPresenter").d("WebSocket closed with code: $code, reason: $reason")
                    onCompleted()
                }


                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    onError("WebSocket error for file ${file.name}: ${t.message} with response ${response?.body?.string()}, ${response?.code}")
                    completedConnections.incrementAndGet()
                    if (completedConnections.get() == files.size) onSetBusy(false)
                }
            })
        }
    }

    suspend fun listFiles(userId: String, type: String? = null, uploadService: String? = "zebra"): List<FileDTO> =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("user_id", userId)
                put("media_type", type)
                put("upload_service", uploadService)
            }
            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$reportsApiUrl/api/files/get_info")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("GMT")
                val data = response.body?.string().let { body ->
                    val jsonArray = JSONArray(body)
                    List(jsonArray.length()) { i ->
                        val item = jsonArray.getJSONObject(i)
                        FileDTO(
                            mediaId = item.getString("session_id"),
                            eventId = item.optString("event_id"),
                            roomId = item.getString("room_id"),
                            senderId = item.getString("sender_id"),
                            filename = item.getString("filename"),
                            filetype = item.getString("mime_type"),
                            size = item.optJSONObject("metadata")?.optDouble("size") ?: 0.0,
                            timestamp = item.optJSONObject("metadata")?.optString("timestamp")?.let { dateString ->
                                dateFormat.parse(dateString)
                            } ?: Date(0)
                        )
                    }
                }
                data
            }
        }

    suspend fun getFile(mediaIds: String, userId: String): ByteArray =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("media_id", mediaIds)
                put("user_id", userId)
            }
            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$reportsApiUrl/api/files/download")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to download file")
                val base64File = JSONObject(response.body?.string() ?: "").getString("file")
                Base64.getDecoder().decode(base64File)
            }
        }

    suspend fun uploadFile(
        filename: String, mediaId: String, type: String, userIds: Any, eventId: String? = null,
        roomId: String? = null, senderId: String? = null
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("filename", filename)
            put("media_id", mediaId)
            put("media_type", type)
            put("user_ids", userIds)
            put("event_id", eventId)
            put("room_id", roomId)
            put("sender_id", senderId)
        }
        val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$reportsApiUrl/api/files/upload_info")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to upload file info")
            JSONObject(response.body?.string() ?: "")
        }
    }

    suspend fun deleteFile(mediaId: String, userId: String): JSONObject =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().apply {
                put("media_id", mediaId)
                put("user_id", userId)
            }
            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$reportsApiUrl/api/files/delete")
                .delete(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Failed to delete file with code ${response.code}")
                JSONObject(response.body?.string() ?: "")
            }
        }

    fun dtoToFileAdapters(dto: FileDTO, defaultSenderId: String?): MatrixFile {
        val synapseUrl = AuthenticationConfig.DEFAULT_HOMESERVER_URL
        val uri = "$synapseUrl/_matrix/media/v3/download/securezebra.com/${dto.mediaId}"
        return MatrixFile(
            id = dto.mediaId,
            name = dto.filename,
            downloadUrl = uri,
            timestamp = dto.timestamp,
            sender = dto.senderId ?: defaultSenderId ?: "null",
            roomId = dto.roomId,
            isEncrypted = false,
            mediaId = dto.mediaId,
            type = "m.file",
            fileSize = dto.size,
            mimetype = dto.filetype,
            event = dto.eventId
        )
    }
}
