package com.arijit.pomodoro.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TabularTimerText(
    remainingMillis: Long,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
    val minutes = (totalSeconds / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()

    val minStr = String.format("%02d", minutes)
    val secStr = String.format("%02d", seconds)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minutes Tens
        RollingDigit(digit = minStr[0], style = textStyle, color = color)
        // Minutes Ones
        RollingDigit(digit = minStr[1], style = textStyle, color = color)

        // Colon separator
        Text(
            text = ":",
            style = textStyle,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(24.dp)
        )

        // Seconds Tens
        RollingDigit(digit = secStr[0], style = textStyle, color = color)
        // Seconds Ones
        RollingDigit(digit = secStr[1], style = textStyle, color = color)
    }
}

@Composable
fun RollingDigit(
    digit: Char,
    style: TextStyle,
    color: Color
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            slideInVertically { height -> -height } togetherWith slideOutVertically { height -> height }
        },
        label = "DigitRoll"
    ) { targetDigit ->
        Text(
            text = targetDigit.toString(),
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(42.dp)
        )
    }
}

