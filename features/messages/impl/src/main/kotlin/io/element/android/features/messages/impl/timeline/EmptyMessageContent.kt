/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline

import kotlinx.serialization.Serializable

@Serializable data class EmptyMessageContent(
    val roomId: String,
    val eventId: String,
    val question: String,
    var firstMessageEventId: String?=null,
)
