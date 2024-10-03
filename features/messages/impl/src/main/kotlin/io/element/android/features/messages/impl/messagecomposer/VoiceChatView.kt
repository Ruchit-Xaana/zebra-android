/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.voicemessages.chat.VoiceChatEvents
import io.element.android.features.messages.impl.voicemessages.chat.VoiceMessageChatState
import io.element.android.libraries.androidutils.ui.hideKeyboard

import io.element.android.libraries.designsystem.theme.components.Text

@Composable
internal fun VoiceChatView(
    state: VoiceMessageChatState,
    composerState: MessageComposerState,
) {
    val localView = LocalView.current
    var isVisible by rememberSaveable { mutableStateOf(composerState.showVoiceChatScreen) }
    var enableButton by rememberSaveable { mutableStateOf(state.enableRecording) }

    BackHandler(enabled = isVisible) {
        isVisible = false
    }

    LaunchedEffect(Unit) {
        localView.hideKeyboard()
    }
    LaunchedEffect(state.enableRecording) {
        enableButton = if (state.enableRecording) {
            localView.hideKeyboard()
            true
        } else {
            false
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            composerState.eventSink(MessageComposerEvents.VoiceChat.Dismiss)
        }
    }

    if (isVisible) {
       VoiceChatScreen(
           state = state,
           composerState = composerState,
           enableButton = enableButton
       )
    }
}
@Composable
private fun VoiceChatScreen(
    state: VoiceMessageChatState,
    composerState: MessageComposerState,
    enableButton : Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voice Chat",
            style = ElementTheme.typography.fontHeadingLgBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Transcription will appear here...",
            style = ElementTheme.typography.fontBodyLgRegular,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                enabled = enableButton,
                onClick = { state.eventSink(VoiceChatEvents.Start) },
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.weight(1f)
            )
            {
                Text(text = "Start")
            }

            Button(
                enabled = enableButton,
                onClick = { composerState.eventSink(MessageComposerEvents.VoiceChat.Dismiss) },
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.weight(1f)
            )
            {
                Text(text = "Dismiss")
            }
        }

    }
}
