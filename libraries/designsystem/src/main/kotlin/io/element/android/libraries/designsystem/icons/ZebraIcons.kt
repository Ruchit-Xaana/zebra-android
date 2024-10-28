/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */
@file:Suppress("all")
package io.element.android.libraries.designsystem.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.element.android.libraries.designsystem.R
import kotlinx.collections.immutable.persistentListOf

object ZebraIcons {
    @Composable fun AudioCapture(): ImageVector {
        return ImageVector.vectorResource(R.drawable.audio_capture)
    }
    @Composable fun HeadsetIcon(): ImageVector {
        return Icons.Default.Headset
    }
    @Composable fun FormIcon(): ImageVector {
        return Icons.Default.Description
    }
    @Composable fun FileIcon(): ImageVector {
        return Icons.AutoMirrored.Filled.InsertDriveFile
    }
    @Composable fun DocumentsIcon(): ImageVector {
        return Icons.Filled.FileCopy
    }
    @Composable fun MediaIcon(): ImageVector {
        return Icons.Filled.PhotoLibrary
    }
    @Composable fun UploadIcon(): ImageVector {
        return Icons.Default.Upload
    }
    @Composable fun DownloadIcon(): ImageVector {
        return Icons.Default.Download
    }
    @Composable fun DeleteIcon(): ImageVector {
        return Icons.Default.Delete
    }

    val all @Composable get() = persistentListOf(
        AudioCapture(),
        HeadsetIcon(),
        FormIcon(),
        FileIcon(),
        DocumentsIcon(),
        MediaIcon(),
        UploadIcon(),
        DownloadIcon(),
        DeleteIcon(),
    )

    val allResIds get() = persistentListOf(
        R.drawable.audio_capture,
    )
}
