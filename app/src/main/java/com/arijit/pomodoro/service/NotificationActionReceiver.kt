package com.arijit.pomodoro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.timer.TimerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerController: TimerController

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        android.util.Log.d("NotificationActionReceiver", "onReceive action: $action")
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    NotificationHelper.ACTION_PAUSE -> {
                        timerController.pause()
                    }
                    NotificationHelper.ACTION_RESUME -> {
                        timerController.resume()
                    }
                    NotificationHelper.ACTION_SKIP -> {
                        timerController.skip()
                    }
                    NotificationHelper.ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 5)
                        notificationHelper.cancelAlertNotification()
                        timerController.snooze(minutes)
                    }
                    NotificationHelper.ACTION_START_NEXT -> {
                        notificationHelper.cancelAlertNotification()
                        timerController.startNextPhase()
                    }
                    NotificationHelper.ACTION_EXIT_ULTRA_FOCUS -> {
                        settingsDataStore.setUltraFocusMode(false)
                    }
                    NotificationHelper.ACTION_LIVE_UPDATE_DISMISSED -> {
                        notificationHelper.liveUpdatesHelper.isDismissedForCurrentSession = true
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

