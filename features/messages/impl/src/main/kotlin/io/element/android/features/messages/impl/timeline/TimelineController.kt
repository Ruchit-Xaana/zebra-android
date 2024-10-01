/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline

import android.util.Log
import com.squareup.anvil.annotations.ContributesBinding
import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.room.MatrixRoom
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.matrix.api.timeline.TimelineProvider
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable
import java.util.Optional
import javax.inject.Inject

/**
 * This controller is responsible of using the right timeline to display messages and make associated actions.
 * It can be focused on the live timeline or on a detached timeline (focusing an unknown event).
 */
@SingleIn(RoomScope::class)
@ContributesBinding(RoomScope::class, boundType = TimelineProvider::class)
class TimelineController @Inject constructor(
    private val room: MatrixRoom,
) : Closeable, TimelineProvider {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val liveTimeline = flowOf(room.liveTimeline)
    private val detachedTimeline = MutableStateFlow<Optional<Timeline>>(Optional.empty())

    private val eventCache = mutableMapOf<String, String?>()
    private val localCache = mutableMapOf<String, String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun timelineItems(): Flow<List<MatrixTimelineItem>> {
        return currentTimelineFlow
            .flatMapLatest { timeline ->
                timeline.timelineItems.map { timelineItems ->
                    val referencedEventIds = timelineItems
                        .mapNotNull { timelineItem ->
                            if (timelineItem is MatrixTimelineItem.Event) {
                                val eventId = timelineItem.eventId?.value
                                if (eventId != null && eventCache.containsKey(eventId)) {
                                    eventId to eventCache[eventId]
                                }else if (eventId != null) {
                                    cacheMessageIfInitFromHomepage(timelineItem)
                                    val relatedEventId = findRelatedEventId(timelineItem)
                                    eventCache[eventId] = relatedEventId
                                    eventId to relatedEventId
                                }else{
                                    null
                                }
                            } else {
                                null
                            }
                        }
                        .toMap()
                    timelineItems.filterNot { timelineItem ->
                        timelineItem is MatrixTimelineItem.Event && timelineItem.eventId != null && referencedEventIds.containsValue(timelineItem.eventId?.value)
                    }
                }
            }
    }

    fun isLive(): Flow<Boolean> {
        return detachedTimeline.map { !it.isPresent }
    }

    suspend fun invokeOnCurrentTimeline(block: suspend (Timeline.() -> Any)) {
        currentTimelineFlow.value.run {
            block(this)
        }
    }

    suspend fun focusOnEvent(eventId: EventId): Result<Unit> {
        return room.timelineFocusedOnEvent(eventId)
            .onFailure {
                if (it is CancellationException) {
                    throw it
                }
            }
            .map { newDetachedTimeline ->
                detachedTimeline.getAndUpdate { current ->
                    if (current.isPresent) {
                        current.get().close()
                    }
                    Optional.of(newDetachedTimeline)
                }
            }
    }

    /**
     * Makes sure the controller is focused on the live timeline.
     * This does close the detached timeline if any.
     */
    fun focusOnLive() {
        closeDetachedTimeline()
    }

    private fun closeDetachedTimeline() {
        detachedTimeline.getAndUpdate {
            when {
                it.isPresent -> {
                    it.get().close()
                    Optional.empty()
                }
                else -> Optional.empty()
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
        closeDetachedTimeline()
    }

    suspend fun paginate(direction: Timeline.PaginationDirection): Result<Boolean> {
        return currentTimelineFlow.value.paginate(direction)
            .onSuccess { hasReachedEnd ->
                if (direction == Timeline.PaginationDirection.FORWARDS && hasReachedEnd) {
                    focusOnLive()
                }
            }
    }

    private val currentTimelineFlow = combine(liveTimeline, detachedTimeline) { live, detached ->
        when {
            detached.isPresent -> detached.get()
            else -> live
        }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, room.liveTimeline)

    private fun findRelatedEventId(event: MatrixTimelineItem.Event): String? {
        if(event.eventId!=null) {
            val debugInfo = event.event.debugInfo.originalJson
            if (debugInfo != null) {
                try {
                    val jsonObject = JSONObject(debugInfo)
                    val contentObject = jsonObject.getJSONObject("content")
                    if (contentObject.getJSONObject("m.relates_to").has("rel_type")) {
                        val relType = contentObject.getJSONObject("m.relates_to").get("rel_type")
                        Log.d("TimelineController", "Found rel_type event index for event ${relType}")
                        if (relType == "m.stream.replace") {
                            if (contentObject.getJSONObject("m.relates_to").has("event_id")) {
                                val relatedEventId = contentObject.getJSONObject("m.relates_to").get("event_id")
                                Log.d("TimelineController", "Found relatedEventId event index for event ${relatedEventId}")
                                return relatedEventId.toString()
                            }
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("TimelineController", "Error parsing debugInfo JSON: ${e.message}")
                }
            }
        }
        return null
    }
    private fun cacheMessageIfInitFromHomepage(timelineItem: MatrixTimelineItem.Event) {
        if (timelineItem.event.isOwn && !localCache.containsKey(timelineItem.eventId?.value) && timelineItem.event.content is MessageContent) {
            val messageContent = timelineItem.event.content as MessageContent
            if (messageContent.type is TextMessageType) {
                val textMessage = messageContent.type as TextMessageType
                if (textMessage.body == "Init from homepage...") {
                    val eventId = timelineItem.eventId?.value
                    val formattedBody = textMessage.formatted?.body
                    if (eventId != null && formattedBody != null) {
                        localCache[eventId] = formattedBody
                    }
                }
            }
        }
    }
    fun getEventIdFromFormattedBody(formattedBody: String): String? {
        for ((eventId, body) in localCache) {
            if (body == formattedBody) {
                return eventId
            }
        }
        return null
    }
    override fun activeTimelineFlow(): StateFlow<Timeline> {
        return currentTimelineFlow
    }
}
