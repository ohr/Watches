package org.ear.watches.data.model

import androidx.compose.runtime.Stable

/**
 * Represents a continuous recording period (run) of a stopwatch, starting when the stopwatch is resumed/started
 * and ending when it is paused or stopped. Holds all [LapRecord]s recorded during this period.
 */
@Stable
data class RunSession(
    val runIndex: Int,
    val startTimeNanos: Long = 0L,
    val endTimeNanos: Long = 0L,
    val accumulatedNanos: Long = 0L,
    val startTimeMillis: Long = 0L,
    val laps: List<LapRecord> = emptyList()
) {
    val accumulatedMillis: Long get() = accumulatedNanos / 1_000_000L
}
