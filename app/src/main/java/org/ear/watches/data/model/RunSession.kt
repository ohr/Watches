package org.ear.watches.data.model

import androidx.compose.runtime.Stable

/**
 * Represents a continuous recording period (run) of a stopwatch, starting when the stopwatch is resumed/started
 * and ending when it is paused or stopped. Holds all [LapRecord]s recorded during this period.
 */
@Stable
data class RunSession(
    val runIndex: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long = 0L,
    val accumulatedMillis: Long = 0L,
    val laps: List<LapRecord> = emptyList()
)
