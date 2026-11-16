package com.arijit.pomodoro.core.di

import android.content.Context
import com.arijit.pomodoro.core.audio.SoundAlertManager
import com.arijit.pomodoro.core.database.FomodoroDatabase
import com.arijit.pomodoro.core.database.dao.SessionDao
import com.arijit.pomodoro.core.datastore.SettingsDataStore
import com.arijit.pomodoro.core.timer.Clock
import com.arijit.pomodoro.core.timer.SystemClockImpl
import com.arijit.pomodoro.core.timer.TimerController
import com.arijit.pomodoro.core.timer.TimerStateMachine
import com.arijit.pomodoro.service.LiveUpdatesHelper
import com.arijit.pomodoro.service.NotificationHelper
import com.arijit.pomodoro.service.PhaseAlertManager
import com.arijit.pomodoro.system.HapticManager
import com.arijit.pomodoro.system.TorchManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideClock(): Clock {
        return SystemClockImpl()
    }

    @Provides
    @Singleton
    fun provideTimerStateMachine(): TimerStateMachine {
        return TimerStateMachine()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FomodoroDatabase {
        return FomodoroDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSessionDao(database: FomodoroDatabase): SessionDao {
        return database.sessionDao()
    }

    @Provides
    @Singleton
    fun provideHapticManager(@ApplicationContext context: Context): HapticManager {
        return HapticManager(context)
    }

    @Provides
    @Singleton
    fun provideTorchManager(@ApplicationContext context: Context): TorchManager {
        return TorchManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioEngine(@ApplicationContext context: Context): com.arijit.pomodoro.core.audio.AudioEngine {
        return com.arijit.pomodoro.core.audio.AudioEngine(context)
    }

    @Provides
    @Singleton
    fun provideSoundAlertManager(@ApplicationContext context: Context): SoundAlertManager {
        return SoundAlertManager(context)
    }

    @Provides
    @Singleton
    fun provideLiveUpdatesHelper(@ApplicationContext context: Context): LiveUpdatesHelper {
        return LiveUpdatesHelper(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        liveUpdatesHelper: LiveUpdatesHelper
    ): NotificationHelper {
        return NotificationHelper(context, liveUpdatesHelper)
    }

    @Provides
    @Singleton
    fun providePhaseAlertManager(
        @ApplicationContext context: Context,
        soundAlertManager: SoundAlertManager,
        hapticManager: HapticManager,
        torchManager: TorchManager,
        notificationHelper: NotificationHelper,
        sessionDao: SessionDao,
        scope: CoroutineScope
    ): PhaseAlertManager {
        return PhaseAlertManager(
            context = context,
            soundAlertManager = soundAlertManager,
            hapticManager = hapticManager,
            torchManager = torchManager,
            notificationHelper = notificationHelper,
            sessionDao = sessionDao,
            scope = scope
        )
    }

    @Provides
    @Singleton
    fun provideTimerController(
        clock: Clock,
        stateMachine: TimerStateMachine,
        scope: CoroutineScope
    ): TimerController {
        return TimerController(
            clock = clock,
            stateMachine = stateMachine,
            scope = scope
        )
    }
}

