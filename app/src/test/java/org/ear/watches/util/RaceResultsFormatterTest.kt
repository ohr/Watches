package org.ear.watches.util

import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RaceResultsFormatterTest {

    @Test
    fun formatRaceResults_emptyList() {
        val result = RaceResultsFormatter.formatRaceResults(emptyList())
        assertTrue(result.contains("No stopwatch data available"))
    }

    @Test
    fun formatRaceResults_withLapsAndRunners() {
        val item1 = StopwatchItem(
            name = "Athlete Alpha",
            pastelColorHex = 0xFFEF9A9AL,
            isRunning = false,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeMillis = 0L,
                    endTimeMillis = 50000L,
                    accumulatedMillis = 50000L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeMillis = 20000L, totalTimeMillis = 20000L),
                        LapRecord(lapNumber = 2, lapTimeMillis = 30000L, totalTimeMillis = 50000L)
                    )
                )
            )
        )

        val item2 = StopwatchItem(
            name = "Athlete Beta",
            pastelColorHex = 0xFF90CAF9L,
            isRunning = false,
            runs = emptyList()
        )

        val report = RaceResultsFormatter.formatRaceResults(
            stopwatches = listOf(item1, item2),
            precision = AppPrecision.HUNDREDTHS,
            nowMillis = 100000L
        )

        assertTrue(report.contains("Runner: Athlete Alpha"))
        assertTrue(report.contains("Total (All Runs): 00:50.00"))
        assertTrue(report.contains("Best Lap: 00:20.00 (Lap 1)"))
        assertTrue(report.contains("Lap 1: 00:20.00 | Cumulative: 00:20.00"))
        assertTrue(report.contains("Lap 2: 00:30.00 | Cumulative: 00:50.00"))

        assertTrue(report.contains("Runner: Athlete Beta"))
        assertTrue(report.contains("Best Lap: N/A"))
        assertTrue(report.contains("Laps: None recorded"))
    }

    @Test
    fun formatRaceResults_withGermanSubsecondSeparator() {
        val item = StopwatchItem(
            name = "Athlete German",
            pastelColorHex = 0xFFEF9A9AL,
            isRunning = false,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeMillis = 0L,
                    endTimeMillis = 50000L,
                    accumulatedMillis = 50000L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeMillis = 20450L, totalTimeMillis = 20450L),
                        LapRecord(lapNumber = 2, lapTimeMillis = 29550L, totalTimeMillis = 50000L)
                    )
                )
            )
        )

        val report = RaceResultsFormatter.formatRaceResults(
            stopwatches = listOf(item),
            precision = AppPrecision.HUNDREDTHS,
            nowMillis = 100000L,
            subsecondSeparator = ","
        )

        assertTrue(report.contains("Runner: Athlete German"))
        assertTrue(report.contains("Total (All Runs): 00:50,00"))
        assertTrue(report.contains("Best Lap: 00:20,45 (Lap 1)"))
        assertTrue(report.contains("Lap 1: 00:20,45 | Cumulative: 00:20,45"))
        assertTrue(report.contains("Lap 2: 00:29,55 | Cumulative: 00:50,00"))
    }

    @Test
    fun formatRaceResults_withMultiRunSessions() {
        val run1 = RunSession(
            runIndex = 1,
            startTimeMillis = 1000L,
            endTimeMillis = 20000L,
            accumulatedMillis = 19000L,
            laps = listOf(
                LapRecord(lapNumber = 1, lapTimeMillis = 19000L, totalTimeMillis = 19000L)
            )
        )
        val run2 = RunSession(
            runIndex = 2,
            startTimeMillis = 25000L,
            endTimeMillis = 55000L,
            accumulatedMillis = 30000L,
            laps = listOf(
                LapRecord(lapNumber = 1, lapTimeMillis = 10000L, totalTimeMillis = 10000L),
                LapRecord(lapNumber = 2, lapTimeMillis = 20000L, totalTimeMillis = 30000L)
            )
        )

        val item = StopwatchItem(
            name = "Sprint Champion",
            pastelColorHex = 0xFFEF9A9AL,
            runs = listOf(run1, run2)
        )

        val report = RaceResultsFormatter.formatRaceResults(
            stopwatches = listOf(item),
            precision = AppPrecision.HUNDREDTHS,
            nowMillis = 60000L
        )

        assertTrue(report.contains("Runner: Sprint Champion"))
        assertTrue(report.contains("Total (All Runs): 00:49.00"))
        assertTrue(report.contains("Run 1"))
        assertTrue(report.contains("Run 2"))
        assertTrue(report.contains("Best Lap: 00:19.00"))
        assertTrue(report.contains("Best Lap: 00:10.00"))
    }
}
