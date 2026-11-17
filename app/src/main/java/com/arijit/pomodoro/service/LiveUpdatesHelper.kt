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
        return true
    }

    @RequiresApi(36)
    fun applyLiveUpdateStyle(
        builder: Notification.Builder,
        snapshot: TimerSnapshot
    ) {
        if (!canPostLiveUpdate()) return

        runCatching {
            // Meet promotion criteria: request promoted ongoing (API 36+ runtime)
            setRequestPromotedOngoingSafe(builder, true)

            // Status bar chip: Compact countdown text / phase name (<= 7 chars)
            val chipText = when {
                snapshot.isPaused -> "Paused"
                snapshot.phase == Phase.SHORT_BREAK -> "Break"
                snapshot.phase == Phase.LONG_BREAK -> "L-Brk"
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
                    .setColor(0xFFFF7C7C.toInt())
                progressStyle.addProgressSegment(focusSegment)

                // Break segment
                val isLast = (index == totalSessions - 1)
                val breakLength = if (isLast) 60 else 30
                val breakColor = if (isLast) 0xFF8BCAFF.toInt() else 0xFF8CE8A1.toInt()
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

            runCatching {
                progressStyle.setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_notification_timer))
            }

            builder.style = progressStyle
        }.onFailure { e ->
            android.util.Log.e("LiveUpdatesHelper", "Failed to apply ProgressStyle: ${e.message}", e)
        }
    }

    fun setRequestPromotedOngoingSafe(builder: Notification.Builder, promoted: Boolean) {
        runCatching {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, promoted)
        }
    }
}
