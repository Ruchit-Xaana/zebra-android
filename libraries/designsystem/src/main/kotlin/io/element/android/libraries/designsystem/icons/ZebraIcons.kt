/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */
@file:Suppress("all")
package io.element.android.libraries.designsystem.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import io.element.android.libraries.designsystem.R
import kotlinx.collections.immutable.persistentListOf

object ZebraIcons {
    @Composable fun AudioCapture(): ImageVector {
        return ImageVector.vectorResource(R.drawable.audio_capture)
    }

    val all @Composable get() = persistentListOf(
        AudioCapture(),
    )

    val allResIds get() = persistentListOf(
        R.drawable.audio_capture,
    )
}
