package com.ear.watches.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.dataStore)

    companion object {
        val SOUND_HAPTICS_ENABLED = booleanPreferencesKey("sound_haptics_enabled")
        val HARDWARE_KEYS_ENABLED = booleanPreferencesKey("hardware_keys_enabled")
        val TIME_PRECISION = stringPreferencesKey("time_precision")
        val AUTO_SORT_ENABLED = booleanPreferencesKey("auto_sort_enabled")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
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

            AppSettings(
                soundAndHapticsEnabled = soundHaptics,
                hardwareVolumeKeysEnabled = hardwareKeys,
                timePrecision = precision,
                language = language,
                autoSortEnabled = autoSort
            )
        }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[SOUND_HAPTICS_ENABLED] = settings.soundAndHapticsEnabled
            preferences[HARDWARE_KEYS_ENABLED] = settings.hardwareVolumeKeysEnabled
            preferences[TIME_PRECISION] = settings.timePrecision.name
            preferences[AUTO_SORT_ENABLED] = settings.autoSortEnabled
            preferences[APP_LANGUAGE] = settings.language.name
        }
    }

    suspend fun setSoundHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setHardwareKeysEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HARDWARE_KEYS_ENABLED] = enabled
        }
    }

    suspend fun setTimePrecision(precision: AppPrecision) {
        dataStore.edit { preferences ->
            preferences[TIME_PRECISION] = precision.name
        }
    }

    suspend fun setAutoSortEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SORT_ENABLED] = enabled
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language.name
        }
    }
}
