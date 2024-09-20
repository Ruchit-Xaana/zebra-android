/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.factories

import android.util.Log
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.element.android.features.messages.impl.timeline.TimelineItemIndexer
import io.element.android.features.messages.impl.timeline.diff.TimelineItemsCacheInvalidator
import io.element.android.features.messages.impl.timeline.factories.event.TimelineItemEventFactory
import io.element.android.features.messages.impl.timeline.factories.virtual.TimelineItemVirtualFactory
import io.element.android.features.messages.impl.timeline.groups.TimelineItemGrouper
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.libraries.androidutils.diff.DiffCacheUpdater
import io.element.android.libraries.androidutils.diff.MutableListDiffCache
import io.element.android.libraries.core.coroutine.CoroutineDispatchers
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.room.RoomMember
import io.element.android.libraries.matrix.api.timeline.MatrixTimelineItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class TimelineItemsFactory @AssistedInject constructor(
    @Assisted config: TimelineItemsFactoryConfig,
    eventItemFactoryCreator: TimelineItemEventFactory.Creator,
    private val dispatchers: CoroutineDispatchers,
    private val virtualItemFactory: TimelineItemVirtualFactory,
    private val timelineItemGrouper: TimelineItemGrouper,
    private val timelineItemIndexer: TimelineItemIndexer,
) {
    @AssistedFactory
    interface Creator {
        fun create(config: TimelineItemsFactoryConfig): TimelineItemsFactory
    }

    private val eventItemFactory = eventItemFactoryCreator.create(config)
    private val _timelineItems = MutableSharedFlow<ImmutableList<TimelineItem>>(replay = 1)
    private val lock = Mutex()
    private val diffCache = MutableListDiffCache<TimelineItem>()
    private val relatedEventIndices = mutableListOf<String>()
    private val diffCacheUpdater = DiffCacheUpdater<MatrixTimelineItem, TimelineItem>(
        diffCache = diffCache,
        detectMoves = false,
        cacheInvalidator = TimelineItemsCacheInvalidator()
    ) { old, new ->
        if (old is MatrixTimelineItem.Event && new is MatrixTimelineItem.Event) {
            old.uniqueId == new.uniqueId
        } else {
            false
        }
    }

    val timelineItems: Flow<ImmutableList<TimelineItem>> = _timelineItems.distinctUntilChanged()

    suspend fun replaceWith(
        timelineItems: List<MatrixTimelineItem>,
        roomMembers: List<RoomMember>,
    ) = withContext(dispatchers.computation) {
        lock.withLock {
            diffCacheUpdater.updateWith(timelineItems)
            buildAndEmitTimelineItemStates(timelineItems, roomMembers)
        }
    }

    private suspend fun buildAndEmitTimelineItemStates(
        timelineItems: List<MatrixTimelineItem>,
        roomMembers: List<RoomMember>,
    ) {
        val newTimelineItemStates = ArrayList<TimelineItem>()
        for (index in diffCache.indices().reversed()) {
            val cacheItem = diffCache.get(index)
            if (cacheItem == null) {
                buildAndCacheItem(timelineItems, index, roomMembers,relatedEventIndices)?.also { timelineItemState ->
                    newTimelineItemStates.add(timelineItemState)
                }
            } else {
                val updatedItem = if (cacheItem is TimelineItem.Event && roomMembers.isNotEmpty()) {
                    eventItemFactory.update(
                        timelineItem = cacheItem,
                        receivedMatrixTimelineItem = timelineItems[index] as MatrixTimelineItem.Event,
                        roomMembers = roomMembers
                    )
                } else {
                    cacheItem
                }
                newTimelineItemStates.add(updatedItem)
            }
        }
        relatedEventIndices.forEach { eventId ->
            val removed = newTimelineItemStates.removeAll{
                timelineItem -> (timelineItem as? TimelineItem.Event)?.eventId?.value == eventId
            }
            if (removed) {
                Log.d("TimelineItemsFactory", "Removed timeline item with eventId: $eventId")
            }
        }
        val result = timelineItemGrouper.group(newTimelineItemStates).toPersistentList()
        timelineItemIndexer.process(result)
        this._timelineItems.emit(result)
    }

    private suspend fun buildAndCacheItem(
        timelineItems: List<MatrixTimelineItem>,
        index: Int,
        roomMembers: List<RoomMember>,
        relatedEventIndices: MutableList<String>,
    ): TimelineItem? {
        val timelineItem =
            when (val currentTimelineItem = timelineItems[index]) {
                is MatrixTimelineItem.Event -> {
                    val relatedEventId = findRelatedEventId(currentTimelineItem)
                    val eventItem = eventItemFactory.create(currentTimelineItem, index, timelineItems, roomMembers)
                    if (relatedEventId != null) {
                        relatedEventIndices.add(relatedEventId)
                    }
                    eventItem
                }
                is MatrixTimelineItem.Virtual -> virtualItemFactory.create(currentTimelineItem)
                MatrixTimelineItem.Other -> null
            }
        diffCache[index] = timelineItem
        return timelineItem
    }
    private fun findRelatedEventId(event: MatrixTimelineItem.Event): String? {
        if(event.eventId!=null) {
            val debugInfo = event.event.debugInfo.originalJson
            if (debugInfo != null) {
                try {
                    val jsonObject = JSONObject(debugInfo)
                    val contentObject = jsonObject.getJSONObject("content")
                    if (contentObject.getJSONObject("m.relates_to").has("rel_type")) {
                        val relType = contentObject.getJSONObject("m.relates_to").get("rel_type")
                        Log.d("TimelineItemsFactory", "Found rel_type event index for event ${relType}")
                        if (relType == "m.stream.replace") {
                            if (contentObject.getJSONObject("m.relates_to").has("event_id")) {
                                val relatedEventId = contentObject.getJSONObject("m.relates_to").get("event_id")
                                Log.d("TimelineItemsFactory", "Found relatedEventId event index for event ${relatedEventId.toString()}")
                                return relatedEventId.toString()
                            }
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("TimelineItemsFactory", "Error parsing debugInfo JSON: ${e.message}")
                }
            }
        }
        return null
    }
}
