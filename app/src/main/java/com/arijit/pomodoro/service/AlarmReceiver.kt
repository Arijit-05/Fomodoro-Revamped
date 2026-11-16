package com.arijit.pomodoro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.timer.TimerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerController: TimerController

    @Inject
    lateinit var phaseAlertManager: PhaseAlertManager

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    companion object {
        const val ACTION_EXACT_ALARM_FIRED = "com.arijit.pomodoro.ACTION_EXACT_ALARM_FIRED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_EXACT_ALARM_FIRED) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Fomodoro:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(5000L /* 5 seconds */)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val previousSnapshot = timerController.state.value
                val preferences = settingsDataStore.userPreferences.first()
                val randomPrompt = preferences.breakPrompts.randomOrNull()

                // Trigger timer controller state transition
                timerController.onExactAlarmFired()

                // Trigger all crash-proof side effects (sound, vibration, torch, notification)
                val durationMinutes = (previousSnapshot.totalDurationMillis / (60 * 1000L)).toInt().coerceAtLeast(1)
                phaseAlertManager.onPhaseCompleted(
                    phase = previousSnapshot.phase,
                    cycleState = previousSnapshot.cycleState,
                    durationMinutes = durationMinutes,
                    preferences = preferences,
                    breakPrompt = randomPrompt
                )
            } finally {
                runCatching { wakeLock?.release() }
                pendingResult.finish()
            }
        }
    }
}

