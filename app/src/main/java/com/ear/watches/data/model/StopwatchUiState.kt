package com.ear.watches.data.model

import androidx.compose.runtime.Stable

@Stable
data class StopwatchUiState(
    val stopwatches: List<StopwatchItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val activeFocusedWatchId: String? = null,
    val showHelpDialog: Boolean = false
)
