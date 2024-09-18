/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.libraries.voicerecorder.api

interface SpeechRecognitionListener {
    fun onTextRecognized(recognizedText: String)
    fun onError(error: Int)
}
