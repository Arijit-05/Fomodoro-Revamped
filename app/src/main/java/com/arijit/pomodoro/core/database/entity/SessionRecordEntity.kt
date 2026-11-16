package com.arijit.pomodoro.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_records")
data class SessionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimeWallClock: Long,
    val endTimeWallClock: Long,
    val durationMinutes: Int,
    val phase: String, // FOCUS, SHORT_BREAK, LONG_BREAK
    val sessionNumber: Int,
    val totalSessions: Int,
    val completed: Boolean = true
)

