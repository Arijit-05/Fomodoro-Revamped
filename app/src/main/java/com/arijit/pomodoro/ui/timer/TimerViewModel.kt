package com.arijit.pomodoro.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.datastore.UserPreferences
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.timer.TimerController
import com.arijit.pomodoro.service.TimerForegroundService
import com.arijit.pomodoro.system.HapticManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val snapshot: TimerSnapshot = TimerSnapshot(),
    val preferences: UserPreferences = UserPreferences(),
    val currentBreakPrompt: String? = null
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val timerController: TimerController,
    private val settingsDataStore: SettingsDataStore,
    val hapticManager: HapticManager
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = combine(
        timerController.state,
        settingsDataStore.userPreferences
    ) { snapshot, preferences ->
        val prompt = if (snapshot.phase.isBreak) {
            preferences.breakPrompts.randomOrNull()
        } else null
        TimerUiState(
            snapshot = snapshot,
            preferences = preferences,
            currentBreakPrompt = prompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerUiState()
    )

    fun onStart() {
        hapticManager.start()
        viewModelScope.launch {
            timerController.start()
            TimerForegroundService.startService(context)
        }
    }

    fun onPause() {
        hapticManager.pause()
        viewModelScope.launch {
            timerController.pause()
        }
    }

    fun onResume() {
        hapticManager.resume()
        viewModelScope.launch {
            timerController.resume()
            TimerForegroundService.startService(context)
        }
    }

    fun onSkip() {
        hapticManager.skip()
        viewModelScope.launch {
            timerController.skip()
        }
    }

    fun onReset() {
        hapticManager.reset()
        viewModelScope.launch {
            timerController.reset()
        }
    }

    fun onSnooze(minutes: Int = 5) {
        hapticManager.snooze()
        viewModelScope.launch {
            timerController.snooze(minutes)
            TimerForegroundService.startService(context)
        }
    }

    fun onStartNextPhase() {
        hapticManager.start()
        viewModelScope.launch {
            timerController.startNextPhase()
            TimerForegroundService.startService(context)
        }
    }

    fun onExitUltraFocus() {
        viewModelScope.launch {
            settingsDataStore.setUltraFocusMode(false)
        }
    }
}

