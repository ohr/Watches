package org.ear.watches.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StopwatchItemTest {

    private val initialWatch = StopwatchItem(
        name = "Test Athlete",
        pastelColorHex = 0xFF90CAF9L
    )

    @Test
    fun start_newWatch_setsIsRunningAndCreatesInitialRunSession() {
        val started = initialWatch.start(nowNanos = 1_000_000_000L, nowMillis = 1_000L)

        assertTrue(started.isRunning)
        assertEquals(1, started.runs.size)
        val activeRun = started.activeRun!!
        assertEquals(1, activeRun.runIndex)
        assertEquals(1_000_000_000L, activeRun.startTimeNanos)
        assertEquals(1_000L, activeRun.startTimeMillis)
        assertTrue(activeRun.isRunning)
        assertFalse(activeRun.isFinished)
    }

    @Test
    fun start_alreadyRunningWatch_returnsSameInstance() {
        val startedOnce = initialWatch.start(nowNanos = 1_000_000_000L, nowMillis = 1_000L)
        val startedAgain = startedOnce.start(nowNanos = 2_000_000_000L, nowMillis = 2_000L)

        assertSame(startedOnce, startedAgain)
    }

    @Test
    fun start_pausedWatch_createsNewRunSession() {
        val started = initialWatch.start(nowNanos = 1_000_000_000L, nowMillis = 1_000L)
        val paused = started.pause(nowNanos = 5_000_000_000L)

        val restarted = paused.start(nowNanos = 7_000_000_000L, nowMillis = 7_000L)

        assertTrue(restarted.isRunning)
        assertEquals(2, restarted.runs.size)

        val run2 = restarted.runs[1]
        assertEquals(2, run2.runIndex)
        assertEquals(7_000_000_000L, run2.startTimeNanos)
        assertEquals(7_000L, run2.startTimeMillis)
    }

    @Test
    fun start_watchAfterShortReset_activatesPreparedRunSession() {
        val started = initialWatch.start(nowNanos = 1_000_000_000L, nowMillis = 1_000L)
        val resetWatch = started.reset(nowNanos = 5_000_000_000L)

        assertFalse(resetWatch.isRunning)
        assertEquals(2, resetWatch.runs.size)
        assertTrue(resetWatch.runs.last().isUnstarted)

        val restarted = resetWatch.start(nowNanos = 8_000_000_000L, nowMillis = 8_000L)

        assertTrue(restarted.isRunning)
        assertEquals(2, restarted.runs.size)
        val activeRun = restarted.activeRun!!
        assertEquals(2, activeRun.runIndex)
        assertEquals(8_000_000_000L, activeRun.startTimeNanos)
        assertEquals(8_000L, activeRun.startTimeMillis)
    }

    @Test
    fun pause_runningWatch_stopsWatchAndCalculatesAccumulatedNanos() {
        val started = initialWatch.start(nowNanos = 1_000_000_000L, nowMillis = 1_000L)
        val paused = started.pause(nowNanos = 6_000_000_000L)

        assertFalse(paused.isRunning)
        assertEquals(1, paused.runs.size)
        val activeRun = paused.activeRun!!
        assertEquals(6_000_000_000L, activeRun.endTimeNanos)
        assertTrue(activeRun.isFinished)
        assertFalse(activeRun.isRunning)
        assertEquals(5_000_000_000L, activeRun.accumulatedNanos)
        assertEquals(5000L, paused.getElapsedMillis(nowNanos = 6_000_000_000L))
    }

    @Test
    fun pause_notRunningWatch_returnsSameInstance() {
        val paused = initialWatch.pause(nowNanos = 1_000_000_000L)

        assertSame(initialWatch, paused)
    }

    @Test
    fun lap_runningWatch_recordsLapsWithCorrectDeltaAndTotalTime() {
        val started = initialWatch.start(nowNanos = 0L, nowMillis = 0L)
        val lap1 = started.lap(nowNanos = 4_000_000_000L)
        val lap2 = lap1.lap(nowNanos = 10_000_000_000L)

        val laps = lap2.currentRunLaps
        assertEquals(2, laps.size)

        assertEquals(1, laps[0].lapNumber)
        assertEquals(4_000_000_000L, laps[0].lapTimeNanos)
        assertEquals(4_000_000_000L, laps[0].totalTimeNanos)
        assertNull(laps[0].lapDeltaNanos)

        assertEquals(2, laps[1].lapNumber)
        assertEquals(6_000_000_000L, laps[1].lapTimeNanos)
        assertEquals(10_000_000_000L, laps[1].totalTimeNanos)
        assertEquals(2_000_000_000L, laps[1].lapDeltaNanos)
    }

    @Test
    fun lap_notRunningWatch_returnsSameInstance() {
        val lapped = initialWatch.lap(nowNanos = 1_000_000_000L)

        assertSame(initialWatch, lapped)
    }

    @Test
    fun reset_runningWatch_finalizesActiveRunAndPreparesNewRunSession() {
        val started = initialWatch.start(nowNanos = 1_000_000_000L, nowMillis = 1_000L)
        val resetWatch = started.reset(nowNanos = 5_000_000_000L)

        assertFalse(resetWatch.isRunning)
        assertEquals(2, resetWatch.runs.size)
        assertEquals(4_000_000_000L, resetWatch.runs[0].accumulatedNanos)
        assertTrue(resetWatch.runs[1].isUnstarted)

        assertEquals(0L, resetWatch.getElapsedMillis(nowNanos = 5_000_000_000L))
        assertEquals(4000L, resetWatch.getTotalCumulativeMillis(nowNanos = 5_000_000_000L))
    }

    @Test
    fun reset_stoppedWatchWithCompletedRun_resetsDisplayTimeAndPreparesNewSession() {
        val started = initialWatch.start(nowNanos = 0L, nowMillis = 0L)
        val paused = started.pause(nowNanos = 3_000_000_000L)
        val resetWatch = paused.reset(nowNanos = 3_000_000_000L)

        assertFalse(resetWatch.isRunning)
        assertEquals(0L, resetWatch.getElapsedMillis(nowNanos = 3_000_000_000L))
        assertEquals(3000L, resetWatch.getTotalCumulativeMillis(nowNanos = 3_000_000_000L))
    }

    @Test
    fun reset_alreadyPreparedWatch_doesNotDuplicatePreparedSessions() {
        val started = initialWatch.start(nowNanos = 0L, nowMillis = 0L)
        val resetOnce = started.reset(nowNanos = 2_000_000_000L)
        val resetTwice = resetOnce.reset(nowNanos = 2_000_000_000L)

        assertEquals(resetOnce.runs.size, resetTwice.runs.size)
    }

    @Test
    fun resetFull_clearsAllRunsAndHistory() {
        val started = initialWatch.start(nowNanos = 0L, nowMillis = 0L)
        val lapped = started.lap(nowNanos = 2_000_000_000L)
        val paused = lapped.pause(nowNanos = 5_000_000_000L)

        val resetFullWatch = paused.resetFull()

        assertFalse(resetFullWatch.isRunning)
        assertTrue(resetFullWatch.runs.isEmpty())
        assertTrue(resetFullWatch.allLaps.isEmpty())
        assertEquals(0L, resetFullWatch.getElapsedMillis(nowNanos = 5_000_000_000L))
        assertEquals(0L, resetFullWatch.getTotalCumulativeMillis(nowNanos = 5_000_000_000L))
    }
}
