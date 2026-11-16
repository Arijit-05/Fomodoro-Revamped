package com.arijit.pomodoro.system

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.arijit.pomodoro.core.datastore.HapticLevel

class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    var hapticLevel: HapticLevel = HapticLevel.STANDARD

    private fun getAmplitudeMultiplier(): Float {
        return when (hapticLevel) {
            HapticLevel.OFF -> 0f
            HapticLevel.SUBTLE -> 0.5f
            HapticLevel.STANDARD -> 1.0f
            HapticLevel.STRONG -> 1.5f
        }
    }

    fun start(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        playComposition(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)
    }

    fun pause(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        playComposition(VibrationEffect.Composition.PRIMITIVE_THUD)
    }

    fun resume(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        playComposition(VibrationEffect.Composition.PRIMITIVE_CLICK)
    }

    fun skip(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
        }
        playWaveform(longArrayOf(0, 30, 60, 30), intArrayOf(0, scaleAmp(120), 0, scaleAmp(180)))
    }

    fun reset(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        playWaveform(longArrayOf(0, 60, 40, 60), intArrayOf(0, scaleAmp(200), 0, scaleAmp(255)))
    }

    fun toggle(enabled: Boolean, view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val constant = if (enabled) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
            view?.performHapticFeedback(constant)
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        playComposition(VibrationEffect.Composition.PRIMITIVE_CLICK, scale = if (enabled) 0.8f else 0.5f)
    }

    fun sliderTick(isRound: Boolean, view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view?.performHapticFeedback(
                if (isRound) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            )
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
        playComposition(VibrationEffect.Composition.PRIMITIVE_TICK, scale = if (isRound) 1.0f else 0.4f)
    }

    fun ultraFocusRamp(progress: Float) {
        if (hapticLevel == HapticLevel.OFF) return
        val scale = (0.2f + (progress * 0.8f)).coerceIn(0.1f, 1f)
        playComposition(VibrationEffect.Composition.PRIMITIVE_TICK, scale = scale)
    }

    fun ultraFocusExitSuccess(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        playComposition(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, scale = 1f)
    }

    fun snooze(view: View? = null) {
        if (hapticLevel == HapticLevel.OFF) return
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        playComposition(VibrationEffect.Composition.PRIMITIVE_CLICK, scale = 0.6f)
    }

    fun phaseEndFocus() {
        if (hapticLevel == HapticLevel.OFF) return
        // Triumphant pattern: Rising fanfare pulses
        playWaveform(
            timings = longArrayOf(0, 150, 100, 150, 100, 350),
            amplitudes = intArrayOf(0, scaleAmp(150), 0, scaleAmp(200), 0, scaleAmp(255))
        )
    }

    fun phaseEndBreak() {
        if (hapticLevel == HapticLevel.OFF) return
        // Gentler wake-up pattern: Soft two-pulse tap
        playWaveform(
            timings = longArrayOf(0, 100, 150, 200),
            amplitudes = intArrayOf(0, scaleAmp(100), 0, scaleAmp(150))
        )
    }

    private fun scaleAmp(base: Int): Int {
        return (base * getAmplitudeMultiplier()).toInt().coerceIn(1, 255)
    }

    private fun playComposition(primitiveId: Int, scale: Float = 1.0f) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (vib.areAllPrimitivesSupported(primitiveId)) {
                    val composition = VibrationEffect.startComposition()
                        .addPrimitive(primitiveId, (scale * getAmplitudeMultiplier()).coerceIn(0.01f, 1.0f))
                        .compose()
                    vib.vibrate(composition)
                    return
                }
            }
            // Fallback for older APIs or unsupported primitives
            val duration = if (primitiveId == VibrationEffect.Composition.PRIMITIVE_TICK) 20L else 40L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(duration, scaleAmp((scale * 180).toInt())))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(duration)
            }
        }
    }

    private fun playWaveform(timings: LongArray, amplitudes: IntArray) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        }
    }
}

