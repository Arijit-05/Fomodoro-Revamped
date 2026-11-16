package com.arijit.pomodoro.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.arijit.pomodoro.R
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSnapshot

class LiveUpdatesHelper(private val context: Context) {

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    // Tracks if user dismissed Live Update in the current session
    var isDismissedForCurrentSession: Boolean = false

    fun canPostLiveUpdate(): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        if (isDismissedForCurrentSession) return false

        return runCatching {
            notificationManager.canPostPromotedNotifications()
        }.getOrDefault(false)
    }

    @RequiresApi(36)
    fun applyLiveUpdateStyle(
        builder: Notification.Builder,
        snapshot: TimerSnapshot
    ) {
        if (!canPostLiveUpdate()) return

        runCatching {
            // Meet promotion criteria: promoted ongoing flag
            builder.setFlag(Notification.FLAG_PROMOTED_ONGOING, true)

            // Status bar chip: Compact countdown via chronometer and short critical text (<= 7 chars)
            val chipText = when {
                snapshot.isPaused -> "Paused"
                snapshot.phase == Phase.SHORT_BREAK -> "Break"
                snapshot.phase == Phase.LONG_BREAK -> "L-Break"
                else -> "Focus"
            }
            builder.setShortCriticalText(chipText)

            // ProgressStyle: Segmented progress for Focus / Break phases of cycle
            val progressStyle = Notification.ProgressStyle()

            // Segments based on total sessions in cycle
            val totalSessions = snapshot.cycleState.totalSessions
            repeat(totalSessions) { index ->
                // Focus segment (e.g. 100 units)
                val focusSegment = Notification.ProgressStyle.Segment(100)
                    .setColor(0xFFE53935.toInt()) // Red theme
                progressStyle.addProgressSegment(focusSegment)

                // Break segment (e.g. 30 units for short, 60 for long)
                val breakLength = if (index == totalSessions - 1) 60 else 30
                val breakColor = if (index == totalSessions - 1) 0xFF1E88E5.toInt() else 0xFF43A047.toInt()
                val breakSegment = Notification.ProgressStyle.Segment(breakLength)
                    .setColor(breakColor)
                progressStyle.addProgressSegment(breakSegment)

                // Point marker at session boundaries
                val boundaryPoint = Notification.ProgressStyle.Point(100)
                    .setColor(0xFFFFFFFF.toInt())
                progressStyle.addProgressPoint(boundaryPoint)
            }

            // Current progress in current phase (0..100)
            val progressPercent = (snapshot.progress * 100f).toInt().coerceIn(0, 100)
            progressStyle.setProgress(progressPercent)

            builder.style = progressStyle
        }
    }
}

