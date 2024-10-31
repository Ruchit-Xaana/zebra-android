/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.files

import io.element.android.libraries.di.RoomScope
import io.element.android.libraries.di.SingleIn
import javax.inject.Inject

@SingleIn(RoomScope::class)
class FileSelectionCache @Inject constructor() {
    var selectedFiles: List<MatrixFile> = emptyList()

    fun selectedFiles(files: List<MatrixFile>) {
        selectedFiles = files
    }

    fun clear() {
        selectedFiles = emptyList()
    }

    fun removeFile(mediaId: String) {
        selectedFiles = selectedFiles.filterNot { it.mediaId == mediaId }
    }
}

