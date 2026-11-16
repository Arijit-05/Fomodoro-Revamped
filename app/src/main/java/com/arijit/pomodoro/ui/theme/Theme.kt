package com.arijit.pomodoro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arijit.pomodoro.core.datastore.ThemeMode
import com.arijit.pomodoro.core.model.Phase

@Composable
fun FomodoroTheme(
    phase: Phase = Phase.FOCUS,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !amoledBlack -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> {
            val baseBackground = if (amoledBlack) AmoledBackground else when (phase) {
                Phase.FOCUS -> FocusDarkBackground
                Phase.SHORT_BREAK -> ShortBreakDarkBackground
                Phase.LONG_BREAK -> LongBreakDarkBackground
            }
            val primaryColor = when (phase) {
                Phase.FOCUS -> FocusPrimary
                Phase.SHORT_BREAK -> ShortBreakPrimary
                Phase.LONG_BREAK -> LongBreakPrimary
            }
            darkColorScheme(
                primary = primaryColor,
                onPrimary = Color.White,
                background = baseBackground,
                surface = if (amoledBlack) AmoledSurface else baseBackground,
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
        else -> {
            val baseBackground = when (phase) {
                Phase.FOCUS -> FocusLightBackground
                Phase.SHORT_BREAK -> ShortBreakLightBackground
                Phase.LONG_BREAK -> LongBreakLightBackground
            }
            val primaryColor = when (phase) {
                Phase.FOCUS -> FocusPrimary
                Phase.SHORT_BREAK -> ShortBreakPrimary
                Phase.LONG_BREAK -> LongBreakPrimary
            }
            lightColorScheme(
                primary = primaryColor,
                onPrimary = Color.White,
                background = baseBackground,
                surface = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

