/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.voicerecorder.api

interface AudioPlaybackListener {
    fun onPlaying(audioSessionId: Int)
    fun onPlaybackCompleted()
    fun onPlaybackError(errorMessage: String)
}
