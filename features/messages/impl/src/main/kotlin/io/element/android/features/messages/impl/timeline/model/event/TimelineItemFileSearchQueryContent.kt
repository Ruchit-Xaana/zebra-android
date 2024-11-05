/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.model.event

import io.element.android.libraries.matrix.ui.messages.toPlainText
import org.jsoup.nodes.Document

data class TimelineItemFileSearchQueryContent (
    val body: String,
    val fileNames : List<String>,
    val pillifiedBody: CharSequence = body,
    val htmlDocument: Document?,
    val plainText: String = htmlDocument?.toPlainText() ?: body,
    val formattedBody: CharSequence?,
    val isEdited: Boolean,
): TimelineItemEventContent {
    override val type: String = "TimelineItemFileSearchQueryContent"
}
