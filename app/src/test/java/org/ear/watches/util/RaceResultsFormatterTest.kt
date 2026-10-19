package org.ear.watches.util

import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.junit.jupiter.api.Assertions.assertEquals
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
                    startTimeNanos = 0L,
                    endTimeNanos = 50_000_000_000L,
                    accumulatedNanos = 50_000_000_000L,
                    startTimeMillis = 0L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 20_000_000_000L, totalTimeNanos = 20_000_000_000L),
                        LapRecord(lapNumber = 2, lapTimeNanos = 30_000_000_000L, totalTimeNanos = 50_000_000_000L)
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
            nowNanos = 100_000_000_000L
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
                    startTimeNanos = 0L,
                    endTimeNanos = 50_000_000_000L,
                    accumulatedNanos = 50_000_000_000L,
                    startTimeMillis = 0L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 20_450_000_000L, totalTimeNanos = 20_450_000_000L),
                        LapRecord(lapNumber = 2, lapTimeNanos = 29_550_000_000L, totalTimeNanos = 50_000_000_000L)
                    )
                )
            )
        )

        val report = RaceResultsFormatter.formatRaceResults(
            stopwatches = listOf(item),
            precision = AppPrecision.HUNDREDTHS,
            nowNanos = 100_000_000_000L,
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
            startTimeNanos = 1_000_000_000L,
            endTimeNanos = 20_000_000_000L,
            accumulatedNanos = 19_000_000_000L,
            startTimeMillis = 1000L,
            laps = listOf(
                LapRecord(lapNumber = 1, lapTimeNanos = 19_000_000_000L, totalTimeNanos = 19_000_000_000L)
            )
        )
        val run2 = RunSession(
            runIndex = 2,
            startTimeNanos = 25_000_000_000L,
            endTimeNanos = 55_000_000_000L,
            accumulatedNanos = 30_000_000_000L,
            startTimeMillis = 25000L,
            laps = listOf(
                LapRecord(lapNumber = 1, lapTimeNanos = 10_000_000_000L, totalTimeNanos = 10_000_000_000L),
                LapRecord(lapNumber = 2, lapTimeNanos = 20_000_000_000L, totalTimeNanos = 30_000_000_000L)
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
            nowNanos = 60_000_000_000L
        )

        assertTrue(report.contains("Runner: Sprint Champion"))
        assertTrue(report.contains("Total (All Runs): 00:49.00"))
        assertTrue(report.contains("Run 1"))
        assertTrue(report.contains("Run 2"))
        assertTrue(report.contains("Best Lap: 00:19.00"))
        assertTrue(report.contains("Best Lap: 00:10.00"))
    }

    @Test
    fun formatAsCsv_emptyList_returnsHeaderOnly() {
        val csv = RaceResultsFormatter.formatAsCsv(emptyList())
        assertEquals("\"Athlete\",\"Run Session\",\"Lap Number\",\"Lap Time\",\"Total Time\"", csv)
    }

    @Test
    fun formatSingleAsCsv_singleStopwatch_formatsLapsCorrectly() {
        val item = StopwatchItem(
            name = "Athlete Alpha",
            pastelColorHex = 0xFFEF9A9AL,
            isRunning = false,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 1_000_000_000L,
                    endTimeNanos = 51_000_000_000L,
                    accumulatedNanos = 50_000_000_000L,
                    startTimeMillis = 1000L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 20_000_000_000L, totalTimeNanos = 20_000_000_000L),
                        LapRecord(lapNumber = 2, lapTimeNanos = 30_000_000_000L, totalTimeNanos = 50_000_000_000L)
                    )
                )
            )
        )

        val csv = RaceResultsFormatter.formatSingleAsCsv(item, precision = AppPrecision.HUNDREDTHS)
        val expected = listOf(
            "\"Athlete\",\"Run Session\",\"Lap Number\",\"Lap Time\",\"Total Time\"",
            "\"Athlete Alpha\",\"Run 1\",\"1\",\"00:20.00\",\"00:20.00\"",
            "\"Athlete Alpha\",\"Run 1\",\"2\",\"00:30.00\",\"00:50.00\""
        ).joinToString("\n")

        assertEquals(expected, csv)
    }

    @Test
    fun formatAsCsv_multipleStopwatches_formatsAllLaps() {
        val item1 = StopwatchItem(
            name = "Athlete Alpha",
            pastelColorHex = 0xFFEF9A9AL,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 0L,
                    endTimeNanos = 15_000_000_000L,
                    accumulatedNanos = 15_000_000_000L,
                    startTimeMillis = 0L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 15_000_000_000L, totalTimeNanos = 15_000_000_000L)
                    )
                )
            )
        )

        val item2 = StopwatchItem(
            name = "Athlete Beta",
            pastelColorHex = 0xFF90CAF9L,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 0L,
                    endTimeNanos = 26_000_000_000L,
                    accumulatedNanos = 26_000_000_000L,
                    startTimeMillis = 0L,
                    laps = listOf(
                        LapRecord(lapNumber = 1, lapTimeNanos = 12_000_000_000L, totalTimeNanos = 12_000_000_000L),
                        LapRecord(lapNumber = 2, lapTimeNanos = 14_000_000_000L, totalTimeNanos = 26_000_000_000L)
                    )
                )
            )
        )

        val csv = RaceResultsFormatter.formatAsCsv(listOf(item1, item2), precision = AppPrecision.HUNDREDTHS)
        val expected = listOf(
            "\"Athlete\",\"Run Session\",\"Lap Number\",\"Lap Time\",\"Total Time\"",
            "\"Athlete Alpha\",\"Run 1\",\"1\",\"00:15.00\",\"00:15.00\"",
            "\"Athlete Beta\",\"Run 1\",\"1\",\"00:12.00\",\"00:12.00\"",
            "\"Athlete Beta\",\"Run 1\",\"2\",\"00:14.00\",\"00:26.00\""
        ).joinToString("\n")

        assertEquals(expected, csv)
    }
}
