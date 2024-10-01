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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Button
import io.element.android.libraries.designsystem.theme.components.Text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceChatView(
    state: MessageComposerState,
    enableTextFormatting: Boolean,
    modifier: Modifier = Modifier,
) {
    val localView = LocalView.current
    var isVisible by rememberSaveable { mutableStateOf(state.showVoiceChatScreen) }

    BackHandler(enabled = isVisible) {
        isVisible = false
    }

    LaunchedEffect(Unit) {
        localView.hideKeyboard()
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            state.eventSink(MessageComposerEvents.VoiceChat.Dismiss)
        }
    }

    if (isVisible) {
       VoiceChatScreen(
           state = state
       )
    }
}
@Composable
private fun VoiceChatScreen(
    state: MessageComposerState,
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
                text = "Start",
                onClick = { state.eventSink(MessageComposerEvents.VoiceChat.Start) },
                modifier = Modifier.weight(1f)
            )

            Button(
                text = "Dismiss",
                onClick = { state.eventSink(MessageComposerEvents.VoiceChat.Dismiss) },
                modifier = Modifier.weight(1f)
            )
        }

    }
}

@PreviewsDayNight
@Composable
internal fun VoiceChatPreview() = ElementPreview {
    VoiceChatScreen(
        state = aMessageComposerState(
            showVoiceChatScreen = true,
        ),
    )
}
