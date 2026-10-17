package com.ear.watches.data.model

import androidx.compose.runtime.Stable

/**
 * The primary UI state holding the entire configuration and data payload for the app.
 * Contains the list of active stopwatches, user settings, the currently focused watch (if any),
 * and dialog visibility flags.
 */
@Stable
data class StopwatchUiState(
    val stopwatches: List<StopwatchItem> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val activeFocusedWatchId: String? = null,
    val showHelpDialog: Boolean = false,
    val showAboutDialog: Boolean = false
)
