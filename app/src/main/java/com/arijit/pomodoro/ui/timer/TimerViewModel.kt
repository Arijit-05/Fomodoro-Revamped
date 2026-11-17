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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
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

    private val currentBreakPromptFlow = MutableStateFlow<String?>(null)
    private var breakPromptJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                timerController.state.map { it.phase.isBreak }.distinctUntilChanged(),
                settingsDataStore.userPreferences.map { it.breakPrompts }.distinctUntilChanged()
            ) { isBreak, prompts ->
                Pair(isBreak, prompts)
            }.collect { (isBreak, prompts) ->
                breakPromptJob?.cancel()
                if (isBreak && prompts.isNotEmpty()) {
                    breakPromptJob = viewModelScope.launch {
                        var index = 0
                        while (isActive) {
                            currentBreakPromptFlow.value = prompts[index % prompts.size]
                            index++
                            delay(3000L) // Show each relaxing text for 3 seconds
                        }
                    }
                } else {
                    currentBreakPromptFlow.value = null
                }
            }
        }
    }

    val uiState: StateFlow<TimerUiState> = combine(
        timerController.state,
        settingsDataStore.userPreferences,
        currentBreakPromptFlow
    ) { snapshot, preferences, prompt ->
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

    fun setAmbientStyle(style: String) {
        viewModelScope.launch {
            settingsDataStore.setAmbientStyle(style)
        }
    }

    fun setAmbientVolume(volume: Float) {
        viewModelScope.launch {
            settingsDataStore.setAmbientVolume(volume)
        }
    }


    fun toggleAmbient() {
        val currentStyle = uiState.value.preferences.ambientSoundStyle
        val nextStyle = if (currentStyle == "none") "rain" else "none"
        viewModelScope.launch {
            settingsDataStore.setAmbientStyle(nextStyle)
        }
    }
}


