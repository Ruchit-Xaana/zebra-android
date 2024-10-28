/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import java.util.Date

data class FileDTO(
    val mediaId: String,
    val eventId: String?,
    val roomId: String?,
    val senderId: String?,
    val filename: String,
    val filetype: String?,
    val size: Double,
    val timestamp: Date
)
