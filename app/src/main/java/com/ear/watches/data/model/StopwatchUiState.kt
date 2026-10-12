package com.ear.watches.data.model

data class StopwatchUiState(
    val stopwatches: List<StopwatchItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val activeFocusedWatchId: String? = null,
    val lastTickMillis: Long = System.currentTimeMillis(),
    val showHelpDialog: Boolean = false
)
