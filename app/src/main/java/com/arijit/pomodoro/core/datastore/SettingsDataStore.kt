package com.arijit.pomodoro.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arijit.pomodoro.core.model.CycleState
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSettings
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fomodoro_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        // Timer settings
        val KEY_FOCUS_DURATION = intPreferencesKey("focus_duration")
        val KEY_SHORT_BREAK_DURATION = intPreferencesKey("short_break_duration")
        val KEY_LONG_BREAK_DURATION = intPreferencesKey("long_break_duration")
        val KEY_SESSIONS_PER_CYCLE = intPreferencesKey("sessions_per_cycle")
        val KEY_AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val KEY_AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
        val KEY_SNOOZE_DURATION = intPreferencesKey("snooze_duration")

        // Appearance
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEY_ULTRA_FOCUS_MODE = booleanPreferencesKey("ultra_focus_mode")
        val KEY_AMBIENT_AOD_MODE = booleanPreferencesKey("ambient_aod_mode")

        // Audio
        val KEY_END_FOCUS_SOUND = stringPreferencesKey("end_focus_sound")
        val KEY_END_BREAK_SOUND = stringPreferencesKey("end_break_sound")
        val KEY_TICKING_STYLE = stringPreferencesKey("ticking_style")
        val KEY_TICKING_VOLUME = floatPreferencesKey("ticking_volume")
        val KEY_TICK_ONLY_FOCUS = booleanPreferencesKey("tick_only_focus")
        val KEY_AMBIENT_STYLE = stringPreferencesKey("ambient_style")
        val KEY_AMBIENT_URI = stringPreferencesKey("ambient_uri")
        val KEY_AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
        val KEY_PAUSE_AMBIENT_BREAK = booleanPreferencesKey("pause_ambient_break")

        // Feedback
        val KEY_HAPTIC_LEVEL = stringPreferencesKey("haptic_level")
        val KEY_FLASH_NOTIFICATION = booleanPreferencesKey("flash_notification")

        // Live updates
        val KEY_LIVE_UPDATES_ENABLED = booleanPreferencesKey("live_updates_enabled")

        // Break prompts
        val KEY_BREAK_PROMPTS = stringPreferencesKey("break_prompts_joined")

        // Snapshot persistence for process restore / reboot
        val KEY_SNAPSHOT_STATUS = stringPreferencesKey("snapshot_status")
        val KEY_SNAPSHOT_PHASE = stringPreferencesKey("snapshot_phase")
        val KEY_SNAPSHOT_CURRENT_SESSION = intPreferencesKey("snapshot_current_session")
        val KEY_SNAPSHOT_TOTAL_SESSIONS = intPreferencesKey("snapshot_total_sessions")
        val KEY_SNAPSHOT_COMPLETED_CYCLES = intPreferencesKey("snapshot_completed_cycles")
        val KEY_SNAPSHOT_TOTAL_DURATION = longPreferencesKey("snapshot_total_duration")
        val KEY_SNAPSHOT_REMAINING_MILLIS = longPreferencesKey("snapshot_remaining_millis")
        val KEY_SNAPSHOT_TARGET_WALL = longPreferencesKey("snapshot_target_wall")
        val KEY_SNAPSHOT_TARGET_ELAPSED = longPreferencesKey("snapshot_target_elapsed")
        val KEY_SNAPSHOT_BOOT_COUNT = intPreferencesKey("snapshot_boot_count")
        val KEY_SNAPSHOT_IS_SNOOZED = booleanPreferencesKey("snapshot_is_snoozed")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { pref ->
        UserPreferences(
            focusDurationMinutes = pref[KEY_FOCUS_DURATION] ?: 25,
            shortBreakDurationMinutes = pref[KEY_SHORT_BREAK_DURATION] ?: 5,
            longBreakDurationMinutes = pref[KEY_LONG_BREAK_DURATION] ?: 15,
            sessionsPerCycle = pref[KEY_SESSIONS_PER_CYCLE] ?: 4,
            autoStartBreaks = pref[KEY_AUTO_START_BREAKS] ?: false,
            autoStartFocus = pref[KEY_AUTO_START_FOCUS] ?: false,
            snoozeDurationMinutes = pref[KEY_SNOOZE_DURATION] ?: 5,
            themeMode = ThemeMode.valueOf(pref[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name),
            dynamicColor = pref[KEY_DYNAMIC_COLOR] ?: true,
            amoledBlack = pref[KEY_AMOLED_BLACK] ?: false,
            keepScreenOn = pref[KEY_KEEP_SCREEN_ON] ?: false,
            ultraFocusMode = pref[KEY_ULTRA_FOCUS_MODE] ?: false,
            ambientAodMode = pref[KEY_AMBIENT_AOD_MODE] ?: false,
            endOfFocusSoundUri = pref[KEY_END_FOCUS_SOUND],
            endOfBreakSoundUri = pref[KEY_END_BREAK_SOUND],
            tickingSoundStyle = pref[KEY_TICKING_STYLE] ?: "none",
            tickingVolume = pref[KEY_TICKING_VOLUME] ?: 0.5f,
            tickOnlyDuringFocus = pref[KEY_TICK_ONLY_FOCUS] ?: true,
            ambientSoundStyle = pref[KEY_AMBIENT_STYLE] ?: "none",
            ambientSoundUri = pref[KEY_AMBIENT_URI],
            ambientVolume = pref[KEY_AMBIENT_VOLUME] ?: 0.7f,
            pauseAmbientDuringBreak = pref[KEY_PAUSE_AMBIENT_BREAK] ?: true,
            hapticLevel = HapticLevel.valueOf(pref[KEY_HAPTIC_LEVEL] ?: HapticLevel.STANDARD.name),
            flashNotification = pref[KEY_FLASH_NOTIFICATION] ?: false,
            liveUpdatesEnabled = pref[KEY_LIVE_UPDATES_ENABLED] ?: true,
            breakPrompts = pref[KEY_BREAK_PROMPTS]?.split(";;;") ?: defaultBreakPrompts
        )
    }

    val timerSettings: Flow<TimerSettings> = userPreferences.map { pref ->
        TimerSettings(
            focusDurationMinutes = pref.focusDurationMinutes,
            shortBreakDurationMinutes = pref.shortBreakDurationMinutes,
            longBreakDurationMinutes = pref.longBreakDurationMinutes,
            sessionsPerCycle = pref.sessionsPerCycle,
            autoStartBreaks = pref.autoStartBreaks,
            autoStartFocus = pref.autoStartFocus,
            snoozeDurationMinutes = pref.snoozeDurationMinutes
        )
    }

    suspend fun updateTimerSettings(
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int,
        sessionsPerCycle: Int,
        autoStartBreaks: Boolean,
        autoStartFocus: Boolean,
        snoozeMinutes: Int
    ) {
        context.dataStore.edit { pref ->
            pref[KEY_FOCUS_DURATION] = focusMinutes
            pref[KEY_SHORT_BREAK_DURATION] = shortBreakMinutes
            pref[KEY_LONG_BREAK_DURATION] = longBreakMinutes
            pref[KEY_SESSIONS_PER_CYCLE] = sessionsPerCycle
            pref[KEY_AUTO_START_BREAKS] = autoStartBreaks
            pref[KEY_AUTO_START_FOCUS] = autoStartFocus
            pref[KEY_SNOOZE_DURATION] = snoozeMinutes
        }
    }

    suspend fun saveSnapshot(snapshot: TimerSnapshot) {
        context.dataStore.edit { pref ->
            pref[KEY_SNAPSHOT_STATUS] = snapshot.status.name
            pref[KEY_SNAPSHOT_PHASE] = snapshot.phase.name
            pref[KEY_SNAPSHOT_CURRENT_SESSION] = snapshot.cycleState.currentSession
            pref[KEY_SNAPSHOT_TOTAL_SESSIONS] = snapshot.cycleState.totalSessions
            pref[KEY_SNAPSHOT_COMPLETED_CYCLES] = snapshot.cycleState.completedCycles
            pref[KEY_SNAPSHOT_TOTAL_DURATION] = snapshot.totalDurationMillis
            pref[KEY_SNAPSHOT_REMAINING_MILLIS] = snapshot.remainingMillis
            pref[KEY_SNAPSHOT_TARGET_WALL] = snapshot.targetEndTimeWallClock
            pref[KEY_SNAPSHOT_TARGET_ELAPSED] = snapshot.targetEndTimeElapsedRealtime
            pref[KEY_SNAPSHOT_BOOT_COUNT] = snapshot.bootCount
            pref[KEY_SNAPSHOT_IS_SNOOZED] = snapshot.isSnoozed
        }
    }

    val savedSnapshot: Flow<TimerSnapshot?> = context.dataStore.data.map { pref ->
        val statusStr = pref[KEY_SNAPSHOT_STATUS] ?: return@map null
        val phaseStr = pref[KEY_SNAPSHOT_PHASE] ?: Phase.FOCUS.name

        TimerSnapshot(
            status = TimerStatus.valueOf(statusStr),
            phase = Phase.valueOf(phaseStr),
            cycleState = CycleState(
                currentSession = pref[KEY_SNAPSHOT_CURRENT_SESSION] ?: 1,
                totalSessions = pref[KEY_SNAPSHOT_TOTAL_SESSIONS] ?: 4,
                completedCycles = pref[KEY_SNAPSHOT_COMPLETED_CYCLES] ?: 0
            ),
            totalDurationMillis = pref[KEY_SNAPSHOT_TOTAL_DURATION] ?: (25 * 60 * 1000L),
            remainingMillis = pref[KEY_SNAPSHOT_REMAINING_MILLIS] ?: (25 * 60 * 1000L),
            targetEndTimeWallClock = pref[KEY_SNAPSHOT_TARGET_WALL] ?: 0L,
            targetEndTimeElapsedRealtime = pref[KEY_SNAPSHOT_TARGET_ELAPSED] ?: 0L,
            bootCount = pref[KEY_SNAPSHOT_BOOT_COUNT] ?: 0,
            isSnoozed = pref[KEY_SNAPSHOT_IS_SNOOZED] ?: false
        )
    }

    suspend fun setUltraFocusMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ULTRA_FOCUS_MODE] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AMOLED_BLACK] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAmbientAodMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AMBIENT_AOD_MODE] = enabled }
    }

    suspend fun setLiveUpdatesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_UPDATES_ENABLED] = enabled }
    }

    suspend fun setHapticLevel(level: HapticLevel) {
        context.dataStore.edit { it[KEY_HAPTIC_LEVEL] = level.name }
    }

    suspend fun setFlashNotification(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FLASH_NOTIFICATION] = enabled }
    }

    suspend fun setSoundUris(focusSoundUri: String?, breakSoundUri: String?) {
        context.dataStore.edit {
            if (focusSoundUri != null) it[KEY_END_FOCUS_SOUND] = focusSoundUri else it.remove(KEY_END_FOCUS_SOUND)
            if (breakSoundUri != null) it[KEY_END_BREAK_SOUND] = breakSoundUri else it.remove(KEY_END_BREAK_SOUND)
        }
    }

    suspend fun setTickingConfig(style: String, volume: Float, onlyDuringFocus: Boolean) {
        context.dataStore.edit {
            it[KEY_TICKING_STYLE] = style
            it[KEY_TICKING_VOLUME] = volume
            it[KEY_TICK_ONLY_FOCUS] = onlyDuringFocus
        }
    }

    suspend fun setAmbientConfig(style: String, uri: String?, volume: Float, pauseDuringBreak: Boolean) {
        context.dataStore.edit {
            it[KEY_AMBIENT_STYLE] = style
            if (uri != null) it[KEY_AMBIENT_URI] = uri else it.remove(KEY_AMBIENT_URI)
            it[KEY_AMBIENT_VOLUME] = volume
            it[KEY_PAUSE_AMBIENT_BREAK] = pauseDuringBreak
        }
    }

    suspend fun setBreakPrompts(prompts: List<String>) {
        context.dataStore.edit {
            it[KEY_BREAK_PROMPTS] = prompts.joinToString(";;;")
        }
    }
}

