package org.ear.watches.service

import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.model.StopwatchUiState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class RunningNotificationModelTest {

    private val nowNanos = 20_000_000_000L
    private val nowMillis = 1_000_000L

    @Test
    fun noRunningWatch_returnsNull() {
        val idle = StopwatchItem(name = "A", pastelColorHex = 1L)
        val paused = StopwatchItem(name = "B", pastelColorHex = 2L).start(1_000L, 1L).pause(2_000L)
        val state = StopwatchUiState(stopwatches = listOf(idle, paused))

        assertNull(RunningNotificationModel.from(state, nowNanos, nowMillis))
    }

    @Test
    fun targetsFocusedRunningWatch_withChronometerBaseAndLastLap() {
        val anna = StopwatchItem(name = "Anna", pastelColorHex = 1L)
            .start(nowNanos = 5_000_000_000L, nowMillis = 1L)
            .lap(nowNanos = 12_000_000_000L)
        val ben = StopwatchItem(name = "Ben", pastelColorHex = 2L).start(5_000_000_000L, 1L)
        val idle = StopwatchItem(name = "Idle", pastelColorHex = 3L)
        val state = StopwatchUiState(stopwatches = listOf(anna, ben, idle), activeFocusedWatchId = anna.id)

        val model = RunningNotificationModel.from(state, nowNanos, nowMillis)!!

        assertEquals(anna.id, model.targetWatchId)
        assertEquals("Anna", model.targetName)
        assertEquals(2, model.runningCount)
        assertEquals(3, model.totalCount)
        // Anna has been running for 15s -> chronometer base 15s before "now".
        assertEquals(nowMillis - 15_000L, model.chronometerBaseMillis)
        assertEquals(1, model.lastLapNumber)
        assertEquals(7_000L, model.lastLapMillis)
    }

    @Test
    fun focusedWatchNotRunning_fallsBackToRunningWatch() {
        val stopped = StopwatchItem(name = "Stopped", pastelColorHex = 1L)
        val running = StopwatchItem(name = "Running", pastelColorHex = 2L).start(5_000_000_000L, 1L)
        val state = StopwatchUiState(stopwatches = listOf(stopped, running), activeFocusedWatchId = stopped.id)

        val model = RunningNotificationModel.from(state, nowNanos, nowMillis)!!

        assertEquals(running.id, model.targetWatchId)
        assertNull(model.lastLapNumber)
    }

    @Test
    fun resolveTargetWatchId_prefersPredictedNextRunningWatchOverFirstRunning() {
        // Both running with one lap; "Second" is expected to lap sooner (absolute time).
        val first = StopwatchItem(name = "First", pastelColorHex = 1L)
            .start(1_000_000_000L, 1L).lap(11_000_000_000L)
        val second = StopwatchItem(name = "Second", pastelColorHex = 2L)
            .start(1_000_000_000L, 1L).lap(5_000_000_000L)
        val stopped = StopwatchItem(name = "Stopped", pastelColorHex = 3L)

        val target = StopwatchItem.resolveTargetWatchId(listOf(first, second, stopped), stopped.id)

        assertEquals(second.id, target)
    }
}
