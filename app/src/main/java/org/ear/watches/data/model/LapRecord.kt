package org.ear.watches.data.model

import androidx.compose.runtime.Stable

/**
 * Represents a recorded lap within a specific [RunSession].
 * Contains information about the lap number, its duration in nanoseconds, the total cumulative time
 * at the point of recording, and any delta compared to previous laps.
 */
@Stable
data class LapRecord(
    val lapNumber: Int,
    val lapTimeNanos: Long,
    val totalTimeNanos: Long,
    val lapDeltaNanos: Long? = null
) {
    val lapTimeMillis: Long get() = lapTimeNanos / 1_000_000L
    val totalTimeMillis: Long get() = totalTimeNanos / 1_000_000L
    val lapDeltaMillis: Long? get() = lapDeltaNanos?.let { it / 1_000_000L }
}
