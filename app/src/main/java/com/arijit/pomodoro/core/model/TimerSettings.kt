package com.arijit.pomodoro.core.model

data class TimerSettings(
    val focusDurationMinutes: Int = 25,
    val shortBreakDurationMinutes: Int = 5,
    val longBreakDurationMinutes: Int = 15,
    val sessionsPerCycle: Int = 4,
    val autoStartBreaks: Boolean = false,
    val autoStartFocus: Boolean = false,
    val snoozeDurationMinutes: Int = 5
) {
    fun durationForPhase(phase: Phase): Long {
        val minutes = when (phase) {
            Phase.FOCUS -> focusDurationMinutes
            Phase.SHORT_BREAK -> shortBreakDurationMinutes
            Phase.LONG_BREAK -> longBreakDurationMinutes
        }
        return minutes * 60 * 1000L
    }

    val snoozeDurationMillis: Long get() = snoozeDurationMinutes * 60 * 1000L
}

