/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.voicemessages.chat

interface VoiceChatEvents {
    data object Start : VoiceChatEvents
    data object Stop : VoiceChatEvents
    data object Exit : VoiceChatEvents
    data object Connect : VoiceChatEvents
    data object Disconnect : VoiceChatEvents
}
