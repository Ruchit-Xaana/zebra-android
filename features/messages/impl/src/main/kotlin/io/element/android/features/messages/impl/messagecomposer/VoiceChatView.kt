/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay

@Composable
internal fun VoiceChatView(
    state: VoiceMessageChatState,
    composerState: MessageComposerState,
) {
    val localView = LocalView.current
    var isVisible by rememberSaveable { mutableStateOf(composerState.showVoiceChatScreen) }

    BackHandler(enabled = isVisible) {
        isVisible = false
    }

    LaunchedEffect(Unit) {
        localView.hideKeyboard()
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            composerState.eventSink(MessageComposerEvents.VoiceChat.Dismiss)
            state.eventSink(VoiceChatEvents.Disconnect)
        }
    }

    if (isVisible) {
       VoiceChatScreen(
           state = state,
           composerState = composerState,
           enableButton = state.canStartSession,
           audioSessionId = state.audioSessionId,
       )
    }
}
@Composable
private fun VoiceChatScreen(
    state: VoiceMessageChatState,
    composerState: MessageComposerState,
    enableButton : Boolean,
    audioSessionId: Int?,
) {
    var showCustomToast by remember { mutableStateOf(false) }
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

            AudioVisualizerView(
                audioSessionId = audioSessionId,
                isRecording = state.isRecording,
                modifier = Modifier
                    .weight(0.55f)
                    .padding(bottom = 20.dp)
            )

            Spacer(modifier = Modifier.weight(0.15f))

            if (showCustomToast && state.toastMessage != null) {
                CustomToast(
                    message = state.toastMessage,
                    onDismiss = { showCustomToast = false }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally).padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(80.dp, Alignment.End) ,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingActionButton(
                    onClick = { if (enableButton) state.eventSink(VoiceChatEvents.Connect) },
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
                    onClick = { state.eventSink(VoiceChatEvents.Disconnect) },
                    modifier = Modifier.size(60.dp),
                    backgroundColor = Color(0xFFFF1744),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                    shape = RoundedCornerShape(100)
                )
                {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.Black
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.1f))
            LaunchedEffect(state.toastMessage) {
                if (state.toastMessage != null) {
                    showCustomToast = true
                }
            }

        }
    }
}
@Composable
fun CustomToast(
    message: String,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2000L,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(durationMillis)
        isVisible = false
        onDismiss()
    }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = modifier
                .padding(16.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Black
            )
        }
    }
}
