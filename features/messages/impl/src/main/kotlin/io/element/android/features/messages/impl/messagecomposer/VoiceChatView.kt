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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.voicemessages.chat.VoiceChatEvents
import io.element.android.features.messages.impl.voicemessages.chat.VoiceMessageChatState
import io.element.android.libraries.androidutils.ui.hideKeyboard
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.theme.customScrimColor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceChatView(
    state: VoiceMessageChatState,
    composerState: MessageComposerState,
    modifier: Modifier = Modifier,
) {

    val localView = LocalView.current
    var isVisible by rememberSaveable { mutableStateOf(composerState.showVoiceChatScreen) }

    BackHandler(enabled = isVisible) {
        isVisible = false
    }

    LaunchedEffect(composerState.showVoiceChatScreen) {
        isVisible = if (composerState.showVoiceChatScreen) {
            localView.hideKeyboard()
            true
        } else {
            false
        }
    }
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            composerState.eventSink(MessageComposerEvents.VoiceChat.Dismiss)
            state.eventSink(VoiceChatEvents.Disconnect)
        }
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val bottomSheetTopPadding = screenHeight * 1/ 3

    if (isVisible) {
        ModalBottomSheet(
            modifier = modifier.systemBarsPadding()
                .padding(top = bottomSheetTopPadding),
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            scrimColor =ElementTheme.colors.customScrimColor.copy(alpha = 0.8f),
            onDismissRequest = { isVisible = false }
        ) {
                VoiceChatScreen(
                    state = state,
                    composerState = composerState,
                    enableButton = state.canStartSession,
                    audioSessionId = state.audioSessionId,
                )
        }
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
            .navigationBarsPadding()
            .imePadding()
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
            AudioVisualizerView(
                audioSessionId = audioSessionId,
                isRecording = state.isRecording,
                modifier = Modifier
                    .weight(0.55f)
                    .padding(bottom = 20.dp)
            )

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
                        Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(100)
                )
                {
                        Icon(
                            imageVector = Icons.Default.Call,
                            modifier = Modifier.size(40.dp),
                            contentDescription = "Start Session",
                            tint = ElementTheme.colors.iconPrimary
                        )
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
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Dismiss Session",
                        tint = ElementTheme.colors.iconPrimary
                    )
                }
            }
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
