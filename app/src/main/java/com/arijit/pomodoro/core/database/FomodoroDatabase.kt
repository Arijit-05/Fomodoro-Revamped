package com.arijit.pomodoro.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.arijit.pomodoro.core.database.dao.SessionDao
import com.arijit.pomodoro.core.database.entity.SessionRecordEntity

@Database(
    entities = [SessionRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FomodoroDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: FomodoroDatabase? = null

        fun getInstance(context: Context): FomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FomodoroDatabase::class.java,
                    "fomodoro_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

