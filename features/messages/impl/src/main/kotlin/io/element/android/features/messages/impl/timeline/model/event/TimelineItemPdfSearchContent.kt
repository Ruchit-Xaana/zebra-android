/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */
@file:Suppress("PropertyName")
package io.element.android.features.messages.impl.timeline.model.event

import kotlinx.serialization.Serializable

data class TimelineItemPdfSearchContent (
    val body:String,
    val formattedBody: CharSequence?,
    val additionalData:PdfSearchData?,
): TimelineItemEventContent {
    override val type: String = "TimelineItemPdfSearchContent"
}


@Serializable
data class PdfSearchData(
    val raw_question: String,
    val file_names:List<String>?,
    val web_url:List<String>?,
    val file_prompt:List<String>?,
)
