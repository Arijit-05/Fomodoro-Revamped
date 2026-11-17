package com.arijit.pomodoro.core.datastore

data class UserPreferences(
    // Timer durations (in minutes)
    val focusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsPerCycle: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    val snoozeDurationMinutes: Int = 5,

    // Appearance
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val amoledBlack: Boolean = false,
    val keepScreenOn: Boolean = false,
    val ultraFocusMode: Boolean = false,
    val ambientAodMode: Boolean = false,
    val focusThemeColor: String = "coral_red",
    val shortBreakThemeColor: String = "mint_green",
    val longBreakThemeColor: String = "sky_blue",


    // Sound
    val endOfFocusSoundUri: String? = null,
    val endOfBreakSoundUri: String? = null,
    val tickingSoundStyle: String = "none", // none, mechanical, digital, soft
    val tickingVolume: Float = 0.5f,
    val tickOnlyDuringFocus: Boolean = true,
    val ambientSoundStyle: String = "none", // none, rain, forest, ocean, fireplace, brown_noise, cafe, custom
    val ambientSoundUri: String? = null,
    val ambientVolume: Float = 0.7f,
    val pauseAmbientDuringBreak: Boolean = true,

    // Feedback
    val hapticLevel: HapticLevel = HapticLevel.STANDARD,
    val flashNotification: Boolean = false,

    // Notifications & Live Updates
    val liveUpdatesEnabled: Boolean = true,

    // Break prompts
    val breakPrompts: List<String> = defaultBreakPrompts
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class HapticLevel {
    OFF, SUBTLE, STANDARD, STRONG
}

val defaultBreakPrompts = listOf(
    "Stretch your arms and shoulders",
    "Drink a glass of water",
    "Look at something 20 feet away for 20 seconds",
    "Take 5 deep breaths",
    "Walk around for a minute",
    "Relax your eyes and neck muscles",
    "Stand up and stretch your back"
)

