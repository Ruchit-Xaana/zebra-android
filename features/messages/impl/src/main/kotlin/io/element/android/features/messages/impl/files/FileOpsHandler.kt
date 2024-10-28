/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import android.util.Log
import io.element.android.appconfig.AuthenticationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class FileOpsHandler @Inject constructor() {
    private val client: OkHttpClient = OkHttpClient()
    private val reportsApiUrl: String = AuthenticationConfig.REPORTS_API_URL
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
                Log.d("FileOpsHandler", "Response: ${response.code}")
                if (!response.isSuccessful) return@withContext emptyList()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                dateFormat.timeZone = TimeZone.getTimeZone("GMT")
                val data = response.body?.string().let { body ->
                    Log.d("FileOpsHandler", "ResponseBody: ${body}")
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
                if (!response.isSuccessful) throw IOException("Failed to delete file")
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
