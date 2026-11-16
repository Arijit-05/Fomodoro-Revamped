package com.arijit.pomodoro.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arijit.pomodoro.core.database.entity.SessionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionRecordEntity): Long

    @Query("SELECT * FROM session_records ORDER BY startTimeWallClock DESC")
    fun getAllSessions(): Flow<List<SessionRecordEntity>>

    @Query("SELECT * FROM session_records WHERE startTimeWallClock >= :startOfDay AND startTimeWallClock < :endOfDay ORDER BY startTimeWallClock ASC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<SessionRecordEntity>>

    @Query("SELECT COALESCE(SUM(durationMinutes), 0) FROM session_records WHERE phase = 'FOCUS' AND completed = 1")
    fun getTotalFocusMinutes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_records WHERE phase = 'FOCUS' AND completed = 1")
    fun getTotalCompletedSessions(): Flow<Int>
}

