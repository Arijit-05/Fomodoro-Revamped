package com.arijit.pomodoro.core.timer

import com.arijit.pomodoro.core.model.TimerSettings
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TimerController(
    private val clock: Clock,
    private val stateMachine: TimerStateMachine = TimerStateMachine(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    initialSettings: TimerSettings = TimerSettings(),
    bootCount: Int = 0
) {
    private val mutex = Mutex()

    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<TimerSettings> = _settings.asStateFlow()

    private val _state = MutableStateFlow(stateMachine.createInitialSnapshot(initialSettings, bootCount))
    val state: StateFlow<TimerSnapshot> = _state.asStateFlow()

    private var currentBootCount: Int = bootCount
    private var tickerJob: Job? = null

    // Listeners for side effects (service, notifications, alarms)
    private val stateChangeListeners = mutableListOf<(TimerSnapshot) -> Unit>()
    private val phaseCompletedListeners = mutableListOf<(TimerSnapshot) -> Unit>()

    fun addStateChangeListener(listener: (TimerSnapshot) -> Unit) {
        stateChangeListeners.add(listener)
    }

    fun removeStateChangeListener(listener: (TimerSnapshot) -> Unit) {
        stateChangeListeners.remove(listener)
    }

    fun addPhaseCompletedListener(listener: (TimerSnapshot) -> Unit) {
        phaseCompletedListeners.add(listener)
    }

    fun removePhaseCompletedListener(listener: (TimerSnapshot) -> Unit) {
        phaseCompletedListeners.remove(listener)
    }

    suspend fun start() = mutex.withLock {
        val newState = stateMachine.start(
            current = _state.value,
            elapsedRealtime = clock.elapsedRealtime(),
            wallClockTime = clock.currentTimeMillis(),
            bootCount = currentBootCount,
            settings = _settings.value
        )
        _state.value = newState
        startTicker()
        notifyStateChange(newState)
    }

    suspend fun pause() = mutex.withLock {
        val newState = stateMachine.pause(_state.value, clock.elapsedRealtime())
        _state.value = newState
        stopTicker()
        notifyStateChange(newState)
    }

    suspend fun resume() = mutex.withLock {
        val newState = stateMachine.resume(
            current = _state.value,
            elapsedRealtime = clock.elapsedRealtime(),
            wallClockTime = clock.currentTimeMillis(),
            bootCount = currentBootCount
        )
        _state.value = newState
        startTicker()
        notifyStateChange(newState)
    }

    suspend fun skip() = mutex.withLock {
        val wasRunning = _state.value.isRunning
        val newState = stateMachine.skip(
            current = _state.value,
            elapsedRealtime = clock.elapsedRealtime(),
            wallClockTime = clock.currentTimeMillis(),
            bootCount = currentBootCount,
            activeSettings = _settings.value
        )
        _state.value = newState
        if (wasRunning) {
            startTicker()
        } else {
            stopTicker()
        }
        notifyStateChange(newState)
    }

    suspend fun reset() = mutex.withLock {
        stopTicker()
        val newState = stateMachine.reset(_settings.value, currentBootCount)
        _state.value = newState
        notifyStateChange(newState)
    }

    suspend fun snooze(snoozeMinutes: Int? = null) = mutex.withLock {
        val durationMillis = (snoozeMinutes ?: _settings.value.snoozeDurationMinutes) * 60 * 1000L
        val newState = stateMachine.snooze(
            current = _state.value,
            snoozeDurationMillis = durationMillis,
            elapsedRealtime = clock.elapsedRealtime(),
            wallClockTime = clock.currentTimeMillis(),
            bootCount = currentBootCount
        )
        _state.value = newState
        startTicker()
        notifyStateChange(newState)
    }

    suspend fun startNextPhase() = mutex.withLock {
        val newState = stateMachine.startNextPhase(
            current = _state.value,
            elapsedRealtime = clock.elapsedRealtime(),
            wallClockTime = clock.currentTimeMillis(),
            bootCount = currentBootCount,
            activeSettings = _settings.value
        )
        _state.value = newState
        startTicker()
        notifyStateChange(newState)
    }

    suspend fun updateSettings(newSettings: TimerSettings) = mutex.withLock {
        _settings.value = newSettings
        val newState = stateMachine.updateSettingsMidPhase(_state.value, newSettings)
        _state.value = newState
        notifyStateChange(newState)
    }

    suspend fun restore(saved: TimerSnapshot, bootCount: Int) = mutex.withLock {
        currentBootCount = bootCount
        val restored = stateMachine.restore(
            saved = saved,
            currentElapsed = clock.elapsedRealtime(),
            currentWallClock = clock.currentTimeMillis(),
            currentBootCount = bootCount,
            activeSettings = _settings.value
        )
        _state.value = restored
        if (restored.isRunning) {
            startTicker()
        } else {
            stopTicker()
        }
        notifyStateChange(restored)

        if (restored.isPhaseEnded && saved.isRunning) {
            notifyPhaseCompleted(restored)
        }
    }

    suspend fun onExactAlarmFired() = mutex.withLock {
        if (_state.value.isRunning) {
            val completed = stateMachine.phaseCompleted(_state.value)
            _state.value = completed
            stopTicker()
            notifyStateChange(completed)
            notifyPhaseCompleted(completed)

            // Auto-start check
            val shouldAutoStart = if (completed.phase.isFocus) {
                _settings.value.autoStartBreaks
            } else {
                _settings.value.autoStartFocus
            }

            if (shouldAutoStart) {
                val nextState = stateMachine.startNextPhase(
                    current = completed,
                    elapsedRealtime = clock.elapsedRealtime(),
                    wallClockTime = clock.currentTimeMillis(),
                    bootCount = currentBootCount,
                    activeSettings = _settings.value
                )
                _state.value = nextState
                startTicker()
                notifyStateChange(nextState)
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _state.value.isRunning) {
                delay(500)
                mutex.withLock {
                    if (_state.value.isRunning) {
                        val ticked = stateMachine.tick(_state.value, clock.elapsedRealtime())
                        _state.value = ticked
                        if (ticked.isPhaseEnded) {
                            stopTicker()
                            notifyStateChange(ticked)
                            notifyPhaseCompleted(ticked)
                        }
                    }
                }
            }
        }
    }

    fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun stop() {
        stopTicker()
    }

    private fun notifyStateChange(snapshot: TimerSnapshot) {
        stateChangeListeners.forEach { it.invoke(snapshot) }
    }

    private fun notifyPhaseCompleted(snapshot: TimerSnapshot) {
        phaseCompletedListeners.forEach { it.invoke(snapshot) }
    }
}

