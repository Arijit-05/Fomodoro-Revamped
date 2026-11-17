package com.arijit.pomodoro.ui.theme

import androidx.compose.ui.graphics.Color
import com.arijit.pomodoro.core.model.Phase

// Fomodoro Signature Brand Colors from Original Design
val FomodoroLightRed = Color(0xFFFFF2F2)
val FomodoroMediumRed = Color(0xFFFF7C7C)
val FomodoroDeepRed = Color(0xFF471515)

val FomodoroLightGreen = Color(0xFFF2FFF5)
val FomodoroMediumGreen = Color(0xFF8CE8A1)
val FomodoroDeepGreen = Color(0xFF14401D)

val FomodoroLightBlue = Color(0xFFD9EEFF)
val FomodoroMediumBlue = Color(0xFF8BCAFF)
val FomodoroDeepBlue = Color(0xFF153047)

val FomodoroAmoledBlack = Color(0xFF000000)
val FomodoroWaveformGreen = Color(0xFF2BB696)

// Compatibility fallbacks
val FocusPrimary = FomodoroMediumRed
val FocusOnPrimary = Color.White
val FocusPrimaryContainer = FomodoroLightRed
val FocusOnPrimaryContainer = FomodoroDeepRed
val FocusDarkBackground = Color(0xFF241616)
val FocusLightBackground = FomodoroLightRed

val ShortBreakPrimary = FomodoroMediumGreen
val ShortBreakOnPrimary = Color.White
val ShortBreakPrimaryContainer = FomodoroLightGreen
val ShortBreakOnPrimaryContainer = FomodoroDeepGreen
val ShortBreakDarkBackground = FomodoroDeepGreen
val ShortBreakLightBackground = FomodoroLightGreen

val LongBreakPrimary = FomodoroMediumBlue
val LongBreakOnPrimary = Color.White
val LongBreakPrimaryContainer = FomodoroLightBlue
val LongBreakOnPrimaryContainer = FomodoroDeepBlue
val LongBreakDarkBackground = FomodoroDeepBlue
val LongBreakLightBackground = FomodoroLightBlue

val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF121212)
val NeutralDarkSurface = Color(0xFF1C1B1F)
val NeutralLightSurface = Color(0xFFFFFBFE)

data class FomodoroPalette(
    val background: Color,
    val digitColor: Color,
    val pillBackground: Color,
    val pillContentColor: Color,
    val buttonBackground: Color,
    val buttonContentColor: Color,
    val controlIconColor: Color,
    val topBarWaveColor: Color,
    val topBarGearColor: Color,
    val actionTextColor: Color,
    val sessionTextColor: Color
)

fun getFomodoroPalette(
    phase: Phase,
    isDark: Boolean,
    presetId: String = when (phase) {
        Phase.FOCUS -> "coral_red"
        Phase.SHORT_BREAK -> "mint_green"
        Phase.LONG_BREAK -> "sky_blue"
    },
    amoledBlack: Boolean = false
): FomodoroPalette {
    val preset = SessionColorPresets.getPreset(presetId)
    val bg = when {
        isDark && amoledBlack -> FomodoroAmoledBlack
        isDark -> preset.darkBg
        else -> preset.lightBg
    }
    val digitColor = if (isDark) preset.darkText else preset.lightText
    val pillBg = if (isDark) preset.darkButtonBg else preset.lightText
    val pillContent = if (isDark) preset.darkButtonContent else Color.White
    val buttonBg = if (isDark) preset.darkButtonBg else preset.lightButtonBg
    val buttonContent = if (isDark) preset.darkButtonContent else preset.lightButtonContent
    val controlIcon = if (isDark) preset.darkControlIcon else preset.lightControlIcon
    val topBarWave = if (isDark) preset.darkButtonBg else preset.primaryColor
    val topBarGear = if (isDark) Color.White else preset.lightText
    val actionText = if (isDark) preset.darkText else preset.lightText
    val sessionText = if (isDark) preset.darkText else preset.lightText

    return FomodoroPalette(
        background = bg,
        digitColor = digitColor,
        pillBackground = pillBg,
        pillContentColor = pillContent,
        buttonBackground = buttonBg,
        buttonContentColor = buttonContent,
        controlIconColor = controlIcon,
        topBarWaveColor = topBarWave,
        topBarGearColor = topBarGear,
        actionTextColor = actionText,
        sessionTextColor = sessionText
    )
}
