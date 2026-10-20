package org.ear.watches.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Repository layer for persisting and retrieving user preferences using DataStore.
 * Provides a reactive [Flow] of [AppSettings] to the UI and methods to update individual
 * configuration values.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val APP_SETTINGS_JSON = stringPreferencesKey("app_settings_json")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val LEGACY_SOUND_HAPTICS_ENABLED = booleanPreferencesKey("sound_haptics_enabled")
        val HARDWARE_KEYS_ENABLED = booleanPreferencesKey("hardware_keys_enabled")
        val RUNNING_DISPLAY_PRECISION = stringPreferencesKey("running_display_precision")
        val TIME_PRECISION = stringPreferencesKey("time_precision")
        val AUTO_SORT_OPTION = stringPreferencesKey("auto_sort_option")
        val AUTO_SORT_ENABLED = booleanPreferencesKey("auto_sort_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEEP_SCREEN_ON_WHILE_RUNNING = booleanPreferencesKey("keep_screen_on_while_running")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val jsonString = preferences[APP_SETTINGS_JSON]
            if (jsonString != null) {
                try {
                    val decoded = json.decodeFromString<AppSettings>(jsonString)
                    val legacySoundHaptics = preferences[LEGACY_SOUND_HAPTICS_ENABLED]
                    val sound = if (jsonString.contains("soundEnabled")) {
                        decoded.soundEnabled
                    } else {
                        preferences[SOUND_ENABLED] ?: legacySoundHaptics ?: decoded.soundEnabled
                    }
                    val haptics = if (jsonString.contains("hapticsEnabled")) {
                        decoded.hapticsEnabled
                    } else {
                        preferences[HAPTICS_ENABLED] ?: legacySoundHaptics ?: decoded.hapticsEnabled
                    }
                    val keepScreenOn = if (jsonString.contains("keepScreenOnWhileRunning")) {
                        decoded.keepScreenOnWhileRunning
                    } else {
                        preferences[KEEP_SCREEN_ON_WHILE_RUNNING] ?: decoded.keepScreenOnWhileRunning
                    }
                    val dynamicColor = if (jsonString.contains("useDynamicColor")) {
                        decoded.useDynamicColor
                    } else {
                        preferences[USE_DYNAMIC_COLOR] ?: decoded.useDynamicColor
                    }
                    val autoSortOption = if (jsonString.contains("autoSortOption")) {
                        decoded.autoSortOption
                    } else if (jsonString.contains("\"autoSortEnabled\":true")) {
                        AppAutoSortOption.BY_FASTEST_LAP
                    } else {
                        val optionStr = preferences[AUTO_SORT_OPTION]
                        val legacyAutoSort = preferences[AUTO_SORT_ENABLED]
                        if (optionStr != null) {
                            AppAutoSortOption.entries.find { it.code == optionStr || it.name == optionStr } ?: AppAutoSortOption.NONE
                        } else if (legacyAutoSort == true) {
                            AppAutoSortOption.BY_FASTEST_LAP
                        } else {
                            decoded.autoSortOption
                        }
                    }
                    decoded.copy(
                        soundEnabled = sound,
                        hapticsEnabled = haptics,
                        keepScreenOnWhileRunning = keepScreenOn,
                        useDynamicColor = dynamicColor,
                        autoSortOption = autoSortOption
                    )
                } catch (_: Exception) {
                    fallbackFromPreferences(preferences)
                }
            } else {
                fallbackFromPreferences(preferences)
            }
        }

    private fun fallbackFromPreferences(preferences: Preferences): AppSettings {
        val legacySoundHaptics = preferences[LEGACY_SOUND_HAPTICS_ENABLED]
        val sound = preferences[SOUND_ENABLED] ?: legacySoundHaptics ?: true
        val haptics = preferences[HAPTICS_ENABLED] ?: legacySoundHaptics ?: true
        val hardwareKeys = preferences[HARDWARE_KEYS_ENABLED] ?: true
        val runningPrecisionStr = preferences[RUNNING_DISPLAY_PRECISION]
        val runningPrecision = runningPrecisionStr?.let { str ->
            AppPrecision.entries.find { it.name == str }
        } ?: AppPrecision.TENTHS
        val precisionStr = preferences[TIME_PRECISION]
        val precision = precisionStr?.let { str ->
            AppPrecision.entries.find { it.name == str }
        } ?: AppPrecision.TENTHS
        val autoSortOptionStr = preferences[AUTO_SORT_OPTION]
        val autoSortOption = if (autoSortOptionStr != null) {
            AppAutoSortOption.entries.find { it.code == autoSortOptionStr || it.name == autoSortOptionStr } ?: AppAutoSortOption.NONE
        } else {
            val legacyAutoSort = preferences[AUTO_SORT_ENABLED] ?: false
            if (legacyAutoSort) AppAutoSortOption.BY_FASTEST_LAP else AppAutoSortOption.NONE
        }
        val languageStr = preferences[APP_LANGUAGE]
        val language = languageStr?.let { str ->
            AppLanguage.entries.find { it.name == str }
        } ?: AppLanguage.SYSTEM
        val keepScreenOn = preferences[KEEP_SCREEN_ON_WHILE_RUNNING] ?: true
        val dynamicColor = preferences[USE_DYNAMIC_COLOR] ?: true

        return AppSettings(
            soundEnabled = sound,
            hapticsEnabled = haptics,
            hardwareVolumeKeysEnabled = hardwareKeys,
            runningDisplayPrecision = runningPrecision,
            timePrecision = precision,
            language = language,
            autoSortOption = autoSortOption,
            themeMode = AppThemeMode.SYSTEM_DEFAULT,
            fontSizeScale = AppFontSize.NORMAL,
            keepScreenOnWhileRunning = keepScreenOn,
            useDynamicColor = dynamicColor
        )
    }

    private suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val currentJson = preferences[APP_SETTINGS_JSON]
            val currentSettings = if (currentJson != null) {
                try {
                    val decoded = json.decodeFromString<AppSettings>(currentJson)
                    val legacySoundHaptics = preferences[LEGACY_SOUND_HAPTICS_ENABLED]
                    val sound = if (currentJson.contains("soundEnabled")) {
                        decoded.soundEnabled
                    } else {
                        preferences[SOUND_ENABLED] ?: legacySoundHaptics ?: decoded.soundEnabled
                    }
                    val haptics = if (currentJson.contains("hapticsEnabled")) {
                        decoded.hapticsEnabled
                    } else {
                        preferences[HAPTICS_ENABLED] ?: legacySoundHaptics ?: decoded.hapticsEnabled
                    }
                    val keepScreenOn = if (currentJson.contains("keepScreenOnWhileRunning")) {
                        decoded.keepScreenOnWhileRunning
                    } else {
                        preferences[KEEP_SCREEN_ON_WHILE_RUNNING] ?: decoded.keepScreenOnWhileRunning
                    }
                    val dynamicColor = if (currentJson.contains("useDynamicColor")) {
                        decoded.useDynamicColor
                    } else {
                        preferences[USE_DYNAMIC_COLOR] ?: decoded.useDynamicColor
                    }
                    val autoSortOption = if (currentJson.contains("autoSortOption")) {
                        decoded.autoSortOption
                    } else if (currentJson.contains("\"autoSortEnabled\":true")) {
                        AppAutoSortOption.BY_FASTEST_LAP
                    } else {
                        val optionStr = preferences[AUTO_SORT_OPTION]
                        val legacyAutoSort = preferences[AUTO_SORT_ENABLED]
                        if (optionStr != null) {
                            AppAutoSortOption.entries.find { it.code == optionStr || it.name == optionStr } ?: AppAutoSortOption.NONE
                        } else if (legacyAutoSort == true) {
                            AppAutoSortOption.BY_FASTEST_LAP
                        } else {
                            decoded.autoSortOption
                        }
                    }
                    decoded.copy(
                        soundEnabled = sound,
                        hapticsEnabled = haptics,
                        keepScreenOnWhileRunning = keepScreenOn,
                        useDynamicColor = dynamicColor,
                        autoSortOption = autoSortOption
                    )
                } catch (_: Exception) {
                    fallbackFromPreferences(preferences)
                }
            } else {
                fallbackFromPreferences(preferences)
            }
            val updated = transform(currentSettings)
            preferences[APP_SETTINGS_JSON] = json.encodeToString(updated)
            preferences[SOUND_ENABLED] = updated.soundEnabled
            preferences[HAPTICS_ENABLED] = updated.hapticsEnabled
            preferences[HARDWARE_KEYS_ENABLED] = updated.hardwareVolumeKeysEnabled
            preferences[RUNNING_DISPLAY_PRECISION] = updated.runningDisplayPrecision.name
            preferences[TIME_PRECISION] = updated.timePrecision.name
            preferences[AUTO_SORT_OPTION] = updated.autoSortOption.code
            preferences[AUTO_SORT_ENABLED] = (updated.autoSortOption != AppAutoSortOption.NONE)
            preferences[APP_LANGUAGE] = updated.language.name
            preferences[KEEP_SCREEN_ON_WHILE_RUNNING] = updated.keepScreenOnWhileRunning
            preferences[USE_DYNAMIC_COLOR] = updated.useDynamicColor
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[APP_SETTINGS_JSON] = json.encodeToString(settings)
            preferences[SOUND_ENABLED] = settings.soundEnabled
            preferences[HAPTICS_ENABLED] = settings.hapticsEnabled
            preferences[HARDWARE_KEYS_ENABLED] = settings.hardwareVolumeKeysEnabled
            preferences[RUNNING_DISPLAY_PRECISION] = settings.runningDisplayPrecision.name
            preferences[TIME_PRECISION] = settings.timePrecision.name
            preferences[AUTO_SORT_OPTION] = settings.autoSortOption.code
            preferences[AUTO_SORT_ENABLED] = (settings.autoSortOption != AppAutoSortOption.NONE)
            preferences[APP_LANGUAGE] = settings.language.name
            preferences[KEEP_SCREEN_ON_WHILE_RUNNING] = settings.keepScreenOnWhileRunning
            preferences[USE_DYNAMIC_COLOR] = settings.useDynamicColor
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        updateSettings { it.copy(soundEnabled = enabled) }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        updateSettings { it.copy(hapticsEnabled = enabled) }
    }

    suspend fun setHardwareKeysEnabled(enabled: Boolean) {
        updateSettings { it.copy(hardwareVolumeKeysEnabled = enabled) }
    }

    suspend fun setRunningDisplayPrecision(precision: AppPrecision) {
        updateSettings { it.copy(runningDisplayPrecision = precision) }
    }

    suspend fun setTimePrecision(precision: AppPrecision) {
        updateSettings { it.copy(timePrecision = precision) }
    }

    suspend fun setAutoSortOption(option: AppAutoSortOption) {
        updateSettings { it.copy(autoSortOption = option) }
    }

    suspend fun setAutoSortEnabled(enabled: Boolean) {
        setAutoSortOption(if (enabled) AppAutoSortOption.BY_FASTEST_LAP else AppAutoSortOption.NONE)
    }

    suspend fun setKeepScreenOnWhileRunning(enabled: Boolean) {
        updateSettings { it.copy(keepScreenOnWhileRunning = enabled) }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        updateSettings { it.copy(useDynamicColor = enabled) }
    }

    suspend fun resetSettingsToDefault() {
        saveSettings(AppSettings())
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        updateSettings { it.copy(language = language) }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    suspend fun setFontSizeScale(fontSizeScale: AppFontSize) {
        updateSettings { it.copy(fontSizeScale = fontSizeScale) }
    }

    suspend fun addProposedAthleteName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        updateSettings { current ->
            if (current.proposedAthleteNames.contains(trimmed)) {
                current
            } else {
                current.copy(proposedAthleteNames = current.proposedAthleteNames + trimmed)
            }
        }
    }

    suspend fun removeProposedAthleteName(name: String) {
        updateSettings { current ->
            current.copy(proposedAthleteNames = current.proposedAthleteNames - name)
        }
    }

    suspend fun addProposedSessionName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        updateSettings { current ->
            if (current.proposedSessionNames.contains(trimmed)) {
                current
            } else {
                current.copy(proposedSessionNames = current.proposedSessionNames + trimmed)
            }
        }
    }

    suspend fun removeProposedSessionName(name: String) {
        updateSettings { current ->
            current.copy(proposedSessionNames = current.proposedSessionNames - name)
        }
    }
}
