package com.arijit.pomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arijit.pomodoro.core.model.CycleState

@Composable
fun SessionDots(
    cycleState: CycleState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(cycleState.totalSessions) { index ->
            val sessionNum = index + 1
            val isCompleted = sessionNum < cycleState.currentSession
            val isCurrent = sessionNum == cycleState.currentSession

            val scale by animateFloatAsState(
                targetValue = if (isCurrent) 1.25f else 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "DotScale"
            )

            val color by animateColorAsState(
                targetValue = when {
                    isCompleted || isCurrent -> activeColor
                    else -> inactiveColor
                },
                label = "DotColor"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(scale)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}

