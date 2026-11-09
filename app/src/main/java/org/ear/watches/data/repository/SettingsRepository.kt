package org.ear.watches.data.repository

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    produceMigrations = { listOf(SettingsLegacyMigration) }
)

/**
 * Repository for persisting user preferences.
 *
 * All settings are stored as a single JSON-encoded [AppSettings] under [APP_SETTINGS_JSON].
 * Individual keys written by older app versions are folded into that JSON exactly once by
 * [SettingsLegacyMigration] and then removed.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::decodeSettings)

    private suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            preferences[APP_SETTINGS_JSON] = json.encodeToString(transform(decodeSettings(preferences)))
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { it[APP_SETTINGS_JSON] = json.encodeToString(settings) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) = updateSettings { it.copy(soundEnabled = enabled) }

    suspend fun setHapticsEnabled(enabled: Boolean) = updateSettings { it.copy(hapticsEnabled = enabled) }

    suspend fun setHardwareKeysEnabled(enabled: Boolean) =
        updateSettings { it.copy(hardwareVolumeKeysEnabled = enabled) }

    suspend fun setRunningDisplayPrecision(precision: AppPrecision) =
        updateSettings { it.copy(runningDisplayPrecision = precision) }

    suspend fun setTimePrecision(precision: AppPrecision) = updateSettings { it.copy(timePrecision = precision) }

    suspend fun setAutoSortOption(option: AppAutoSortOption) = updateSettings { it.copy(autoSortOption = option) }

    suspend fun setKeepScreenOnWhileRunning(enabled: Boolean) =
        updateSettings { it.copy(keepScreenOnWhileRunning = enabled) }

    suspend fun setShowRunningNotification(enabled: Boolean) =
        updateSettings { it.copy(showRunningNotification = enabled) }

    suspend fun setUseDynamicColor(enabled: Boolean) = updateSettings { it.copy(useDynamicColor = enabled) }

    suspend fun resetSettingsToDefault() = saveSettings(AppSettings())

    suspend fun setAppLanguage(language: AppLanguage) = updateSettings { it.copy(language = language) }

    suspend fun setThemeMode(themeMode: AppThemeMode) =
        updateSettings { it.copy(themeMode = themeMode) }

    suspend fun setFontSizeScale(fontSizeScale: AppFontSize) =
        updateSettings { it.copy(fontSizeScale = fontSizeScale) }

    suspend fun addProposedAthleteName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        updateSettings { current ->
            if (trimmed in current.proposedAthleteNames) current
            else current.copy(proposedAthleteNames = current.proposedAthleteNames + trimmed)
        }
    }

    suspend fun removeProposedAthleteName(name: String) =
        updateSettings { it.copy(proposedAthleteNames = it.proposedAthleteNames - name) }

    suspend fun addProposedSessionName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        updateSettings { current ->
            if (trimmed in current.proposedSessionNames) current
            else current.copy(proposedSessionNames = current.proposedSessionNames + trimmed)
        }
    }

    suspend fun removeProposedSessionName(name: String) =
        updateSettings { it.copy(proposedSessionNames = it.proposedSessionNames - name) }

    companion object {
        val APP_SETTINGS_JSON = stringPreferencesKey("app_settings_json")

        internal val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        /** Decodes the stored settings; missing or unreadable data yields the defaults. */
        internal fun decodeSettings(preferences: Preferences): AppSettings =
            preferences[APP_SETTINGS_JSON]
                ?.let { runCatching { json.decodeFromString<AppSettings>(it) }.getOrNull() }
                ?: AppSettings()
    }
}

/**
 * One-time migration from the individual preference keys used by older app versions to the
 * single JSON blob. Runs before the first read whenever any legacy key is still present.
 */
object SettingsLegacyMigration : DataMigration<Preferences> {

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

    private val LEGACY_KEYS: List<Preferences.Key<*>> = listOf(
        SOUND_ENABLED, HAPTICS_ENABLED, LEGACY_SOUND_HAPTICS_ENABLED, HARDWARE_KEYS_ENABLED,
        RUNNING_DISPLAY_PRECISION, TIME_PRECISION, AUTO_SORT_OPTION, AUTO_SORT_ENABLED,
        APP_LANGUAGE, KEEP_SCREEN_ON_WHILE_RUNNING, USE_DYNAMIC_COLOR
    )

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        LEGACY_KEYS.any { currentData.contains(it) }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val settings = mergeLegacySettings(currentData)
        return currentData.toMutablePreferences().apply {
            LEGACY_KEYS.forEach { remove(it) }
            this[SettingsRepository.APP_SETTINGS_JSON] = SettingsRepository.json.encodeToString(settings)
        }.toPreferences()
    }

    override suspend fun cleanUp() = Unit

    /**
     * Combines the JSON blob (if any) with legacy keys. Fields present in the JSON win; fields that
     * older JSON versions did not contain are taken from the legacy keys.
     */
    internal fun mergeLegacySettings(preferences: Preferences): AppSettings {
        val jsonObject = preferences[SettingsRepository.APP_SETTINGS_JSON]
            ?.let { runCatching { SettingsRepository.json.parseToJsonElement(it).jsonObject }.getOrNull() }
        val decoded = jsonObject
            ?.let { runCatching { SettingsRepository.json.decodeFromJsonElement(AppSettings.serializer(), it) }.getOrNull() }
            ?: return fromLegacyKeys(preferences)

        val legacySoundHaptics = preferences[LEGACY_SOUND_HAPTICS_ENABLED]
        fun has(field: String) = jsonObject.containsKey(field)

        return decoded.copy(
            soundEnabled = if (has("soundEnabled")) decoded.soundEnabled
            else preferences[SOUND_ENABLED] ?: legacySoundHaptics ?: decoded.soundEnabled,
            hapticsEnabled = if (has("hapticsEnabled")) decoded.hapticsEnabled
            else preferences[HAPTICS_ENABLED] ?: legacySoundHaptics ?: decoded.hapticsEnabled,
            keepScreenOnWhileRunning = if (has("keepScreenOnWhileRunning")) decoded.keepScreenOnWhileRunning
            else preferences[KEEP_SCREEN_ON_WHILE_RUNNING] ?: decoded.keepScreenOnWhileRunning,
            useDynamicColor = if (has("useDynamicColor")) decoded.useDynamicColor
            else preferences[USE_DYNAMIC_COLOR] ?: decoded.useDynamicColor,
            autoSortOption = when {
                has("autoSortOption") -> decoded.autoSortOption
                jsonObject.booleanField("autoSortEnabled") == true -> AppAutoSortOption.BY_FASTEST_LAP
                else -> legacyAutoSortOption(preferences) ?: decoded.autoSortOption
            }
        )
    }

    private fun fromLegacyKeys(preferences: Preferences): AppSettings {
        val defaults = AppSettings()
        val legacySoundHaptics = preferences[LEGACY_SOUND_HAPTICS_ENABLED]
        return defaults.copy(
            soundEnabled = preferences[SOUND_ENABLED] ?: legacySoundHaptics ?: defaults.soundEnabled,
            hapticsEnabled = preferences[HAPTICS_ENABLED] ?: legacySoundHaptics ?: defaults.hapticsEnabled,
            hardwareVolumeKeysEnabled = preferences[HARDWARE_KEYS_ENABLED] ?: defaults.hardwareVolumeKeysEnabled,
            runningDisplayPrecision = preferences[RUNNING_DISPLAY_PRECISION].toPrecision() ?: defaults.runningDisplayPrecision,
            timePrecision = preferences[TIME_PRECISION].toPrecision() ?: defaults.timePrecision,
            autoSortOption = legacyAutoSortOption(preferences) ?: defaults.autoSortOption,
            language = AppLanguage.entries.find { it.name == preferences[APP_LANGUAGE] } ?: defaults.language,
            keepScreenOnWhileRunning = preferences[KEEP_SCREEN_ON_WHILE_RUNNING] ?: defaults.keepScreenOnWhileRunning,
            useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: defaults.useDynamicColor
        )
    }

    private fun legacyAutoSortOption(preferences: Preferences): AppAutoSortOption? {
        val option = preferences[AUTO_SORT_OPTION]
        if (option != null) {
            return AppAutoSortOption.entries.find { it.code == option || it.name == option } ?: AppAutoSortOption.NONE
        }
        return when (preferences[AUTO_SORT_ENABLED]) {
            true -> AppAutoSortOption.BY_FASTEST_LAP
            false -> AppAutoSortOption.NONE
            null -> null
        }
    }

    private fun String?.toPrecision(): AppPrecision? = this?.let { name -> AppPrecision.entries.find { it.name == name } }

    private fun JsonObject.booleanField(name: String): Boolean? =
        runCatching { this[name]?.jsonPrimitive?.booleanOrNull }.getOrNull()
}
