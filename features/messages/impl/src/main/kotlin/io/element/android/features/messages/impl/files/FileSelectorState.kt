/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

data class FileSelectorState(
    val documents: List<MatrixFile>,
    val completedFileOp: Boolean,
    val isBusy: Boolean,
    val exit: Boolean,
    val progress: Int,
    val eventSink: (FileSelectorEvents) -> Unit,
)
