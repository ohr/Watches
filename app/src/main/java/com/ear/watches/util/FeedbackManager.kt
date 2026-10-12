package com.ear.watches.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

typealias SoundHapticsHelper = FeedbackManager

/**
 * Manages tactile (vibration) and audio (tone/beep) feedback for stopwatch user actions.
 */
class FeedbackManager(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator ?: (appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (_: Exception) {
            null
        }
    }

    fun playStart(enabled: Boolean = true) {
        if (!enabled) return
        vibrate(60L)
        playTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    fun playLap(enabled: Boolean = true) {
        if (!enabled) return
        vibrate(40L)
        playTone(ToneGenerator.TONE_PROP_BEEP2, 80)
    }

    fun playStop(enabled: Boolean = true) {
        if (!enabled) return
        vibrate(80L)
        playTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    fun playReset(enabled: Boolean = true) {
        if (!enabled) return
        vibrate(100L)
        playTone(ToneGenerator.TONE_PROP_NACK, 120)
    }

    fun playFeedback(enabled: Boolean = true) {
        if (!enabled) return
        vibrate(50L)
        playTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    private fun vibrate(durationMs: Long) {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
            // Ignore vibration error on emulators/devices without permission or hardware
        }
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (_: Exception) {
            // Ignore tone error
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {
        }
    }
}
