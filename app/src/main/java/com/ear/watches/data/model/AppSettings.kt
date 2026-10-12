package com.ear.watches.data.model

data class AppSettings(
    val soundAndHapticsEnabled: Boolean = true,
    val hardwareVolumeKeysEnabled: Boolean = true,
    val timePrecision: AppPrecision = AppPrecision.TENTHS,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val autoSortEnabled: Boolean = false
)
