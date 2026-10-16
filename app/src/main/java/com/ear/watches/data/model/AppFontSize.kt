package com.ear.watches.data.model

import kotlinx.serialization.Serializable

/**
 * Represents the user's preferred font scaling factor for the application UI.
 */
@Serializable
enum class AppFontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    NORMAL(1.0f),
    LARGE(1.15f)
}
