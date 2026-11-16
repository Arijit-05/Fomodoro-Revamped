package com.arijit.pomodoro.core.model

data class TimerSnapshot(
    val status: TimerStatus = TimerStatus.IDLE,
    val phase: Phase = Phase.FOCUS,
    val cycleState: CycleState = CycleState(),
    val totalDurationMillis: Long = 25 * 60 * 1000L,
    val remainingMillis: Long = 25 * 60 * 1000L,
    val targetEndTimeWallClock: Long = 0L,
    val targetEndTimeElapsedRealtime: Long = 0L,
    val bootCount: Int = 0,
    val isSnoozed: Boolean = false,
    val pendingNextPhaseSettings: TimerSettings? = null
) {
    val progress: Float get() = if (totalDurationMillis > 0) {
        1f - (remainingMillis.toFloat() / totalDurationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isRunning: Boolean get() = status == TimerStatus.RUNNING
    val isPaused: Boolean get() = status == TimerStatus.PAUSED
    val isIdle: Boolean get() = status == TimerStatus.IDLE
    val isPhaseEnded: Boolean get() = status == TimerStatus.PHASE_ENDED
}

