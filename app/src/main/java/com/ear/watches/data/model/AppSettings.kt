package com.ear.watches.data.model

import kotlinx.serialization.Serializable

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
