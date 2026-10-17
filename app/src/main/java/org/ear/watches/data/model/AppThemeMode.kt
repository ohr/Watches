package org.ear.watches.data.model

import kotlinx.serialization.Serializable

/**
 * Determines the visual theme of the application (Light, Dark, or System Default).
 */
@Serializable
enum class AppThemeMode {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}
