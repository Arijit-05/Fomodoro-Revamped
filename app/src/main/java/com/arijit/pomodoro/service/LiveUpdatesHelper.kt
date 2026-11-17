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

            // Status bar chip: Show the running timer (e.g. "24:54") instead of static "Focus"
            val remainingSec = (snapshot.remainingMillis / 1000).coerceAtLeast(0)
            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            val timerChipText = String.format("%02d:%02d", minutes, seconds)
            builder.setShortCriticalText(timerChipText)
        }.onFailure { e ->
            android.util.Log.e("LiveUpdatesHelper", "Failed to apply live update: ${e.message}", e)
        }
    }

    fun setRequestPromotedOngoingSafe(builder: Notification.Builder, promoted: Boolean) {
        runCatching {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, promoted)
        }
    }
}
