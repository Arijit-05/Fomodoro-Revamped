package com.arijit.pomodoro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arijit.pomodoro.core.datastore.HapticLevel
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.datastore.ThemeMode
import com.arijit.pomodoro.core.datastore.UserPreferences
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.timer.TimerController
import com.arijit.pomodoro.system.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val timerSnapshot: TimerSnapshot = TimerSnapshot()
) {
    val isTimerRunningOrPaused: Boolean get() = timerSnapshot.isRunning || timerSnapshot.isPaused
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val timerController: TimerController,
    val hapticManager: HapticManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsDataStore.userPreferences,
        timerController.state
    ) { prefs, snapshot ->
        SettingsUiState(preferences = prefs, timerSnapshot = snapshot)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateTimerDurations(
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        sessionsPerCycle: Int,
        autoStartBreaks: Boolean,
        autoStartFocus: Boolean,
        snoozeMinutes: Int
    ) {
        viewModelScope.launch {
            settingsDataStore.updateTimerSettings(
                focusMinutes = focusMinutes,
                shortBreakMinutes = shortBreakMinutes,
                longBreakMinutes = longBreakMinutes,
                sessionsPerCycle = sessionsPerCycle,
                autoStartBreaks = autoStartBreaks,
                autoStartFocus = autoStartFocus,
                snoozeMinutes = snoozeMinutes
            )
            // Push to controller (queued mid-phase or immediate if idle)
            timerController.updateSettings(
                com.arijit.pomodoro.core.model.TimerSettings(
                    focusDurationMinutes = focusMinutes,
                    shortBreakDurationMinutes = shortBreakMinutes,
                    longBreakDurationMinutes = longBreakMinutes,
                    sessionsPerCycle = sessionsPerCycle,
                    autoStartBreaks = autoStartBreaks,
                    autoStartFocus = autoStartFocus,
                    snoozeDurationMinutes = snoozeMinutes
                )
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setAmoledBlack(enabled) }
    }

    fun setFocusThemeColor(colorId: String) {
        viewModelScope.launch { settingsDataStore.setFocusThemeColor(colorId) }
    }

    fun setShortBreakThemeColor(colorId: String) {
        viewModelScope.launch { settingsDataStore.setShortBreakThemeColor(colorId) }
    }

    fun setLongBreakThemeColor(colorId: String) {
        viewModelScope.launch { settingsDataStore.setLongBreakThemeColor(colorId) }
    }


    fun setKeepScreenOn(enabled: Boolean) {
        hapticManager.toggle(enabled)
        viewModelScope.launch { settingsDataStore.setKeepScreenOn(enabled) }
    }

    fun setUltraFocusMode(enabled: Boolean) {
        hapticManager.toggle(enabled)
        viewModelScope.launch { settingsDataStore.setUltraFocusMode(enabled) }
    }

    fun setAmbientAodMode(enabled: Boolean) {
        hapticManager.toggle(enabled)
        viewModelScope.launch { settingsDataStore.setAmbientAodMode(enabled) }
    }

    fun setHapticLevel(level: HapticLevel) {
        hapticManager.hapticLevel = level
        hapticManager.start()
        viewModelScope.launch { settingsDataStore.setHapticLevel(level) }
    }

    fun setFlashNotification(enabled: Boolean) {
        hapticManager.toggle(enabled)
        viewModelScope.launch { settingsDataStore.setFlashNotification(enabled) }
    }

    fun setAmbientConfig(style: String, uri: String?, volume: Float, pauseDuringBreak: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAmbientConfig(style, uri, volume, pauseDuringBreak)
        }
    }

    fun setTickingConfig(style: String, volume: Float, onlyDuringFocus: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setTickingConfig(style, volume, onlyDuringFocus)
        }
    }

    fun setSoundUris(focusSoundUri: String?, breakSoundUri: String?) {
        viewModelScope.launch {
            settingsDataStore.setSoundUris(focusSoundUri, breakSoundUri)
        }
    }

    fun setLiveUpdatesEnabled(enabled: Boolean) {
        hapticManager.toggle(enabled)
        viewModelScope.launch { settingsDataStore.setLiveUpdatesEnabled(enabled) }
    }

    fun setBreakPrompts(prompts: List<String>) {
        viewModelScope.launch { settingsDataStore.setBreakPrompts(prompts) }
    }

    fun testHaptics() {
        hapticManager.phaseEndFocus()
    }
}

