/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.voicemessages.chat

import androidx.compose.runtime.Stable

@Stable
data class VoiceMessageChatState(
    val canStartSession : Boolean,
    val audioSessionId: Int?,
    val isRecording: Boolean,
    val toastMessage: String?,
    val eventSink: (VoiceChatEvents) -> Unit,
)
