package com.arijit.pomodoro.ui.aod

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.ui.components.TabularTimerText
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AodScreen(
    snapshot: TimerSnapshot,
    onExitAod: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pixel-shift offset updated once every 60 seconds (prevents OLED burn-in, #20)
    var offsetX by remember { mutableIntStateOf(0) }
    var offsetY by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L) // 1 minute
            offsetX = Random.nextInt(-16, 17)
            offsetY = Random.nextInt(-16, 17)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExitAod
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.offset { IntOffset(offsetX, offsetY) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = snapshot.phase.name.replace('_', ' '),
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Thin stroke dim countdown (updated only 1 Hz via timer, zero continuous animation)
            TabularTimerText(
                remainingMillis = snapshot.remainingMillis,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Thin,
                    fontSize = 64.sp,
                    letterSpacing = (-1).sp,
                    fontFeatureSettings = "tnum"
                ),
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap anywhere to wake",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.25f)
                )
            )
        }
    }
}

