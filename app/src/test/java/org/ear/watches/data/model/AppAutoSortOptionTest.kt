package org.ear.watches.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class AppAutoSortOptionTest {

    @Test
    fun sort_none_returnsStopwatchesAsIs() {
        val w1 = StopwatchItem(name = "Charlie", pastelColorHex = 1L)
        val w2 = StopwatchItem(name = "Alice", pastelColorHex = 2L)
        val w3 = StopwatchItem(name = "Bob", pastelColorHex = 3L)

        val input = listOf(w1, w2, w3)
        val sorted = AppAutoSortOption.NONE.sort(input)

        assertEquals(listOf("Charlie", "Alice", "Bob"), sorted.map { it.name })
    }

    @Test
    fun sort_byFastestLap_ordersZeroLapsFirstThenExpectedNextLapTime() {
        val w1 = StopwatchItem(name = "Slow Lap", pastelColorHex = 1L)
            .start(0L, 0L).lap(10_000_000_000L)
        val w2 = StopwatchItem(name = "Fast Lap", pastelColorHex = 2L)
            .start(0L, 0L).lap(4_000_000_000L)
        val w3 = StopwatchItem(name = "Zero Laps", pastelColorHex = 3L)
            .start(0L, 0L)

        val input = listOf(w1, w2, w3)
        val sorted = AppAutoSortOption.BY_FASTEST_LAP.sort(input)

        assertEquals(listOf("Zero Laps", "Fast Lap", "Slow Lap"), sorted.map { it.name })
    }

    @Test
    fun sort_byName_sortsAlphabeticallyCaseInsensitive() {
        val w1 = StopwatchItem(name = "charlie", pastelColorHex = 1L)
        val w2 = StopwatchItem(name = "Alice", pastelColorHex = 2L)
        val w3 = StopwatchItem(name = "bob", pastelColorHex = 3L)

        val input = listOf(w1, w2, w3)
        val sorted = AppAutoSortOption.BY_NAME.sort(input)

        assertEquals(listOf("Alice", "bob", "charlie"), sorted.map { it.name })
    }
}
