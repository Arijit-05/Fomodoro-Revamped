package com.arijit.pomodoro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.timer.AlarmScheduler
import com.arijit.pomodoro.core.timer.TimerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerController: TimerController

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val savedSnapshot = settingsDataStore.savedSnapshot.firstOrNull() ?: return@launch
                val currentBootCount = runCatching {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
                }.getOrDefault(0)

                timerController.restore(savedSnapshot, currentBootCount)

                val restored = timerController.state.value
                val alarmScheduler = AlarmScheduler(context)

                if (restored.isRunning) {
                    alarmScheduler.scheduleExactAlarm(
                        targetElapsedRealtime = restored.targetEndTimeElapsedRealtime,
                        targetWallClock = restored.targetEndTimeWallClock
                    )
                    TimerForegroundService.startService(context)
                } else {
                    alarmScheduler.cancelExactAlarm()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

