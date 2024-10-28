/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

data class FileSelectorState(
    val documents: List<MatrixFile>,
    val downloadComplete: Boolean,
    val eventSink: (FileSelectorEvents) -> Unit,
)
