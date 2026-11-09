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
        // Zero-lap watches first, then by absolute expected next-lap time so that
        // watches started at different times are compared on the same timeline.
        compareBy { watch -> watch.calculateExpectedNextLapAtNanos() ?: Long.MIN_VALUE }
    ),
    BY_NAME(
        "by_name",
        compareBy(String.CASE_INSENSITIVE_ORDER) { watch -> watch.name }
    );

    fun sort(stopwatches: List<StopwatchItem>): List<StopwatchItem> {
        return comparator?.let { stopwatches.sortedWith(it) } ?: stopwatches
    }
}

