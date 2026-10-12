package com.ear.watches.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ear.watches.data.model.AppFontSize
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import com.ear.watches.data.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val APP_SETTINGS_JSON = stringPreferencesKey("app_settings_json")
        val SOUND_HAPTICS_ENABLED = booleanPreferencesKey("sound_haptics_enabled")
        val HARDWARE_KEYS_ENABLED = booleanPreferencesKey("hardware_keys_enabled")
        val TIME_PRECISION = stringPreferencesKey("time_precision")
        val AUTO_SORT_ENABLED = booleanPreferencesKey("auto_sort_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
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
                    json.decodeFromString<AppSettings>(jsonString)
                } catch (_: Exception) {
                    fallbackFromPreferences(preferences)
                }
            } else {
                fallbackFromPreferences(preferences)
            }
        }

    private fun fallbackFromPreferences(preferences: Preferences): AppSettings {
        val soundHaptics = preferences[SOUND_HAPTICS_ENABLED] ?: true
        val hardwareKeys = preferences[HARDWARE_KEYS_ENABLED] ?: true
        val precisionStr = preferences[TIME_PRECISION]
        val precision = precisionStr?.let { str ->
            AppPrecision.entries.find { it.name == str }
        } ?: AppPrecision.TENTHS
        val autoSort = preferences[AUTO_SORT_ENABLED] ?: false
        val languageStr = preferences[APP_LANGUAGE]
        val language = languageStr?.let { str ->
            AppLanguage.entries.find { it.name == str }
        } ?: AppLanguage.SYSTEM

        return AppSettings(
            soundAndHapticsEnabled = soundHaptics,
            hardwareVolumeKeysEnabled = hardwareKeys,
            timePrecision = precision,
            language = language,
            autoSortEnabled = autoSort,
            themeMode = AppThemeMode.SYSTEM_DEFAULT,
            fontSizeScale = AppFontSize.NORMAL
        )
    }

    private suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val currentJson = preferences[APP_SETTINGS_JSON]
            val currentSettings = if (currentJson != null) {
                try {
                    json.decodeFromString<AppSettings>(currentJson)
                } catch (_: Exception) {
                    fallbackFromPreferences(preferences)
                }
            } else {
                fallbackFromPreferences(preferences)
            }
            val updated = transform(currentSettings)
            preferences[APP_SETTINGS_JSON] = json.encodeToString(updated)
            preferences[SOUND_HAPTICS_ENABLED] = updated.soundAndHapticsEnabled
            preferences[HARDWARE_KEYS_ENABLED] = updated.hardwareVolumeKeysEnabled
            preferences[TIME_PRECISION] = updated.timePrecision.name
            preferences[AUTO_SORT_ENABLED] = updated.autoSortEnabled
            preferences[APP_LANGUAGE] = updated.language.name
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[APP_SETTINGS_JSON] = json.encodeToString(settings)
            preferences[SOUND_HAPTICS_ENABLED] = settings.soundAndHapticsEnabled
            preferences[HARDWARE_KEYS_ENABLED] = settings.hardwareVolumeKeysEnabled
            preferences[TIME_PRECISION] = settings.timePrecision.name
            preferences[AUTO_SORT_ENABLED] = settings.autoSortEnabled
            preferences[APP_LANGUAGE] = settings.language.name
        }
    }

    suspend fun setSoundHapticsEnabled(enabled: Boolean) {
        updateSettings { it.copy(soundAndHapticsEnabled = enabled) }
    }

    suspend fun setHardwareKeysEnabled(enabled: Boolean) {
        updateSettings { it.copy(hardwareVolumeKeysEnabled = enabled) }
    }

    suspend fun setTimePrecision(precision: AppPrecision) {
        updateSettings { it.copy(timePrecision = precision) }
    }

    suspend fun setAutoSortEnabled(enabled: Boolean) {
        updateSettings { it.copy(autoSortEnabled = enabled) }
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
}
