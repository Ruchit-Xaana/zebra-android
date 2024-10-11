/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.R
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
           enableButton = enableButton,
           audioSessionId = state.audioSessionId,
           rmsDB = state.rmsDB
       )
    }
}
@Composable
private fun VoiceChatScreen(
    state: VoiceMessageChatState,
    composerState: MessageComposerState,
    enableButton : Boolean,
    audioSessionId: Int?,
    rmsDB: Float?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_chat),
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.2f))
            Text(
                text = "Voice Chat",
                style = ElementTheme.typography.fontHeadingLgBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
                // Set a specific size for the visualizer
                AudioVisualizerView(
                    rmsDB = rmsDB,
                    audioSessionId = audioSessionId,
                    modifier = Modifier
                        .weight(0.55f) // Adjust the size as needed
                        .padding(bottom = 20.dp) // Add some spacing
                )

            Spacer(modifier = Modifier.weight(0.15f))
            if (state.errorMessage!=null) {
                Text(
                    text = state.errorMessage,
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(8.dp)
                        .border(2.dp, Color.Green, RoundedCornerShape(8.dp)) // Green border to grab attention
                        .background(
                            color = Color(0xAAFFFFFF), // Semi-transparent black background
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (state.isReady) {
                Text(
                    text = "Speak now...",
                    style = ElementTheme.typography.fontBodyLgRegular,
                    color = Color.Black,
                    modifier = Modifier
                        .padding(8.dp)
                        .border(2.dp, Color.Green, RoundedCornerShape(8.dp)) // Green border to grab attention
                        .background(
                            color = Color(0xAAFFFFFF), // Semi-transparent black background
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally).padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(80.dp, Alignment.End) ,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { if (enableButton) state.eventSink(VoiceChatEvents.Start) },
                    backgroundColor = if (!enableButton) {
                        ElementTheme.colors.iconDisabled
                    } else{
                            MaterialTheme.colors.secondary
                          },
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(100)
                )
                {
                    if (enableButton) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            modifier = Modifier.size(40.dp),
                            contentDescription = "Start chat",
                            tint = ElementTheme.colors.iconPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            modifier = Modifier.size(40.dp),
                            contentDescription = "Stop chat",
                            tint = Color.Red
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { if (enableButton) state.eventSink(VoiceChatEvents.Stop) },
                    modifier = Modifier.size(60.dp),
                    backgroundColor = if (enableButton) {
                        ElementTheme.colors.iconPrimary
                    } else {
                        ElementTheme.colors.iconDisabled
                    },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    shape = RoundedCornerShape(100)
                )
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = ElementTheme.materialColors.secondary
                    )
                }
                FloatingActionButton(
                    onClick = { if (enableButton) state.eventSink(VoiceChatEvents.Exit) },
                    modifier = Modifier.size(60.dp),
                    backgroundColor = if (enableButton) {
                        ElementTheme.colors.iconPrimary
                    } else {
                        ElementTheme.colors.iconDisabled
                    },
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    shape = RoundedCornerShape(100)
                )
                {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Dismiss",
                        tint = ElementTheme.materialColors.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.1f))

        }
    }
}
