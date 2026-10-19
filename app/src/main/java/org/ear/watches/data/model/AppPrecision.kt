package org.ear.watches.data.model

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the formatting precision used for displaying stopwatch times.
 * Controls how many decimal digits of a second are shown (e.g., tenths, hundredths, or none).
 */
@Serializable
enum class AppPrecision(
    val displayName: String,
    val decimalDigits: Int,
    val refreshMillis: Long,
) {
    TENTHS("0.1s", 1, 50L),
    HUNDREDTHS("0.01s", 2, 16L),
    SECONDS("1s", 0, 500L);

    suspend fun delayRefresh() {
        delay(refreshMillis.milliseconds)
    }

}
