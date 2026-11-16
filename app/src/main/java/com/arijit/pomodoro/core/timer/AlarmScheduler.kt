package com.arijit.pomodoro.core.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.arijit.pomodoro.MainActivity
import com.arijit.pomodoro.service.AlarmReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }

    companion object {
        const val ALARM_REQUEST_CODE = 2001
    }

    private fun getAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_EXACT_ALARM_FIRED
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleExactAlarm(targetElapsedRealtime: Long, targetWallClock: Long) {
        val am = alarmManager ?: return
        val pendingIntent = getAlarmPendingIntent()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        targetElapsedRealtime,
                        pendingIntent
                    )
                } else {
                    // Fallback to alarm clock or inexact if exact permission not granted
                    val showIntent = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    am.setAlarmClock(
                        AlarmManager.AlarmClockInfo(targetWallClock, showIntent),
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    targetElapsedRealtime,
                    pendingIntent
                )
            } else {
                am.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    targetElapsedRealtime,
                    pendingIntent
                )
            }
        }.onFailure {
            // Graceful fallback to inexact while idle
            runCatching {
                am.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    targetElapsedRealtime,
                    pendingIntent
                )
            }
        }
    }

    fun cancelExactAlarm() {
        val am = alarmManager ?: return
        runCatching {
            am.cancel(getAlarmPendingIntent())
        }
    }
}

