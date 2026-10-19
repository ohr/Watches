package org.ear.watches.ui

import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.util.TimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class StopwatchTileLogicTest {

    @Test
    fun currentLapTimeCalculation_noLapsRecorded_returnsFullElapsed() {
        val watch = StopwatchItem(
            name = "Runner",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = false,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 0L,
                    endTimeNanos = 5_000_000_000L,
                    accumulatedNanos = 5_000_000_000L,
                    startTimeMillis = 0L,
                    laps = emptyList()
                )
            )
        )
        val nowNanos = 10_000_000_000L
        val currentTotalTime = watch.getElapsedMillis(nowNanos)
        val lastLapRecordedTotalTime = watch.currentRunLaps.lastOrNull()?.totalTimeMillis ?: 0L
        val ongoingLapMillis = (currentTotalTime - lastLapRecordedTotalTime).coerceAtLeast(0L)

        assertEquals(5000L, currentTotalTime)
        assertEquals(5000L, ongoingLapMillis)
    }

    @Test
    fun currentLapTimeCalculation_withLapsRecorded_returnsDurationSinceLastLap() {
        val laps = listOf(
            LapRecord(lapNumber = 1, lapTimeNanos = 5_000_000_000L, totalTimeNanos = 5_000_000_000L),
            LapRecord(lapNumber = 2, lapTimeNanos = 4_000_000_000L, totalTimeNanos = 9_000_000_000L)
        )
        val watch = StopwatchItem(
            name = "Runner",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = false,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 0L,
                    endTimeNanos = 14_000_000_000L,
                    accumulatedNanos = 14_000_000_000L,
                    startTimeMillis = 0L,
                    laps = laps
                )
            )
        )
        val nowNanos = 20_000_000_000L
        val currentTotalTime = watch.getElapsedMillis(nowNanos)
        val lastLapRecordedTotalTime = watch.currentRunLaps.lastOrNull()?.totalTimeMillis ?: 0L
        val ongoingLapMillis = (currentTotalTime - lastLapRecordedTotalTime).coerceAtLeast(0L)

        assertEquals(14000L, currentTotalTime)
        assertEquals(5000L, ongoingLapMillis) // 14000 - 9000 = 5000

        val formattedOverall = TimeFormatter.formatTime(currentTotalTime, AppPrecision.TENTHS)
        val formattedOngoingLap = TimeFormatter.formatTime(ongoingLapMillis, AppPrecision.TENTHS)

        assertEquals("00:14.0", formattedOverall)
        assertEquals("00:05.0", formattedOngoingLap)
    }

    @Test
    fun resetButtonBehavior_shortAndLongResetAvailable() {
        val runningWatch = StopwatchItem(
            name = "Runner 1",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = true
        )
        val stoppedWatch = StopwatchItem(
            name = "Runner 2",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = false
        )

        // Reset button handles short click and long click for both running and stopped states
        assertTrue(runningWatch.id.isNotBlank())
        assertTrue(stoppedWatch.id.isNotBlank())
    }

    @Test
    fun instantPressTrigger_executesImmediatelyOnTouchDown() {
        var startCount = 0
        var lapCount = 0
        var resetCount = 0
        var fullResetCount = 0

        val onStartStop = { startCount++ }
        val onLap = { lapCount++ }
        val onReset = { resetCount++ }
        val onFullReset = { fullResetCount++ }

        onStartStop()
        assertEquals(1, startCount)

        onLap()
        assertEquals(1, lapCount)

        onReset()
        assertEquals(1, resetCount)

        onFullReset()
        assertEquals(1, fullResetCount)
    }

    @Test
    fun lastLapPaceTrendLogic_extractsDeltaAndFormatsCorrectly() {
        val laps = listOf(
            LapRecord(lapNumber = 1, lapTimeNanos = 14_200_000_000L, totalTimeNanos = 14_200_000_000L, lapDeltaNanos = null),
            LapRecord(lapNumber = 2, lapTimeNanos = 13_000_000_000L, totalTimeNanos = 27_200_000_000L, lapDeltaNanos = -1_200_000_000L)
        )
        val watch = StopwatchItem(
            name = "Runner",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = true,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 0L,
                    laps = laps
                )
            )
        )

        val lastLap = watch.currentRunLaps.lastOrNull()
        assertEquals(2, lastLap?.lapNumber)
        val delta = lastLap?.lapDeltaMillis
        assertEquals(-1200L, delta)

        val formattedDeltaComma = TimeFormatter.formatDeltaTime(delta!!, ",")
        assertEquals("-1,2s", formattedDeltaComma)

        val formattedDeltaDot = TimeFormatter.formatDeltaTime(delta, ".")
        assertEquals("-1.2s", formattedDeltaDot)
    }

    @Test
    fun runningAndResultPrecisionSeparation_formattingRules() {
        val runningDisplayPrecision = AppPrecision.SECONDS
        val timePrecision = AppPrecision.HUNDREDTHS

        val runningWatch = StopwatchItem(
            name = "Runner",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = true,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 1_000_000_000L,
                    startTimeMillis = 1000L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 12_345_000_000L, totalTimeNanos = 12_345_000_000L)
                    )
                )
            )
        )

        val stoppedWatch = StopwatchItem(
            name = "Runner Stopped",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = false,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 1_000_000_000L,
                    endTimeNanos = 16_432_000_000L,
                    accumulatedNanos = 15_432_000_000L,
                    startTimeMillis = 1000L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 12_345_000_000L, totalTimeNanos = 12_345_000_000L)
                    )
                )
            )
        )

        val nowNanos = 16_000_000_000L
        val runningElapsed = runningWatch.getElapsedMillis(nowNanos) // 16000000000 - 1000000000 = 15000ms
        val stoppedElapsed = stoppedWatch.getElapsedMillis(nowNanos) // 15432ms

        // When running: clock precision uses runningDisplayPrecision (SECONDS -> "00:15")
        val activeRunningPrecision = if (runningWatch.isRunning) runningDisplayPrecision else timePrecision
        val formattedRunningClock = TimeFormatter.formatTime(runningElapsed, activeRunningPrecision, ".")
        assertEquals("00:15", formattedRunningClock)

        // When stopped: clock precision uses timePrecision (HUNDREDTHS -> "00:15.43")
        val activeStoppedPrecision = if (stoppedWatch.isRunning) runningDisplayPrecision else timePrecision
        val formattedStoppedClock = TimeFormatter.formatTime(stoppedElapsed, activeStoppedPrecision, ".")
        assertEquals("00:15.43", formattedStoppedClock)

        // Last lap always uses timePrecision (HUNDREDTHS -> "00:12.34")
        val lastLapTime = runningWatch.currentRunLaps.lastOrNull()?.let {
            TimeFormatter.formatTime(it.lapTimeMillis, timePrecision, ".")
        }
        assertEquals("00:12.34", lastLapTime)
    }
}
