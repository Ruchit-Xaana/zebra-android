/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import androidx.compose.runtime.Immutable

@Immutable
sealed interface FileSelectorEvents {
    data object FetchFiles : FileSelectorEvents
    data class DownloadFiles(val matrixFiles: List<MatrixFile>) : FileSelectorEvents
    data object UploadFiles : FileSelectorEvents
    data object DeleteFiles : FileSelectorEvents
    data object ResetDownloadComplete : FileSelectorEvents
}
