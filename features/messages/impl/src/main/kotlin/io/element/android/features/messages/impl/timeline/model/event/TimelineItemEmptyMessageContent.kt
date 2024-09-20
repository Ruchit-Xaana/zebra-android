/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import kotlinx.serialization.json.JsonObject

data class TimelineItemEmptyMessageContent(
    val body: String,
    val contentInfo: JsonObject,
) : TimelineItemEventContent {
    override val type: String = "TimelineItemEmptyMessageContent"
}
