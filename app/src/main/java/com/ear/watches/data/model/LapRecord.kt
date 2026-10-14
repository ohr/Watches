package com.ear.watches.data.model

import androidx.compose.runtime.Stable

@Stable
data class LapRecord(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long,
    val lapDeltaMillis: Long? = null
)
