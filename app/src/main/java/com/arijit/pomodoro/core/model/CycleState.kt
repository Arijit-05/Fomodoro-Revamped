package com.arijit.pomodoro.core.model

data class CycleState(
    val currentSession: Int = 1,
    val totalSessions: Int = 4,
    val completedCycles: Int = 0
) {
    val isLastSessionInCycle: Boolean get() = currentSession >= totalSessions

    fun advanceAfterFocus(): CycleState {
        // After focus, session index stays same until break is finished
        return this
    }

    fun advanceAfterShortBreak(): CycleState {
        val nextSession = currentSession + 1
        return copy(currentSession = nextSession)
    }

    fun advanceAfterLongBreak(): CycleState {
        return copy(
            currentSession = 1,
            completedCycles = completedCycles + 1
        )
    }

    fun reset(): CycleState {
        return copy(currentSession = 1)
    }
}

