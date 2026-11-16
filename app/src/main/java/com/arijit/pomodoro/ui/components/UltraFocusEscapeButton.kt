package com.arijit.pomodoro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arijit.pomodoro.system.HapticManager
import kotlinx.coroutines.launch

@Composable
fun UltraFocusEscapeButton(
    onExit: () -> Unit,
    hapticManager: HapticManager,
    modifier: Modifier = Modifier,
    holdDurationMs: Int = 1500
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .size(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val animJob = scope.launch {
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = holdDurationMs, easing = LinearEasing)
                            ) {
                                hapticManager.ultraFocusRamp(this.value)
                            }
                            if (progress.value >= 1f) {
                                hapticManager.ultraFocusExitSuccess()
                                onExit()
                            }
                        }

                        val released = tryAwaitRelease()
                        if (!animJob.isCompleted) {
                            animJob.cancel()
                            scope.launch {
                                progress.animateTo(0f, animationSpec = tween(200))
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Background track and progress ring
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        val progressColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = 3.dp.toPx()
            // Track
            drawCircle(
                color = trackColor,
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(width = stroke)
            )
            // Progress arc
            if (progress.value > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = progress.value * 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Hold to exit Ultra Focus",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
    }
}

