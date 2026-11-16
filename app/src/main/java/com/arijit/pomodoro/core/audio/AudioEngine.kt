package com.arijit.pomodoro.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arijit.pomodoro.R
import com.arijit.pomodoro.core.datastore.UserPreferences
import com.arijit.pomodoro.core.model.Phase
import com.arijit.pomodoro.core.model.TimerSnapshot
import com.arijit.pomodoro.core.model.TimerStatus

class AudioEngine(private val context: Context) {

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val ambientPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(audioAttributes, true /* handle audio focus */)
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private val tickingPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(audioAttributes, false)
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private var currentAmbientStyle: String? = null
    private var currentAmbientUri: String? = null
    private var currentTickingStyle: String? = null

    fun updatePlaybackState(snapshot: TimerSnapshot, preferences: UserPreferences) {
        val isTimerRunning = snapshot.status == TimerStatus.RUNNING
        val isBreak = snapshot.phase.isBreak

        // 1. Ambient Audio Logic
        val shouldPlayAmbient = isTimerRunning &&
                preferences.ambientSoundStyle != "none" &&
                (!isBreak || !preferences.pauseAmbientDuringBreak)

        if (shouldPlayAmbient) {
            setupAmbientMediaItem(preferences)
            ambientPlayer.volume = preferences.ambientVolume.coerceIn(0f, 1f)
            if (!ambientPlayer.isPlaying) {
                ambientPlayer.play()
            }
        } else {
            if (ambientPlayer.isPlaying) {
                ambientPlayer.pause()
            }
        }

        // 2. Ticking Sound Logic
        val shouldPlayTicking = isTimerRunning &&
                preferences.tickingSoundStyle != "none" &&
                (!isBreak || !preferences.tickOnlyDuringFocus)

        if (shouldPlayTicking) {
            setupTickingMediaItem(preferences)
            tickingPlayer.volume = preferences.tickingVolume.coerceIn(0f, 1f)
            if (!tickingPlayer.isPlaying) {
                tickingPlayer.play()
            }
        } else {
            if (tickingPlayer.isPlaying) {
                tickingPlayer.pause()
            }
        }
    }

    private fun setupAmbientMediaItem(preferences: UserPreferences) {
        val style = preferences.ambientSoundStyle
        val uriStr = preferences.ambientSoundUri

        if (style == currentAmbientStyle && uriStr == currentAmbientUri) {
            return
        }

        currentAmbientStyle = style
        currentAmbientUri = uriStr

        val uri: Uri = when (style) {
            "brown_noise" -> getRawUri(R.raw.ambient_brown_noise)
            "rain" -> getRawUri(R.raw.ambient_rain)
            "ocean" -> getRawUri(R.raw.ambient_ocean)
            "fireplace" -> getRawUri(R.raw.ambient_fireplace)
            "forest" -> getRawUri(R.raw.ambient_forest)
            "cafe" -> getRawUri(R.raw.ambient_cafe)
            "custom" -> {
                if (!uriStr.isNullOrEmpty()) {
                    runCatching { Uri.parse(uriStr) }.getOrDefault(getRawUri(R.raw.ambient_brown_noise))
                } else {
                    getRawUri(R.raw.ambient_brown_noise)
                }
            }
            else -> getRawUri(R.raw.ambient_brown_noise)
        }

        val mediaItem = MediaItem.fromUri(uri)
        ambientPlayer.setMediaItem(mediaItem)
        ambientPlayer.prepare()
    }

    private fun setupTickingMediaItem(preferences: UserPreferences) {
        val style = preferences.tickingSoundStyle
        if (style == currentTickingStyle) {
            return
        }
        currentTickingStyle = style

        val rawRes = when (style) {
            "mechanical" -> R.raw.tick_mechanical
            "digital" -> R.raw.tick_digital
            "soft" -> R.raw.tick_soft
            else -> R.raw.tick_mechanical
        }

        val mediaItem = MediaItem.fromUri(getRawUri(rawRes))
        tickingPlayer.setMediaItem(mediaItem)
        tickingPlayer.prepare()
    }

    private fun getRawUri(rawResId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$rawResId")
    }

    fun release() {
        runCatching {
            ambientPlayer.release()
            tickingPlayer.release()
        }
    }
}

