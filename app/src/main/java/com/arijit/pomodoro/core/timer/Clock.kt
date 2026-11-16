package com.arijit.pomodoro.core.timer

interface Clock {
    fun elapsedRealtime(): Long
    fun currentTimeMillis(): Long
}

class SystemClockImpl : Clock {
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

