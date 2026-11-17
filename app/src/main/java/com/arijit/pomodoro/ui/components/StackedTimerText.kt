package com.arijit.pomodoro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arijit.pomodoro.ui.theme.PoppinsBlack
import java.util.Locale

@Composable
fun StackedTimerText(
    remainingMillis: Long,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 135.sp
) {
    val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val minStr = String.format(Locale.ROOT, "%02d", minutes)
    val secStr = String.format(Locale.ROOT, "%02d", seconds)

    val digitStyle = TextStyle(
        fontFamily = PoppinsBlack,
        fontWeight = FontWeight.Black,
        fontSize = fontSize,
        lineHeight = fontSize * 0.72f,
        letterSpacing = (-3).sp,
        fontFeatureSettings = "tnum",
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

    // Negative spacing between the two numbers pulls seconds right below minutes tightly
    val verticalOverlap = (-fontSize.value * 0.38f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(verticalOverlap),
        modifier = modifier
    ) {
        Text(
            text = minStr,
            style = digitStyle,
            color = color,
            textAlign = TextAlign.Center
        )
        Text(
            text = secStr,
            style = digitStyle,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
