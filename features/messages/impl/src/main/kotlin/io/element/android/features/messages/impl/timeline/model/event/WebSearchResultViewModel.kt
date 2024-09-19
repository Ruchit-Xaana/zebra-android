/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.element.android.appconfig.AuthenticationConfig.BOT_API_URL
import io.element.android.features.messages.impl.timeline.stream.BotStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject

class WebSearchResultViewModel: ViewModel() {
    private val _displayedText = mutableStateOf(SpannableString(""))
    val displayedText: State<Spanned> = _displayedText
    private val _displayedLinks = MutableStateFlow<List<Spanned>>(emptyList())
    val displayedLinks: StateFlow<List<Spanned>> = _displayedLinks
    var eventId:String = ""

    init {
        //Log.d("Factory Debug","init ${eventId.toString()}")
        _displayedText.value = SpannableString("")
        _displayedLinks.value = emptyList()
    }

    fun initializeTextAndLinks(){
        _displayedText.value = SpannableString("")
        _displayedLinks.value = emptyList()
    }

    fun fetchAndProcessData(contentInfo:JsonObject,roomId:String?,eventId:String?) {
        if (roomId != null && eventId != null && contentInfo.containsKey("questionId") && contentInfo.containsKey("raw_question")) {
            viewModelScope.launch {
                this@WebSearchResultViewModel.eventId = eventId.toString()
                val url = "${BOT_API_URL}/web_stream"
                val questionId=Json.parseToJsonElement(contentInfo["questionId"].toString()).jsonPrimitive.content
                val rawQuestion=Json.parseToJsonElement(contentInfo["raw_question"].toString()).jsonPrimitive.content

                val payload = JSONObject().apply {
                    put("roomId", roomId)
                    put("firstMessageEventId", eventId)
                    put("question", rawQuestion)
                    put("eventId", questionId)
                }
                val zebraStream = BotStream()
                launch(Dispatchers.IO) {
                    var isFirstLine = true
                    zebraStream.sendPostRequest(url, payload, eventId.toString()).collect { line ->
                        Log.d("RuchitStream", "$line")
                        if (isFirstLine) {
                            val jsonRegex = Regex("\\{.*\\}")
                            val jsonString = jsonRegex.find(line)?.value
                            if (jsonString != null) {
                                Log.d("RuchitStream", "$jsonString")
                                val links = extractLinks(jsonString)
                                _displayedLinks.value = links.map { SpannableString(it) }
                            }
                            isFirstLine = false
                        } else{
                            Log.d("RuchitStream", "${line}")
                            _displayedText.value = SpannableString(line)
                        }
                    }
                }
            }
        }
        else{
            _displayedText.value = SpannableString("Error sending request")
            _displayedLinks.value = emptyList()
        }
    }
    private fun extractLinks(text: String): List<String> {
        val json = Json {
            ignoreUnknownKeys = true}
        return try {
            val jsonObject = json.decodeFromString<JsonObject>(text)
            jsonObject["source_links"]?.jsonArray?.map { it.jsonPrimitive.content }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
