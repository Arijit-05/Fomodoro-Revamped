package com.arijit.pomodoro.core.timer

import com.arijit.pomodoro.core.model.CycleState
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSettings
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus

class TimerStateMachine {

    fun createInitialSnapshot(settings: TimerSettings, bootCount: Int = 0): TimerSnapshot {
        val focusDuration = settings.durationForPhase(Phase.FOCUS)
        return TimerSnapshot(
            status = TimerStatus.IDLE,
            phase = Phase.FOCUS,
            cycleState = CycleState(currentSession = 1, totalSessions = settings.sessionsPerCycle),
            totalDurationMillis = focusDuration,
            remainingMillis = focusDuration,
            targetEndTimeWallClock = 0L,
            targetEndTimeElapsedRealtime = 0L,
            bootCount = bootCount
        )
    }

    fun start(
        current: TimerSnapshot,
        elapsedRealtime: Long,
        wallClockTime: Long,
        bootCount: Int,
        settings: TimerSettings
    ): TimerSnapshot {
        val duration = if (current.status == TimerStatus.PHASE_ENDED || current.remainingMillis <= 0) {
            settings.durationForPhase(current.phase)
        } else {
            current.remainingMillis
        }

        return current.copy(
            status = TimerStatus.RUNNING,
            totalDurationMillis = duration,
            remainingMillis = duration,
            targetEndTimeElapsedRealtime = elapsedRealtime + duration,
            targetEndTimeWallClock = wallClockTime + duration,
            bootCount = bootCount,
            isSnoozed = false
        )
    }

    fun pause(
        current: TimerSnapshot,
        elapsedRealtime: Long
    ): TimerSnapshot {
        if (current.status != TimerStatus.RUNNING) return current

        val derivedRemaining = (current.targetEndTimeElapsedRealtime - elapsedRealtime).coerceAtLeast(0L)
        return current.copy(
            status = TimerStatus.PAUSED,
            remainingMillis = derivedRemaining,
            targetEndTimeElapsedRealtime = 0L,
            targetEndTimeWallClock = 0L
        )
    }

    fun resume(
        current: TimerSnapshot,
        elapsedRealtime: Long,
        wallClockTime: Long,
        bootCount: Int
    ): TimerSnapshot {
        if (current.status != TimerStatus.PAUSED) return current

        val remaining = current.remainingMillis.coerceAtLeast(0L)
        return current.copy(
            status = TimerStatus.RUNNING,
            targetEndTimeElapsedRealtime = elapsedRealtime + remaining,
            targetEndTimeWallClock = wallClockTime + remaining,
            bootCount = bootCount
        )
    }

    fun tick(
        current: TimerSnapshot,
        elapsedRealtime: Long
    ): TimerSnapshot {
        if (current.status != TimerStatus.RUNNING) return current

        val remaining = (current.targetEndTimeElapsedRealtime - elapsedRealtime).coerceAtLeast(0L)
        return if (remaining <= 0L) {
            current.copy(
                status = TimerStatus.PHASE_ENDED,
                remainingMillis = 0L,
                targetEndTimeElapsedRealtime = 0L,
                targetEndTimeWallClock = 0L
            )
        } else {
            current.copy(remainingMillis = remaining)
        }
    }

    fun phaseCompleted(current: TimerSnapshot): TimerSnapshot {
        return current.copy(
            status = TimerStatus.PHASE_ENDED,
            remainingMillis = 0L,
            targetEndTimeElapsedRealtime = 0L,
            targetEndTimeWallClock = 0L
        )
    }

    fun startNextPhase(
        current: TimerSnapshot,
        elapsedRealtime: Long,
        wallClockTime: Long,
        bootCount: Int,
        activeSettings: TimerSettings
    ): TimerSnapshot {
        val effectiveSettings = current.pendingNextPhaseSettings ?: activeSettings
        val nextPhase = computeNextPhase(current.phase, current.cycleState)
        val nextCycleState = computeNextCycleState(current.phase, current.cycleState)
        val nextDuration = effectiveSettings.durationForPhase(nextPhase)

        return current.copy(
            status = TimerStatus.RUNNING,
            phase = nextPhase,
            cycleState = nextCycleState,
            totalDurationMillis = nextDuration,
            remainingMillis = nextDuration,
            targetEndTimeElapsedRealtime = elapsedRealtime + nextDuration,
            targetEndTimeWallClock = wallClockTime + nextDuration,
            bootCount = bootCount,
            isSnoozed = false,
            pendingNextPhaseSettings = null
        )
    }

    fun skip(
        current: TimerSnapshot,
        elapsedRealtime: Long,
        wallClockTime: Long,
        bootCount: Int,
        activeSettings: TimerSettings
    ): TimerSnapshot {
        val effectiveSettings = current.pendingNextPhaseSettings ?: activeSettings
        val nextPhase = computeNextPhase(current.phase, current.cycleState)
        val nextCycleState = computeNextCycleState(current.phase, current.cycleState)
        val nextDuration = effectiveSettings.durationForPhase(nextPhase)

        val shouldRun = current.status == TimerStatus.RUNNING
        val status = if (shouldRun) TimerStatus.RUNNING else TimerStatus.IDLE

        return current.copy(
            status = status,
            phase = nextPhase,
            cycleState = nextCycleState,
            totalDurationMillis = nextDuration,
            remainingMillis = nextDuration,
            targetEndTimeElapsedRealtime = if (shouldRun) elapsedRealtime + nextDuration else 0L,
            targetEndTimeWallClock = if (shouldRun) wallClockTime + nextDuration else 0L,
            bootCount = bootCount,
            isSnoozed = false,
            pendingNextPhaseSettings = null
        )
    }

    fun reset(
        activeSettings: TimerSettings,
        bootCount: Int = 0
    ): TimerSnapshot {
        return createInitialSnapshot(activeSettings, bootCount)
    }

    fun snooze(
        current: TimerSnapshot,
        snoozeDurationMillis: Long,
        elapsedRealtime: Long,
        wallClockTime: Long,
        bootCount: Int
    ): TimerSnapshot {
        return current.copy(
            status = TimerStatus.RUNNING,
            totalDurationMillis = snoozeDurationMillis,
            remainingMillis = snoozeDurationMillis,
            targetEndTimeElapsedRealtime = elapsedRealtime + snoozeDurationMillis,
            targetEndTimeWallClock = wallClockTime + snoozeDurationMillis,
            bootCount = bootCount,
            isSnoozed = true
        )
    }

    fun updateSettingsMidPhase(
        current: TimerSnapshot,
        newSettings: TimerSettings
    ): TimerSnapshot {
        return if (current.status == TimerStatus.IDLE) {
            val updatedCycle = current.cycleState.copy(totalSessions = newSettings.sessionsPerCycle)
            val updatedDuration = newSettings.durationForPhase(current.phase)
            current.copy(
                cycleState = updatedCycle,
                totalDurationMillis = updatedDuration,
                remainingMillis = updatedDuration,
                pendingNextPhaseSettings = null
            )
        } else {
            // Keep running phase untouched, queue for next phase
            current.copy(pendingNextPhaseSettings = newSettings)
        }
    }

    fun restore(
        saved: TimerSnapshot,
        currentElapsed: Long,
        currentWallClock: Long,
        currentBootCount: Int,
        activeSettings: TimerSettings
    ): TimerSnapshot {
        if (saved.status != TimerStatus.RUNNING) {
            return saved.copy(bootCount = currentBootCount)
        }

        // Did a reboot occur?
        val timeRemaining = if (currentBootCount != saved.bootCount) {
            // Clock reset: use wall clock
            saved.targetEndTimeWallClock - currentWallClock
        } else {
            // Same boot session: elapsedRealtime is monotonic and exact
            saved.targetEndTimeElapsedRealtime - currentElapsed
        }

        return if (timeRemaining <= 0L) {
            // Phase completed while app was dead
            saved.copy(
                status = TimerStatus.PHASE_ENDED,
                remainingMillis = 0L,
                targetEndTimeElapsedRealtime = 0L,
                targetEndTimeWallClock = 0L,
                bootCount = currentBootCount
            )
        } else {
            saved.copy(
                status = TimerStatus.RUNNING,
                remainingMillis = timeRemaining,
                targetEndTimeElapsedRealtime = currentElapsed + timeRemaining,
                targetEndTimeWallClock = currentWallClock + timeRemaining,
                bootCount = currentBootCount
            )
        }
    }

    private fun computeNextPhase(currentPhase: Phase, cycleState: CycleState): Phase {
        return when (currentPhase) {
            Phase.FOCUS -> {
                if (cycleState.isLastSessionInCycle) Phase.LONG_BREAK else Phase.SHORT_BREAK
            }
            Phase.SHORT_BREAK, Phase.LONG_BREAK -> Phase.FOCUS
        }
    }

    private fun computeNextCycleState(currentPhase: Phase, cycleState: CycleState): CycleState {
        return when (currentPhase) {
            Phase.FOCUS -> cycleState.advanceAfterFocus()
            Phase.SHORT_BREAK -> cycleState.advanceAfterShortBreak()
            Phase.LONG_BREAK -> cycleState.advanceAfterLongBreak()
        }
    }
}

