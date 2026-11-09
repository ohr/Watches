package org.ear.watches.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Audio and haptic feedback for stopwatch actions, designed to be told apart without looking:
 *
 * | Action | Sound                          | Vibration          |
 * |--------|--------------------------------|--------------------|
 * | Start  | rising two notes (880→1320 Hz) | one medium pulse   |
 * | Lap    | one short high blip (1760 Hz)  | one short tick     |
 * | Stop   | falling two notes (1320→880 Hz)| two pulses         |
 * | Reset  | one long low tone (330 Hz)     | one long pulse     |
 *
 * Sounds are synthesized once and preloaded into static [AudioTrack]s for minimal latency.
 * (`ToneGenerator`'s predefined tones were indistinguishable: they are fixed-length beeps that
 * the requested duration only cuts off or pads with silence.)
 */
class FeedbackManager(context: Context) {

    /** The distinguishable feedback cues. */
    enum class Cue(val tones: List<Tone>, val vibrationTimings: LongArray) {
        START(listOf(Tone(880.0, 70), Tone(1320.0, 110)), longArrayOf(0, 60)),
        LAP(listOf(Tone(1760.0, 60)), longArrayOf(0, 25)),
        STOP(listOf(Tone(1320.0, 70), Tone(880.0, 140)), longArrayOf(0, 45, 70, 45)),
        RESET(listOf(Tone(330.0, 260)), longArrayOf(0, 220))
    }

    /** A sine tone segment; [frequencyHz] 0 means silence. */
    data class Tone(val frequencyHz: Double, val durationMs: Int)

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                @Suppress("DEPRECATION")
                vibratorManager?.defaultVibrator ?: (appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private val tracks: Map<Cue, AudioTrack> = Cue.entries.mapNotNull { cue ->
        createTrack(synthesize(cue.tones))?.let { cue to it }
    }.toMap()

    fun playStart(soundEnabled: Boolean = true, hapticsEnabled: Boolean = true) =
        play(Cue.START, soundEnabled, hapticsEnabled)

    fun playLap(soundEnabled: Boolean = true, hapticsEnabled: Boolean = true) =
        play(Cue.LAP, soundEnabled, hapticsEnabled)

    fun playStop(soundEnabled: Boolean = true, hapticsEnabled: Boolean = true) =
        play(Cue.STOP, soundEnabled, hapticsEnabled)

    fun playReset(soundEnabled: Boolean = true, hapticsEnabled: Boolean = true) =
        play(Cue.RESET, soundEnabled, hapticsEnabled)

    /** Whether the sound for [cue] is ready to play (false if audio could not be initialized). */
    fun isSoundReady(cue: Cue): Boolean = tracks[cue]?.state == AudioTrack.STATE_INITIALIZED

    private fun play(cue: Cue, soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) vibrate(cue.vibrationTimings)
        if (soundEnabled) playSound(cue)
    }

    private fun playSound(cue: Cue) {
        val track = tracks[cue] ?: return
        try {
            // Restart from the beginning, also when the same cue is triggered in quick succession.
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {
            // Ignore audio errors (e.g. audio focus or device issues); timing must never be affected.
        }
    }

    private fun vibrate(timings: LongArray) {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        } catch (_: Exception) {
            // Ignore vibration errors on emulators/devices without hardware.
        }
    }

    private fun createTrack(samples: ShortArray): AudioTrack? = try {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
            .also { it.write(samples, 0, samples.size) }
    } catch (_: Exception) {
        null
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
    }

    companion object {
        const val SAMPLE_RATE = 44_100
        private const val AMPLITUDE = 0.6
        private const val FADE_MS = 5

        /**
         * Renders [tones] to 16-bit mono PCM. Each segment fades in and out over a few
         * milliseconds to avoid audible clicks at tone boundaries.
         */
        fun synthesize(tones: List<Tone>, sampleRate: Int = SAMPLE_RATE): ShortArray {
            val out = ArrayList<Short>()
            tones.forEach { tone ->
                val count = sampleRate * tone.durationMs / 1000
                val fade = min(sampleRate * FADE_MS / 1000, count / 2)
                for (i in 0 until count) {
                    val envelope = when {
                        fade == 0 -> 1.0
                        i < fade -> i.toDouble() / fade
                        i >= count - fade -> (count - 1 - i).toDouble() / fade
                        else -> 1.0
                    }
                    val value = if (tone.frequencyHz <= 0.0) 0.0
                    else sin(2 * PI * tone.frequencyHz * i / sampleRate) * AMPLITUDE * envelope
                    out += (value * Short.MAX_VALUE).toInt().toShort()
                }
            }
            return out.toShortArray()
        }
    }
}
