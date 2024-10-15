/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import android.media.audiofx.Visualizer
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

@Composable
fun AudioVisualizerView(
    audioSessionId: Int?,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int
) {
    val visualizer = remember(audioSessionId) { audioSessionId?.let { Visualizer(it) } }
    val fftData = remember { mutableStateOf(ByteArray(Visualizer.getCaptureSizeRange()[1])) }
    val barCount = 64 // Number of bars in the circular graph

    DisposableEffect(visualizer) {
        visualizer?.let {
            it.setCaptureSize(fftData.value.size)
            it.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
                        // Not used in this example
                    }
                    override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
                        fft.copyInto(fftData.value)
                        fftData.value = fft.copyOf()
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true
            )
            it.enabled = true
        }
        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
        }
    }
    val volumeLevel = remember(fftData.value) {
        fftData.value.map { it.toInt() and 0xFF }.average().toFloat()
    }
    val animatedScale = animateFloatAsState(
        targetValue = 1.5f + (volumeLevel / 255f) * 1f, // Adjust scale factor as needed
        animationSpec = tween(durationMillis = 30), label = "" // Adjust animation speed
    )
    val animatedAlpha = animateFloatAsState(
        targetValue = 0.7f + (volumeLevel / 255f) * 0.2f, // Adjust alpha range as needed
        animationSpec = tween(durationMillis = 30), label = "" // Adjust animation speed
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale.value
                    scaleY = animatedScale.value
                    alpha = animatedAlpha.value
                },
            contentScale = ContentScale.Fit,
        )
    }
    /**
     * Circular bar visualizer
     */
//    Canvas(modifier = modifier.fillMaxSize()) {
//        val centerX = size.width / 2
//        val centerY = size.height / 2
//        val innerRadius = min(centerX, centerY) * 0.5f // Inner radius where the bars start
//        val outerRadius = min(centerX, centerY) * 0.8f // Outer radius where the bars end
//
//        for (i in 0 until barCount) {
//            val angle = (i * 360f / barCount) * (Math.PI / 180f).toFloat() // Angle in radians
//            val barHeight = when {
//                audioSessionId == null -> 0f
//                fftData.value.isNotEmpty() -> {
//                    fftData.value.getOrNull(i)?.toInt()?.coerceAtLeast(0)?.div(255f)?.times(outerRadius - innerRadius) ?: 0f
//                }
//                else -> 0f
//            }
//
//            // Calculate bar start and end points
//            val startX = centerX + cos(angle) * innerRadius
//            val startY = centerY + sin(angle) * innerRadius
//            val endX = centerX + cos(angle) * (innerRadius + barHeight)
//            val endY = centerY + sin(angle) * (innerRadius + barHeight)
//
//            drawLine(
//                color = if(isRecording) Color(0xFF90EE90) else Color(0xFFFFA500),
//                start = Offset(startX, startY),
//                end = Offset(endX, endY),
//                strokeWidth = 8f,
//                cap = StrokeCap.Round
//            )
//        }
//    }
}
