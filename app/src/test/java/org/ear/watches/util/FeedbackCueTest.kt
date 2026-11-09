package org.ear.watches.util

import org.ear.watches.util.FeedbackManager.Cue
import org.ear.watches.util.FeedbackManager.Tone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class FeedbackCueTest {

    @Test
    fun allCues_soundDifferent() {
        val signatures = Cue.entries.map { cue -> cue.tones.map { it.frequencyHz } }
        assertEquals(signatures.size, signatures.toSet().size, "Cues must not share the same melody: $signatures")
    }

    @Test
    fun startRises_stopFalls() {
        val start = Cue.START.tones.map { it.frequencyHz }
        val stop = Cue.STOP.tones.map { it.frequencyHz }
        assertTrue(start.zipWithNext().all { (a, b) -> b > a }, "Start should rise: $start")
        assertTrue(stop.zipWithNext().all { (a, b) -> b < a }, "Stop should fall: $stop")
    }

    @Test
    fun allCues_vibrateDifferently() {
        val patterns = Cue.entries.map { it.vibrationTimings.toList() }
        assertEquals(patterns.size, patterns.toSet().size, "Vibration patterns must differ: $patterns")
    }

    @Test
    fun allCues_areShort() {
        // Feedback must never lag behind quickly repeated actions (e.g. several laps in a row).
        Cue.entries.forEach { cue -> assertTrue(cue.tones.sumOf { it.durationMs } <= 300, "$cue too long") }
    }

    @Test
    fun synthesize_producesExpectedLengthAndFadesInAndOut() {
        val samples = FeedbackManager.synthesize(listOf(Tone(1000.0, 100)), sampleRate = 10_000)

        assertEquals(1_000, samples.size)
        assertEquals(0, samples.first().toInt())
        assertTrue(abs(samples.last().toInt()) < 2_000, "Should fade out, was ${samples.last()}")
        assertTrue(samples.maxOf { abs(it.toInt()) } > 15_000, "Should reach audible amplitude")
    }

    @Test
    fun synthesize_zeroFrequencyIsSilence() {
        val samples = FeedbackManager.synthesize(listOf(Tone(0.0, 20)), sampleRate = 10_000)
        assertTrue(samples.all { it.toInt() == 0 })
    }
}
