package com.arijit.pomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arijit.pomodoro.MainActivity
import com.arijit.pomodoro.R
import com.arijit.pomodoro.core.model.CycleState
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus

class NotificationHelper(
    private val context: Context,
    val liveUpdatesHelper: LiveUpdatesHelper = LiveUpdatesHelper(context)
) {

    companion object {
        const val CHANNEL_ONGOING = "timer_ongoing"
        const val CHANNEL_ALERTS = "timer_alerts"

        const val NOTIFICATION_ID_ONGOING = 1001
        const val NOTIFICATION_ID_ALERT = 1002

        const val ACTION_PAUSE = "com.arijit.pomodoro.ACTION_PAUSE"
        const val ACTION_RESUME = "com.arijit.pomodoro.ACTION_RESUME"
        const val ACTION_SKIP = "com.arijit.pomodoro.ACTION_SKIP"
        const val ACTION_SNOOZE = "com.arijit.pomodoro.ACTION_SNOOZE"
        const val ACTION_EXIT_ULTRA_FOCUS = "com.arijit.pomodoro.ACTION_EXIT_ULTRA_FOCUS"
        const val ACTION_START_NEXT = "com.arijit.pomodoro.ACTION_START_NEXT"
        const val ACTION_LIVE_UPDATE_DISMISSED = "com.arijit.pomodoro.ACTION_LIVE_UPDATE_DISMISSED"

        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Ongoing Timer (Silent, LOW importance)
            val ongoingChannel = NotificationChannel(
                CHANNEL_ONGOING,
                "Ongoing Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live ongoing timer status"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }

            // Channel 2: Phase Complete Alerts (High importance, Heads-up)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Phase Complete Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a focus session or break completes"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(ongoingChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildOngoingNotification(
        snapshot: TimerSnapshot,
        breakPrompt: String? = null,
        isUltraFocusActive: Boolean = false,
        liveUpdatesEnabled: Boolean = true
    ): Notification {
        val title = when (snapshot.phase) {
            Phase.FOCUS -> "Focus"
            Phase.SHORT_BREAK -> "Short Break"
            Phase.LONG_BREAK -> "Long Break"
        }

        val contentText = if (snapshot.phase.isBreak && !breakPrompt.isNullOrBlank()) {
            breakPrompt
        } else {
            "Session ${snapshot.cycleState.currentSession} of ${snapshot.cycleState.totalSessions}"
        }

        // Content intent: open app
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss intent (for detecting Live Update dismissal on Android 16)
        val deleteIntent = PendingIntent.getBroadcast(
            context,
            99,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_LIVE_UPDATE_DISMISSED
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action PendingIntents
        val pauseIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resumeIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val skipIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_SKIP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            4,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_SNOOZE_MINUTES, 5)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exitUltraFocusIntent = PendingIntent.getBroadcast(
            context,
            5,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_EXIT_ULTRA_FOCUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Android 16 Live Updates path
        if (Build.VERSION.SDK_INT >= 36 && liveUpdatesEnabled && liveUpdatesHelper.canPostLiveUpdate()) {
            val nativeBuilder = Notification.Builder(context, CHANNEL_ONGOING)
                .setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notification_timer)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deleteIntent)
                .setOngoing(true)

            if (snapshot.isRunning && snapshot.targetEndTimeWallClock > 0) {
                nativeBuilder.setWhen(snapshot.targetEndTimeWallClock)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setShowWhen(true)
            } else {
                val remainingSec = (snapshot.remainingMillis / 1000).coerceAtLeast(0)
                val timeFormatted = String.format("%02d:%02d (Paused)", remainingSec / 60, remainingSec % 60)
                nativeBuilder.setContentText("$contentText • $timeFormatted")
                    .setUsesChronometer(false)
                    .setShowWhen(false)
            }

            // Actions
            if (snapshot.isRunning) {
                nativeBuilder.addAction(
                    Notification.Action.Builder(null, "Pause", pauseIntent).build()
                )
            } else {
                nativeBuilder.addAction(
                    Notification.Action.Builder(null, "Resume", resumeIntent).build()
                )
            }
            nativeBuilder.addAction(
                Notification.Action.Builder(null, "Skip", skipIntent).build()
            )
            nativeBuilder.addAction(
                Notification.Action.Builder(null, "+5m", snoozeIntent).build()
            )
            if (isUltraFocusActive) {
                nativeBuilder.addAction(
                    Notification.Action.Builder(null, "Exit Ultra Focus", exitUltraFocusIntent).build()
                )
            }

            liveUpdatesHelper.applyLiveUpdateStyle(nativeBuilder, snapshot)
            return nativeBuilder.build()
        }

        // Standard notification (API 26+)
        val compatBuilder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)

        if (snapshot.isRunning && snapshot.targetEndTimeWallClock > 0) {
            compatBuilder
                .setWhen(snapshot.targetEndTimeWallClock)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        } else {
            val remainingSec = (snapshot.remainingMillis / 1000).coerceAtLeast(0)
            val timeFormatted = String.format("%02d:%02d (Paused)", remainingSec / 60, remainingSec % 60)
            compatBuilder
                .setContentText("$contentText • $timeFormatted")
                .setUsesChronometer(false)
                .setShowWhen(false)
        }

        // Actions
        if (snapshot.isRunning) {
            compatBuilder.addAction(0, "Pause", pauseIntent)
        } else {
            compatBuilder.addAction(0, "Resume", resumeIntent)
        }
        compatBuilder.addAction(0, "Skip", skipIntent)
        compatBuilder.addAction(0, "+5m", snoozeIntent)

        if (isUltraFocusActive) {
            compatBuilder.addAction(0, "Exit Ultra Focus", exitUltraFocusIntent)
        }

        return compatBuilder.build()
    }

    fun postPhaseCompleteNotification(
        phase: Phase,
        cycleState: CycleState,
        breakPrompt: String? = null
    ) {
        val title = if (phase == Phase.FOCUS) "Focus Session Complete!" else "Break Complete!"
        val content = if (phase == Phase.FOCUS) {
            if (cycleState.isLastSessionInCycle) "Time for a well-deserved long break!" else "Great job! Time for a short break."
        } else {
            "Ready to focus again? Session ${cycleState.currentSession} of ${cycleState.totalSessions} is next."
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startNextIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, NotificationActionReceiver::class.java).apply { action = ACTION_START_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            12,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_SNOOZE_MINUTES, 5)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, "Start Next", startNextIntent)
            .addAction(0, "+5m Snooze", snoozeIntent)
            .build()

        runCatching {
            notificationManager.notify(NOTIFICATION_ID_ALERT, notification)
        }
    }

    fun cancelAlertNotification() {
        runCatching {
            notificationManager.cancel(NOTIFICATION_ID_ALERT)
        }
    }
}

