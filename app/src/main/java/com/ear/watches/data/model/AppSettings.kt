package com.ear.watches.data.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * User-configurable settings for the application.
 * Controls preferences like haptics, volume key behavior, display precision, language, and theme.
 * Designed to be serialized and persisted using DataStore.
 */
@Stable
@Serializable
data class AppSettings(
    val soundAndHapticsEnabled: Boolean = true,
    val hardwareVolumeKeysEnabled: Boolean = true,
    val runningDisplayPrecision: AppPrecision = AppPrecision.TENTHS,
    val timePrecision: AppPrecision = AppPrecision.TENTHS,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val autoSortEnabled: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM_DEFAULT,
    val fontSizeScale: AppFontSize = AppFontSize.NORMAL
)
