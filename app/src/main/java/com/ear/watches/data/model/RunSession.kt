package com.ear.watches.data.model

import androidx.compose.runtime.Stable

@Stable
data class RunSession(
    val runIndex: Int,
    val startTimeMillis: Long,
    val endTimeMillis: Long = 0L,
    val accumulatedMillis: Long = 0L,
    val laps: List<LapRecord> = emptyList()
)
