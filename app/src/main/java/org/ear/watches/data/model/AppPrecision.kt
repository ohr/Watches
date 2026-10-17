package org.ear.watches.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the formatting precision used for displaying stopwatch times.
 * Controls how many decimal digits of a second are shown (e.g., tenths, hundredths, or none).
 */
@Serializable
enum class AppPrecision(
    val displayName: String,
    val decimalDigits: Int
) {
    TENTHS("0.1s", 1),
    HUNDREDTHS("0.01s", 2),
    SECONDS("1s", 0)
}
