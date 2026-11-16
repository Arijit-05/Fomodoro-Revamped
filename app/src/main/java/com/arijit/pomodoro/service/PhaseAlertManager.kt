package com.arijit.pomodoro.service

import android.content.Context
import com.arijit.pomodoro.core.audio.SoundAlertManager
import com.arijit.pomodoro.core.database.dao.SessionDao
import com.arijit.pomodoro.core.database.entity.SessionRecordEntity
import com.arijit.pomodoro.core.datastore.UserPreferences
import com.arijit.pomodoro.core.model.CycleState
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.system.HapticManager
import com.arijit.pomodoro.system.TorchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PhaseAlertManager(
    private val context: Context,
    private val soundAlertManager: SoundAlertManager,
    private val hapticManager: HapticManager,
    private val torchManager: TorchManager,
    private val notificationHelper: NotificationHelper,
    private val sessionDao: SessionDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    fun onPhaseCompleted(
        phase: Phase,
        cycleState: CycleState,
        durationMinutes: Int,
        preferences: UserPreferences,
        breakPrompt: String? = null
    ) {
        // 1. Sound Alert (runCatching)
        runCatching {
            soundAlertManager.playPhaseEndSound(phase, preferences)
        }

        // 2. Vibration Alert (runCatching)
        runCatching {
            hapticManager.hapticLevel = preferences.hapticLevel
            if (phase == Phase.FOCUS) {
                hapticManager.phaseEndFocus()
            } else {
                hapticManager.phaseEndBreak()
            }
        }

        // 3. Torch Blink (runCatching)
        runCatching {
            if (preferences.flashNotification) {
                torchManager.blinkTorch()
            }
        }

        // 4. Notification Alert (runCatching)
        runCatching {
            notificationHelper.postPhaseCompleteNotification(phase, cycleState, breakPrompt)
        }

        // 5. Database Logging (runCatching)
        runCatching {
            scope.launch {
                val now = System.currentTimeMillis()
                val startTime = now - (durationMinutes * 60 * 1000L)
                sessionDao.insertSession(
                    SessionRecordEntity(
                        startTimeWallClock = startTime,
                        endTimeWallClock = now,
                        durationMinutes = durationMinutes,
                        phase = phase.name,
                        sessionNumber = cycleState.currentSession,
                        totalSessions = cycleState.totalSessions,
                        completed = true
                    )
                )
            }
        }
    }
}

