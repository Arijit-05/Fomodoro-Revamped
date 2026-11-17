package com.arijit.pomodoro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus
import com.arijit.pomodoro.core.timer.AlarmScheduler
import com.arijit.pomodoro.core.timer.TimerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject
    lateinit var timerController: TimerController

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var audioEngine: com.arijit.pomodoro.core.audio.AudioEngine

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var alarmScheduler: AlarmScheduler
    private var lastTargetEndTime: Long = -1L
    private var lastSavedStatus: TimerStatus? = null
    private var lastSavedPhase: Phase? = null
    private var lastSavedCycleSession: Int = -1
    private var lastSaveTimeMs: Long = 0L


    companion object {
        fun startService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        alarmScheduler = AlarmScheduler(this)

        // Initialize foreground with initial notification
        val initialSnapshot = timerController.state.value
        startInForeground(initialSnapshot)

        // Observe state changes and user preferences
        var hasSeenActiveState = initialSnapshot.isRunning || initialSnapshot.isPaused || initialSnapshot.isPhaseEnded
        combine(
            timerController.state,
            settingsDataStore.userPreferences
        ) { snapshot, preferences ->
            if (snapshot.isRunning || snapshot.isPaused || snapshot.isPhaseEnded) {
                hasSeenActiveState = true
                handleStateAndPreferences(snapshot, preferences)
            } else if (snapshot.status == TimerStatus.IDLE && hasSeenActiveState) {
                handleStateAndPreferences(snapshot, preferences)
            }
        }.launchIn(serviceScope)
    }

    private fun startInForeground(snapshot: TimerSnapshot) {
        val notification = notificationHelper.buildOngoingNotification(snapshot)
        val notificationId = NotificationHelper.NOTIFICATION_ID_ONGOING

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(notificationId, notification, serviceType)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun handleStateAndPreferences(
        snapshot: TimerSnapshot,
        preferences: com.arijit.pomodoro.core.datastore.UserPreferences
    ) {
        // Sync Media3 ExoPlayer ambient and ticking audio
        audioEngine.updatePlaybackState(snapshot, preferences)

        // Save snapshot to DataStore on status/phase/session change or every 30s
        val now = System.currentTimeMillis()
        val shouldSave = snapshot.status != lastSavedStatus ||
            snapshot.phase != lastSavedPhase ||
            snapshot.cycleState.currentSession != lastSavedCycleSession ||
            (now - lastSaveTimeMs > 30_000L)

        if (shouldSave) {
            lastSavedStatus = snapshot.status
            lastSavedPhase = snapshot.phase
            lastSavedCycleSession = snapshot.cycleState.currentSession
            lastSaveTimeMs = now
            serviceScope.launch(Dispatchers.IO) {
                settingsDataStore.saveSnapshot(snapshot)
            }
        }

        val randomPrompt = preferences.breakPrompts.randomOrNull()
        val isUltraFocus = preferences.ultraFocusMode
        val liveUpdatesEnabled = preferences.liveUpdatesEnabled

        when (snapshot.status) {
            TimerStatus.RUNNING -> {
                // Only re-arm exact alarm when target time changes
                if (snapshot.targetEndTimeElapsedRealtime != lastTargetEndTime) {
                    lastTargetEndTime = snapshot.targetEndTimeElapsedRealtime
                    alarmScheduler.scheduleExactAlarm(
                        targetElapsedRealtime = snapshot.targetEndTimeElapsedRealtime,
                        targetWallClock = snapshot.targetEndTimeWallClock
                    )
                }
                updateNotification(snapshot, randomPrompt, isUltraFocus, liveUpdatesEnabled)
            }
            TimerStatus.PAUSED -> {
                lastTargetEndTime = -1L
                alarmScheduler.cancelExactAlarm()
                updateNotification(snapshot, randomPrompt, isUltraFocus, liveUpdatesEnabled)
            }
            TimerStatus.PHASE_ENDED -> {
                lastTargetEndTime = -1L
                alarmScheduler.cancelExactAlarm()
                updateNotification(snapshot, randomPrompt, isUltraFocus, liveUpdatesEnabled)
            }
            TimerStatus.IDLE -> {
                lastTargetEndTime = -1L
                alarmScheduler.cancelExactAlarm()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateNotification(
        snapshot: TimerSnapshot,
        randomPrompt: String?,
        isUltraFocus: Boolean,
        liveUpdatesEnabled: Boolean
    ) {
        val notification = notificationHelper.buildOngoingNotification(
            snapshot = snapshot,
            breakPrompt = randomPrompt,
            isUltraFocusActive = isUltraFocus,
            liveUpdatesEnabled = liveUpdatesEnabled
        )
        val notificationManager = NotificationManagerCompat.from(this)
        runCatching {
            notificationManager.notify(NotificationHelper.NOTIFICATION_ID_ONGOING, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must return START_STICKY so service is recreated if system kills process under memory pressure
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Ensure service survives when app is swiped from Android Recents
        val snapshot = timerController.state.value
        if (snapshot.isRunning || snapshot.isPaused) {
            // Keep running in background!
            alarmScheduler.scheduleExactAlarm(
                targetElapsedRealtime = snapshot.targetEndTimeElapsedRealtime,
                targetWallClock = snapshot.targetEndTimeWallClock
            )
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioEngine.release()
        serviceScope.cancel()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}

