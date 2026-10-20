package org.ear.watches.data.model

import kotlinx.serialization.Serializable

/**
 * Defines the available tile sorting strategies for the stopwatch grid.
 */
@Serializable
enum class AppAutoSortOption(
    val code: String,
    val comparator: Comparator<StopwatchItem>? = null
) {
    NONE("none"),
    BY_FASTEST_LAP(
        "fastest_lap",
        compareBy { watch ->
            if (watch.currentRunLaps.isEmpty()) 0L
            else watch.calculateExpectedNextLapTime()
        }
    ),
    BY_NAME(
        "by_name",
        compareBy(String.CASE_INSENSITIVE_ORDER) { watch -> watch.name }
    );

    fun sort(stopwatches: List<StopwatchItem>): List<StopwatchItem> {
        return comparator?.let { stopwatches.sortedWith(it) } ?: stopwatches
    }
}

