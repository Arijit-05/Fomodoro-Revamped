package com.arijit.pomodoro.core.timer

import com.arijit.pomodoro.core.model.CycleState
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSettings
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerStateMachineTest {

    private lateinit var stateMachine: TimerStateMachine
    private val defaultSettings = TimerSettings(
        focusDurationMinutes = 25,
        shortBreakDurationMinutes = 5,
        longBreakDurationMinutes = 15,
        sessionsPerCycle = 4,
        snoozeDurationMinutes = 5
    )

    @Before
    fun setUp() {
        stateMachine = TimerStateMachine()
    }

    @Test
    fun `initial state is idle and matches settings`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        assertEquals(TimerStatus.IDLE, initial.status)
        assertEquals(Phase.FOCUS, initial.phase)
        assertEquals(1, initial.cycleState.currentSession)
        assertEquals(4, initial.cycleState.totalSessions)
        assertEquals(25 * 60 * 1000L, initial.remainingMillis)
        assertEquals(25 * 60 * 1000L, initial.totalDurationMillis)
        assertEquals(0f, initial.progress, 0.001f)
    }

    @Test
    fun `start transitions to running with exact target end times`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val elapsed = 10_000L
        val wall = 1_000_000L

        val started = stateMachine.start(initial, elapsed, wall, bootCount = 1, defaultSettings)
        assertEquals(TimerStatus.RUNNING, started.status)
        val duration = 25 * 60 * 1000L
        assertEquals(elapsed + duration, started.targetEndTimeElapsedRealtime)
        assertEquals(wall + duration, started.targetEndTimeWallClock)
        assertEquals(duration, started.remainingMillis)
    }

    @Test
    fun `pause and resume preserves remaining time without drift`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        var elapsed = 10_000L
        var wall = 1_000_000L
        val started = stateMachine.start(initial, elapsed, wall, bootCount = 1, defaultSettings)

        // Advance 10 minutes (600,000 ms)
        elapsed += 600_000L
        wall += 600_000L

        val paused = stateMachine.pause(started, elapsed)
        assertEquals(TimerStatus.PAUSED, paused.status)
        val expectedRemaining = 15 * 60 * 1000L
        assertEquals(expectedRemaining, paused.remainingMillis)

        // Paused for 5 minutes (user goes away)
        elapsed += 300_000L
        wall += 300_000L

        val resumed = stateMachine.resume(paused, elapsed, wall, bootCount = 1)
        assertEquals(TimerStatus.RUNNING, resumed.status)
        assertEquals(expectedRemaining, resumed.remainingMillis)
        assertEquals(elapsed + expectedRemaining, resumed.targetEndTimeElapsedRealtime)
        assertEquals(wall + expectedRemaining, resumed.targetEndTimeWallClock)
    }

    @Test
    fun `tick decrements remaining time and transitions to phase ended on zero`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        var elapsed = 10_000L
        val wall = 1_000_000L
        val started = stateMachine.start(initial, elapsed, wall, bootCount = 1, defaultSettings)

        // Tick midway
        elapsed += 60_000L
        val tickedMid = stateMachine.tick(started, elapsed)
        assertEquals(TimerStatus.RUNNING, tickedMid.status)
        assertEquals(24 * 60 * 1000L, tickedMid.remainingMillis)

        // Tick past expiration
        elapsed += 25 * 60 * 1000L
        val tickedEnd = stateMachine.tick(started, elapsed)
        assertEquals(TimerStatus.PHASE_ENDED, tickedEnd.status)
        assertEquals(0L, tickedEnd.remainingMillis)
    }

    @Test
    fun `cycle progression from session 1 to 4 and long break`() {
        var snapshot = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        var elapsed = 10_000L
        var wall = 1_000_000L

        // Session 1 (Focus)
        snapshot = stateMachine.start(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.FOCUS, snapshot.phase)
        assertEquals(1, snapshot.cycleState.currentSession)

        // Finish Focus 1 -> Next is Short Break
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.SHORT_BREAK, snapshot.phase)
        assertEquals(1, snapshot.cycleState.currentSession)
        assertEquals(5 * 60 * 1000L, snapshot.remainingMillis)

        // Finish Short Break 1 -> Next is Focus 2
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.FOCUS, snapshot.phase)
        assertEquals(2, snapshot.cycleState.currentSession)

        // Focus 2 -> Short Break 2
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.SHORT_BREAK, snapshot.phase)

        // Short Break 2 -> Focus 3
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.FOCUS, snapshot.phase)
        assertEquals(3, snapshot.cycleState.currentSession)

        // Focus 3 -> Short Break 3
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.SHORT_BREAK, snapshot.phase)

        // Short Break 3 -> Focus 4 (Last session of cycle!)
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.FOCUS, snapshot.phase)
        assertEquals(4, snapshot.cycleState.currentSession)
        assertTrue(snapshot.cycleState.isLastSessionInCycle)

        // Finish Focus 4 -> MUST transition to LONG_BREAK
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.LONG_BREAK, snapshot.phase)
        assertEquals(15 * 60 * 1000L, snapshot.remainingMillis)

        // Finish Long Break -> Cycle completed! Resets to session 1, increments completedCycles
        snapshot = stateMachine.phaseCompleted(snapshot)
        snapshot = stateMachine.startNextPhase(snapshot, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.FOCUS, snapshot.phase)
        assertEquals(1, snapshot.cycleState.currentSession)
        assertEquals(1, snapshot.cycleState.completedCycles)
    }

    @Test
    fun `skip advances to next phase correctly`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val elapsed = 10_000L
        val wall = 1_000_000L

        // Skip while idle
        val skippedToShortBreak = stateMachine.skip(initial, elapsed, wall, 1, defaultSettings)
        assertEquals(TimerStatus.IDLE, skippedToShortBreak.status)
        assertEquals(Phase.SHORT_BREAK, skippedToShortBreak.phase)
        assertEquals(5 * 60 * 1000L, skippedToShortBreak.remainingMillis)

        // Skip while running (stays running on next phase)
        val runningBreak = stateMachine.start(skippedToShortBreak, elapsed, wall, 1, defaultSettings)
        val skippedToFocus2 = stateMachine.skip(runningBreak, elapsed, wall, 1, defaultSettings)
        assertEquals(TimerStatus.RUNNING, skippedToFocus2.status)
        assertEquals(Phase.FOCUS, skippedToFocus2.phase)
        assertEquals(2, skippedToFocus2.cycleState.currentSession)
    }

    @Test
    fun `snooze extends phase by configured minutes`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val elapsed = 10_000L
        val wall = 1_000_000L
        val running = stateMachine.start(initial, elapsed, wall, 1, defaultSettings)
        val ended = stateMachine.phaseCompleted(running)

        // Snooze 5 minutes
        val snoozed = stateMachine.snooze(ended, 5 * 60 * 1000L, elapsed, wall, 1)
        assertEquals(TimerStatus.RUNNING, snoozed.status)
        assertTrue(snoozed.isSnoozed)
        assertEquals(5 * 60 * 1000L, snoozed.remainingMillis)
        assertEquals(elapsed + 5 * 60 * 1000L, snoozed.targetEndTimeElapsedRealtime)
    }

    @Test
    fun `mid-phase settings change does not corrupt running phase and applies to next phase`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val elapsed = 10_000L
        val wall = 1_000_000L
        val running = stateMachine.start(initial, elapsed, wall, 1, defaultSettings)

        // User changes settings mid-phase: focus = 30m, shortBreak = 10m
        val newSettings = defaultSettings.copy(
            focusDurationMinutes = 30,
            shortBreakDurationMinutes = 10
        )
        val updated = stateMachine.updateSettingsMidPhase(running, newSettings)

        // Running phase remains untouched at 25 min duration
        assertEquals(TimerStatus.RUNNING, updated.status)
        assertEquals(25 * 60 * 1000L, updated.totalDurationMillis)
        assertEquals(25 * 60 * 1000L, updated.remainingMillis)
        assertNotNull(updated.pendingNextPhaseSettings)

        // When phase finishes and next phase starts, pending settings apply
        val ended = stateMachine.phaseCompleted(updated)
        val next = stateMachine.startNextPhase(ended, elapsed, wall, 1, defaultSettings)
        assertEquals(Phase.SHORT_BREAK, next.phase)
        assertEquals(10 * 60 * 1000L, next.remainingMillis)
        assertNull(next.pendingNextPhaseSettings)
    }

    @Test
    fun `restore handles same-boot process death and calculates exact remaining time`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val startElapsed = 10_000L
        val startWall = 1_000_000L
        val running = stateMachine.start(initial, startElapsed, startWall, bootCount = 1, defaultSettings)

        // Process killed 5 minutes into the 25-minute focus session
        val restoreElapsed = startElapsed + 5 * 60 * 1000L
        val restoreWall = startWall + 5 * 60 * 1000L

        val restored = stateMachine.restore(running, restoreElapsed, restoreWall, currentBootCount = 1, defaultSettings)
        assertEquals(TimerStatus.RUNNING, restored.status)
        assertEquals(20 * 60 * 1000L, restored.remainingMillis)
        assertEquals(restoreElapsed + 20 * 60 * 1000L, restored.targetEndTimeElapsedRealtime)
    }

    @Test
    fun `restore across reboot uses wall clock to compute accurate remaining time`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val startElapsed = 10_000L
        val startWall = 1_000_000L
        val running = stateMachine.start(initial, startElapsed, startWall, bootCount = 1, defaultSettings)

        // Device reboots! elapsedRealtime resets to 2,000L, bootCount becomes 2
        val restoreElapsed = 2_000L
        // 10 minutes passed on wall clock
        val restoreWall = startWall + 10 * 60 * 1000L

        val restored = stateMachine.restore(running, restoreElapsed, restoreWall, currentBootCount = 2, defaultSettings)
        assertEquals(TimerStatus.RUNNING, restored.status)
        assertEquals(15 * 60 * 1000L, restored.remainingMillis)
        assertEquals(2, restored.bootCount)
    }

    @Test
    fun `restore when phase expired during death triggers phase ended catch-up`() {
        val initial = stateMachine.createInitialSnapshot(defaultSettings, bootCount = 1)
        val startElapsed = 10_000L
        val startWall = 1_000_000L
        val running = stateMachine.start(initial, startElapsed, startWall, bootCount = 1, defaultSettings)

        // App was killed and reopened 30 minutes later (past 25 min duration)
        val restoreElapsed = startElapsed + 30 * 60 * 1000L
        val restoreWall = startWall + 30 * 60 * 1000L

        val restored = stateMachine.restore(running, restoreElapsed, restoreWall, currentBootCount = 1, defaultSettings)
        assertEquals(TimerStatus.PHASE_ENDED, restored.status)
        assertEquals(0L, restored.remainingMillis)
    }
}

