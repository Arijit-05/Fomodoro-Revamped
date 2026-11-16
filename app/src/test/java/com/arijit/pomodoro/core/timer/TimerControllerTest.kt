package com.arijit.pomodoro.core.timer

import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSettings
import com.arijit.pomodoro.core.model.TimerStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeClock(
    var elapsed: Long = 10_000L,
    var wall: Long = 1_000_000L
) : Clock {
    override fun elapsedRealtime(): Long = elapsed
    override fun currentTimeMillis(): Long = wall

    fun advance(millis: Long) {
        elapsed += millis
        wall += millis
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerControllerTest {

    private lateinit var clock: FakeClock
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val settings = TimerSettings(
        focusDurationMinutes = 25,
        shortBreakDurationMinutes = 5,
        longBreakDurationMinutes = 15,
        sessionsPerCycle = 4,
        autoStartBreaks = true,
        autoStartFocus = false,
        snoozeDurationMinutes = 5
    )

    private lateinit var controller: TimerController

    @Before
    fun setUp() {
        clock = FakeClock()
        controller = TimerController(
            clock = clock,
            stateMachine = TimerStateMachine(),
            scope = testScope,
            initialSettings = settings,
            bootCount = 1
        )
    }

    private fun runTimerTest(block: suspend () -> Unit) = testScope.runTest {
        try {
            block()
        } finally {
            controller.stop()
        }
    }

    @Test
    fun `start transitions controller state to running`() = runTimerTest {
        assertEquals(TimerStatus.IDLE, controller.state.value.status)
        controller.start()
        assertEquals(TimerStatus.RUNNING, controller.state.value.status)
        assertEquals(25 * 60 * 1000L, controller.state.value.remainingMillis)
    }

    @Test
    fun `ticker decrements remaining time over time`() = runTimerTest {
        controller.start()

        // Advance 2 seconds
        clock.advance(2000L)
        testDispatcher.scheduler.advanceTimeBy(2000L)

        val remaining = controller.state.value.remainingMillis
        assertTrue(remaining <= 25 * 60 * 1000L - 2000L)
    }

    @Test
    fun `exact alarm triggers phase completion and auto-starts break when enabled`() = runTimerTest {
        var completedFired = false
        controller.addPhaseCompletedListener {
            completedFired = true
        }

        controller.start()
        clock.advance(25 * 60 * 1000L)

        // Exact alarm triggers
        controller.onExactAlarmFired()

        assertTrue("Phase completion listener must be called", completedFired)
        // Since autoStartBreaks = true, controller should have transitioned to Short Break Running
        assertEquals(Phase.SHORT_BREAK, controller.state.value.phase)
        assertEquals(TimerStatus.RUNNING, controller.state.value.status)
        assertEquals(5 * 60 * 1000L, controller.state.value.remainingMillis)
    }

    @Test
    fun `snooze extends running time and sets snoozed flag`() = runTimerTest {
        controller.start()
        clock.advance(25 * 60 * 1000L)
        controller.onExactAlarmFired() // completed

        controller.snooze(snoozeMinutes = 3)
        assertEquals(TimerStatus.RUNNING, controller.state.value.status)
        assertTrue(controller.state.value.isSnoozed)
        assertEquals(3 * 60 * 1000L, controller.state.value.remainingMillis)
    }

    @Test
    fun `reset returns controller to initial idle state`() = runTimerTest {
        controller.start()
        clock.advance(5000L)
        controller.pause()

        controller.reset()
        assertEquals(TimerStatus.IDLE, controller.state.value.status)
        assertEquals(1, controller.state.value.cycleState.currentSession)
        assertEquals(25 * 60 * 1000L, controller.state.value.remainingMillis)
    }
}
