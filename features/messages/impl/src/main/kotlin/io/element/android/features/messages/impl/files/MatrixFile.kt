/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import java.util.Date

data class MatrixFile(
    val id: String? = null,
    val name: String,
    val downloadUrl: String,
    val timestamp: Date,
    val sender: String,
    val roomId: String? = null,
    val type: String,
    val isEncrypted: Boolean,
    val mediaId: String,
    val event: String? = null,
    val fileSize: Double? = null,
    val mimetype: String? = null
)
