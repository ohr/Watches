package com.ear.watches.data.model

import androidx.compose.runtime.Stable

/**
 * Represents a recorded lap within a specific [RunSession].
 * Contains information about the lap number, its duration, the total cumulative time at the
 * point of recording, and any delta compared to previous laps.
 */
@Stable
data class LapRecord(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long,
    val lapDeltaMillis: Long? = null
)
