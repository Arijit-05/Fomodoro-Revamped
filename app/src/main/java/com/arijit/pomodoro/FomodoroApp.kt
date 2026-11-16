package com.arijit.pomodoro

import android.app.Application
import android.provider.Settings
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.timer.AlarmScheduler
import com.arijit.pomodoro.core.timer.TimerController
import com.arijit.pomodoro.service.NotificationHelper
import com.arijit.pomodoro.service.TimerForegroundService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FomodoroApp : Application() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var timerController: TimerController

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createNotificationChannels()

        // Continuously ensure TimerForegroundService is active when timer is running
        appScope.launch {
            timerController.state.collect { snapshot ->
                if (snapshot.isRunning) {
                    runCatching {
                        TimerForegroundService.startService(this@FomodoroApp)
                    }
                }
            }
        }

        // Restore timer state on app startup if process was killed
        appScope.launch {
            val saved = settingsDataStore.savedSnapshot.firstOrNull() ?: return@launch
            val bootCount = runCatching {
                Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, 0)
            }.getOrDefault(0)

            timerController.restore(saved, bootCount)

            val restored = timerController.state.value
            if (restored.isRunning) {
                val alarmScheduler = AlarmScheduler(this@FomodoroApp)
                alarmScheduler.scheduleExactAlarm(
                    targetElapsedRealtime = restored.targetEndTimeElapsedRealtime,
                    targetWallClock = restored.targetEndTimeWallClock
                )
                TimerForegroundService.startService(this@FomodoroApp)
            }
        }
    }
}

