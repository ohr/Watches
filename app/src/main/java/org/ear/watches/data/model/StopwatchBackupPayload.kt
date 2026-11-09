package org.ear.watches.data.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Encapsulates a versioned backup payload of stopwatches for export and import.
 *
 * @property version Schema version of the backup format.
 * @property exportedAtMillis Wall-clock timestamp (System.currentTimeMillis()) when the backup was created.
 * @property appVersion Optional application version string when the backup was exported.
 * @property stopwatches List of [StopwatchItem] instances saved in this backup.
 */
@Stable
@Serializable
data class StopwatchBackupPayload(
    val version: Int = CURRENT_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val appVersion: String? = null,
    val stopwatches: List<StopwatchItem> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val FILE_SUFFIX = ".watches.json"
    }
}
