package com.arijit.pomodoro.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.arijit.pomodoro.core.datastore.UserPreferences
import com.arijit.pomodoro.core.model.Phase

class SoundAlertManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun playPhaseEndSound(phase: Phase, preferences: UserPreferences) {
        val soundUriString = if (phase == Phase.FOCUS) {
            preferences.endOfFocusSoundUri
        } else {
            preferences.endOfBreakSoundUri
        }

        stop()

        runCatching {
            val uri = if (!soundUriString.isNullOrEmpty()) {
                Uri.parse(soundUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                start()
                setOnCompletionListener {
                    stop()
                }
            }
        }.onFailure {
            // If custom URI or ringtone fails (permission revoked or file moved), try default alarm ringtone
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (fallbackUri != null) {
                    val ringtone = RingtoneManager.getRingtone(context, fallbackUri)
                    ringtone.play()
                }
            }
        }
    }

    fun stop() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}

