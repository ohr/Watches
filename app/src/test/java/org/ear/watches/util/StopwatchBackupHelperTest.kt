package org.ear.watches.util

import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchBackupPayload
import org.ear.watches.data.model.StopwatchItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class StopwatchBackupHelperTest {

    @Test
    fun createBackupJson_serializesNormalizedStoppedStopwatches() {
        val watch = StopwatchItem(
            name = "Runner Alpha",
            pastelColorHex = 0xFFEF9A9AL,
            isRunning = true,
            isFocused = true,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = 1_000_000_000L,
                    endTimeNanos = 0L,
                    accumulatedNanos = 5_000_000_000L,
                    startTimeMillis = 1700000000000L,
                    laps = listOf(
                        LapRecord(
                            lapNumber = 1,
                            lapTimeNanos = 2_000_000_000L,
                            totalTimeNanos = 2_000_000_000L
                        )
                    ),
                    name = "Heat 1"
                )
            )
        )

        val jsonString = StopwatchBackupHelper.createBackupJson(
            stopwatches = listOf(watch),
            appVersion = "1.0.0",
            exportedAtMillis = 1700000005000L
        )

        assertTrue(jsonString.contains("Runner Alpha"))
        assertTrue(jsonString.contains("Heat 1"))
        assertTrue(jsonString.contains("\"version\": 1"))
        assertTrue(jsonString.contains("\"appVersion\": \"1.0.0\""))

        val parseResult = StopwatchBackupHelper.parseBackupJson(jsonString)
        assertTrue(parseResult.isSuccess)
        val payload = parseResult.getOrThrow()

        assertEquals(1, payload.version)
        assertEquals("1.0.0", payload.appVersion)
        assertEquals(1, payload.stopwatches.size)

        val restoredWatch = payload.stopwatches[0]
        assertEquals("Runner Alpha", restoredWatch.name)
        assertEquals(0xFFEF9A9AL, restoredWatch.pastelColorHex)
        assertFalse(restoredWatch.isRunning)
        assertFalse(restoredWatch.isFocused)
        assertEquals(1, restoredWatch.runs.size)

        val restoredRun = restoredWatch.runs[0]
        assertEquals(1, restoredRun.runIndex)
        assertEquals("Heat 1", restoredRun.name)
        assertEquals(1700000000000L, restoredRun.startTimeMillis)
        assertEquals(5_000_000_000L, restoredRun.accumulatedNanos)
        assertFalse(restoredRun.isRunning)
        assertEquals(1, restoredRun.laps.size)
        assertEquals(1, restoredRun.laps[0].lapNumber)
        assertEquals(2_000_000_000L, restoredRun.laps[0].lapTimeNanos)
    }

    @Test
    fun parseBackupJson_unsupportedVersion_fails() {
        val futureJson = """
            {
                "version": 999,
                "exportedAtMillis": 1700000000000,
                "appVersion": "99.0.0",
                "stopwatches": []
            }
        """.trimIndent()

        val result = StopwatchBackupHelper.parseBackupJson(futureJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup version") == true)
    }

    @Test
    fun parseBackupJson_missingStopwatchesField_fails() {
        val arbitraryJson = """
            {
                "user": "alice",
                "settings": { "darkTheme": true }
            }
        """.trimIndent()
        val result = StopwatchBackupHelper.parseBackupJson(arbitraryJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("missing stopwatches definition") == true)
    }

    @Test
    fun parseBackupJson_malformedJson_fails() {
        val badJson = "{ invalid json content }"
        val result = StopwatchBackupHelper.parseBackupJson(badJson)
        assertTrue(result.isFailure)
    }

    @Test
    fun roundtripBackup_multipleStopwatchesWithLapsAndSessions() {
        val watch1 = StopwatchItem(
            name = "Athlete 1",
            pastelColorHex = 0xFFEF9A9AL,
            runs = listOf(
                RunSession(
                    runIndex = 1,
                    accumulatedNanos = 10_000_000_000L,
                    startTimeMillis = 1700000000000L,
                    laps = listOf(
                        LapRecord(1, 4_000_000_000L, 4_000_000_000L, null),
                        LapRecord(2, 6_000_000_000L, 10_000_000_000L, 2_000_000_000L)
                    )
                )
            )
        )
        val watch2 = StopwatchItem(
            name = "Athlete 2",
            pastelColorHex = 0xFF90CAF9L,
            runs = emptyList()
        )

        val json = StopwatchBackupHelper.createBackupJson(listOf(watch1, watch2))
        val parsed = StopwatchBackupHelper.parseBackupJson(json).getOrThrow()

        assertEquals(2, parsed.stopwatches.size)
        assertEquals("Athlete 1", parsed.stopwatches[0].name)
        assertEquals(2, parsed.stopwatches[0].runs[0].laps.size)
        assertEquals(2_000_000_000L, parsed.stopwatches[0].runs[0].laps[1].lapDeltaNanos)
        assertEquals("Athlete 2", parsed.stopwatches[1].name)
        assertTrue(parsed.stopwatches[1].runs.isEmpty())
    }

    @Test
    fun parseBackupJson_excessiveStopwatchCount_fails() {
        val manyWatches = (1..501).map {
            StopwatchItem(name = "Runner $it", pastelColorHex = 0xFFEF9A9AL)
        }
        val payload = StopwatchBackupPayload(stopwatches = manyWatches)
        val json = StopwatchBackupHelper.json.encodeToString(StopwatchBackupPayload.serializer(), payload)

        val result = StopwatchBackupHelper.parseBackupJson(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("too many stopwatches") == true)
    }

    @Test
    fun parseBackupJson_sanitizesControlCharactersAndTruncatesExcessiveLength() {
        val longName = "A".repeat(200) + "\u0000\u0007\u001B"
        val payload = StopwatchBackupPayload(
            stopwatches = listOf(
                StopwatchItem(
                    name = longName,
                    pastelColorHex = 0x00123456L, // Missing alpha channel
                    runs = listOf(
                        RunSession(
                            runIndex = -5,
                            accumulatedNanos = -100L,
                            startTimeMillis = -50L,
                            name = "Run" + "\u0000" + "Session",
                            laps = listOf(
                                LapRecord(
                                    lapNumber = -1,
                                    lapTimeNanos = -500L,
                                    totalTimeNanos = -1000L,
                                    lapDeltaNanos = -9999L
                                )
                            )
                        )
                    )
                )
            )
        )
        val json = StopwatchBackupHelper.json.encodeToString(StopwatchBackupPayload.serializer(), payload)
        val parsed = StopwatchBackupHelper.parseBackupJson(json).getOrThrow()
        val watch = parsed.stopwatches[0]

        // 1. Name truncated to MAX_STRING_LENGTH (120) and stripped of control characters
        assertEquals(120, watch.name.length)
        assertFalse(watch.name.contains("\u0000"))
        assertFalse(watch.name.contains("\u0007"))

        // 2. Alpha channel enforced to 0xFF
        assertEquals(0xFF123456L, watch.pastelColorHex)

        // 3. Run indices and times clamped to non-negative
        val run = watch.runs[0]
        assertEquals(1, run.runIndex)
        assertEquals(0L, run.accumulatedNanos)
        assertEquals(0L, run.startTimeMillis)
        assertEquals("RunSession", run.name)

        // 4. Laps clamped to non-negative
        val lap = run.laps[0]
        assertEquals(1, lap.lapNumber)
        assertEquals(0L, lap.lapTimeNanos)
        assertEquals(0L, lap.totalTimeNanos)
    }

    @Test
    fun parseBackupJson_blankName_fallsBackToAthlete() {
        val payload = StopwatchBackupPayload(
            stopwatches = listOf(
                StopwatchItem(name = "   \n\t  ", pastelColorHex = 0xFFEF9A9AL)
            )
        )
        val json = StopwatchBackupHelper.json.encodeToString(StopwatchBackupPayload.serializer(), payload)
        val parsed = StopwatchBackupHelper.parseBackupJson(json).getOrThrow()

        assertEquals("Athlete 1", parsed.stopwatches[0].name)
    }
}
