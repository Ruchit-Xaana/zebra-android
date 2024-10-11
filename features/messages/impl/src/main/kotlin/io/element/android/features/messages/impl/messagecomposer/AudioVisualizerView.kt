/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.messagecomposer

import android.media.audiofx.Visualizer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AudioVisualizerView(
    audioSessionId: Int?,
    isRecording: Boolean,
    modifier: Modifier = Modifier
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
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val innerRadius = min(centerX, centerY) * 0.5f // Inner radius where the bars start
        val outerRadius = min(centerX, centerY) * 0.8f // Outer radius where the bars end

        for (i in 0 until barCount) {
            val angle = (i * 360f / barCount) * (Math.PI / 180f).toFloat() // Angle in radians
            val barHeight = when {
                audioSessionId == null -> 0f
                fftData.value.isNotEmpty() -> {
                    fftData.value.getOrNull(i)?.toInt()?.coerceAtLeast(0)?.div(255f)?.times(outerRadius - innerRadius) ?: 0f
                }
                else -> 0f
            }

            // Calculate bar start and end points
            val startX = centerX + cos(angle) * innerRadius
            val startY = centerY + sin(angle) * innerRadius
            val endX = centerX + cos(angle) * (innerRadius + barHeight)
            val endY = centerY + sin(angle) * (innerRadius + barHeight)

            drawLine(
                color = if(isRecording) Color(0xFF90EE90) else Color(0xFFFFA500),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }
    }
}
