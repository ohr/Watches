package com.ear.watches.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppPrecision(
    val displayName: String,
    val decimalDigits: Int
) {
    TENTHS("0.1s", 1),
    HUNDREDTHS("0.01s", 2),
    SECONDS("1s", 0)
}
