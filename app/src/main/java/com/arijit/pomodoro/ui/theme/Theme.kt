package com.arijit.pomodoro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arijit.pomodoro.core.datastore.ThemeMode
import com.arijit.pomodoro.core.model.Phase

val LocalFomodoroPalette = staticCompositionLocalOf<FomodoroPalette> {
    getFomodoroPalette(Phase.FOCUS, false)
}

@Composable
fun FomodoroTheme(
    phase: Phase = Phase.FOCUS,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    presetId: String = when (phase) {
        Phase.FOCUS -> "coral_red"
        Phase.SHORT_BREAK -> "mint_green"
        Phase.LONG_BREAK -> "sky_blue"
    },
    dynamicColor: Boolean = false,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val palette = getFomodoroPalette(
        phase = phase,
        isDark = isDark,
        presetId = presetId,
        amoledBlack = amoledBlack
    )
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> {
            darkColorScheme(
                primary = palette.buttonBackground,
                onPrimary = palette.buttonContentColor,
                background = palette.background,
                surface = palette.background,
                onBackground = palette.digitColor,
                onSurface = palette.digitColor
            )
        }
        else -> {
            lightColorScheme(
                primary = palette.buttonBackground,
                onPrimary = palette.buttonContentColor,
                background = palette.background,
                surface = palette.background,
                onBackground = palette.digitColor,
                onSurface = palette.digitColor
            )
        }
    }

    CompositionLocalProvider(LocalFomodoroPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
