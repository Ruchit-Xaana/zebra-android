/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

@file:Suppress("PropertyName")

package io.element.android.features.messages.impl.timeline.model.event

import kotlinx.serialization.Serializable

data class TimelineItemWebSearchContent (
    val body:String,
    val formattedBody: CharSequence?,
    val additionalData:WebSearchData?,
    ): TimelineItemEventContent {
        override val type: String = "TimelineItemWebSearchContent"
}


@Serializable
data class WebSearchData(
    val raw_question: String,
    val sources:List<String>?,
    val prompt:List<String>?,
)
