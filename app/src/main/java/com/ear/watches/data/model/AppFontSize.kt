package com.ear.watches.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppFontSize(val scaleFactor: Float) {
    SMALL(0.85f),
    NORMAL(1.0f),
    LARGE(1.15f)
}
