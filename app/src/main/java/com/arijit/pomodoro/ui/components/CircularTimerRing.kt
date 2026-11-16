package com.arijit.pomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arijit.pomodoro.core.model.Phase

@Composable
fun CircularTimerRing(
    progress: Float,
    phase: Phase,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    // Spring physics animation for progress sweep
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "RingProgress"
    )

    // Smooth color morphing on phase changes
    val targetColor = when (phase) {
        Phase.FOCUS -> MaterialTheme.colorScheme.primary
        Phase.SHORT_BREAK -> Color(0xFF43A047)
        Phase.LONG_BREAK -> Color(0xFF1E88E5)
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PhaseColor"
    )

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val strokePx = strokeWidth.toPx()
                    val diameter = minOf(this.size.width, this.size.height) - strokePx
                    val topLeftOffset = (strokePx / 2f)

                    onDrawBehind {
                        // 1. Background Track Ring
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(topLeftOffset, topLeftOffset),
                            size = androidx.compose.ui.geometry.Size(diameter, diameter),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )

                        // 2. Foreground Animated Sweep Ring
                        val sweep = animatedProgress * 360f
                        if (sweep > 0f) {
                            drawArc(
                                color = animatedColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(topLeftOffset, topLeftOffset),
                                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
        ) {}

        // Center content (timer digits, labels)
        content()
    }
}

