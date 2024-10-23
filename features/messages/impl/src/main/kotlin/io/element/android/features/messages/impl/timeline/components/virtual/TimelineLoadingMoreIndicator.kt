/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.virtual

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.LinearProgressIndicator
import io.element.android.libraries.matrix.api.timeline.Timeline

@Composable
internal fun TimelineLoadingMoreIndicator(
    direction: Timeline.PaginationDirection,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        when (direction) {
            Timeline.PaginationDirection.FORWARDS -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .height(1.dp)
                )
            }
            Timeline.PaginationDirection.BACKWARDS -> {
                BlinkingDots()
//                CircularProgressIndicator(
//                    strokeWidth = 2.dp,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )
            }
        }
    }
}
@Composable
fun BlinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Dot(alpha = dot1Alpha)
        Dot(alpha = dot2Alpha)
        Dot(alpha = dot3Alpha)
    }
}

@Composable
fun Dot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = alpha))
    )
}

@PreviewsDayNight
@Composable
internal fun TimelineLoadingMoreIndicatorPreview() = ElementPreview {
    Column(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimelineLoadingMoreIndicator(Timeline.PaginationDirection.BACKWARDS)
        TimelineLoadingMoreIndicator(Timeline.PaginationDirection.FORWARDS)
    }
}
