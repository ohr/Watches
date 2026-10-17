package org.ear.watches.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @BeforeEach
    fun setUp() {
        val testFile = File(tempDir, "test_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun defaultSettings_returnsExpectedValues() = testScope.runTest {
        val settings = repository.settingsFlow.first()
        assertTrue(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
        assertTrue(settings.hardwareVolumeKeysEnabled)
        assertEquals(AppPrecision.TENTHS, settings.runningDisplayPrecision)
        assertEquals(AppPrecision.TENTHS, settings.timePrecision)
        assertEquals(AppLanguage.SYSTEM, settings.language)
        assertFalse(settings.autoSortEnabled)
        assertEquals(AppThemeMode.SYSTEM_DEFAULT, settings.themeMode)
        assertEquals(AppFontSize.NORMAL, settings.fontSizeScale)
        assertTrue(settings.keepScreenOnWhileRunning)
    }

    @Test
    fun setSoundEnabled_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setSoundEnabled(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.soundEnabled)
        assertTrue(settings.hapticsEnabled)
    }

    @Test
    fun setHapticsEnabled_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setHapticsEnabled(false)
        val settings = repository.settingsFlow.first()
        assertTrue(settings.soundEnabled)
        assertFalse(settings.hapticsEnabled)
    }

    @Test
    fun setHardwareKeysEnabled_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setHardwareKeysEnabled(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.hardwareVolumeKeysEnabled)
    }

    @Test
    fun setRunningDisplayPrecision_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setRunningDisplayPrecision(AppPrecision.SECONDS)
        val settings = repository.settingsFlow.first()
        assertEquals(AppPrecision.SECONDS, settings.runningDisplayPrecision)
    }

    @Test
    fun setTimePrecision_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setTimePrecision(AppPrecision.HUNDREDTHS)
        val settings = repository.settingsFlow.first()
        assertEquals(AppPrecision.HUNDREDTHS, settings.timePrecision)
    }

    @Test
    fun setAutoSortEnabled_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setAutoSortEnabled(true)
        val settings = repository.settingsFlow.first()
        assertTrue(settings.autoSortEnabled)
    }

    @Test
    fun setKeepScreenOnWhileRunning_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setKeepScreenOnWhileRunning(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.keepScreenOnWhileRunning)
    }

    @Test
    fun setAppLanguage_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setAppLanguage(AppLanguage.GERMAN)
        val settings = repository.settingsFlow.first()
        assertEquals(AppLanguage.GERMAN, settings.language)
    }

    @Test
    fun setThemeMode_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setThemeMode(AppThemeMode.DARK)
        val settings = repository.settingsFlow.first()
        assertEquals(AppThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun setFontSizeScale_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setFontSizeScale(AppFontSize.LARGE)
        val settings = repository.settingsFlow.first()
        assertEquals(AppFontSize.LARGE, settings.fontSizeScale)
    }

    @Test
    fun saveSettings_serializesAndDeserializesAllFields() = testScope.runTest {
        val customSettings = AppSettings(
            soundEnabled = false,
            hapticsEnabled = false,
            hardwareVolumeKeysEnabled = false,
            runningDisplayPrecision = AppPrecision.HUNDREDTHS,
            timePrecision = AppPrecision.SECONDS,
            language = AppLanguage.ENGLISH,
            autoSortEnabled = true,
            themeMode = AppThemeMode.DARK,
            fontSizeScale = AppFontSize.SMALL,
            keepScreenOnWhileRunning = false
        )
        repository.saveSettings(customSettings)
        val loaded = repository.settingsFlow.first()
        assertEquals(customSettings, loaded)
    }

    @Test
    fun fallbackFromLegacyPreferences_worksWhenJsonKeyIsMissing() = testScope.runTest {
        dataStore.edit { preferences ->
            preferences[SettingsRepository.LEGACY_SOUND_HAPTICS_ENABLED] = false
            preferences[SettingsRepository.HARDWARE_KEYS_ENABLED] = true
            preferences[SettingsRepository.RUNNING_DISPLAY_PRECISION] = AppPrecision.SECONDS.name
            preferences[SettingsRepository.TIME_PRECISION] = AppPrecision.HUNDREDTHS.name
            preferences[SettingsRepository.AUTO_SORT_ENABLED] = true
            preferences[SettingsRepository.APP_LANGUAGE] = AppLanguage.GERMAN.name
        }

        val settings = repository.settingsFlow.first()
        assertFalse(settings.soundEnabled)
        assertFalse(settings.hapticsEnabled)
        assertTrue(settings.hardwareVolumeKeysEnabled)
        assertEquals(AppPrecision.SECONDS, settings.runningDisplayPrecision)
        assertEquals(AppPrecision.HUNDREDTHS, settings.timePrecision)
        assertTrue(settings.autoSortEnabled)
        assertEquals(AppLanguage.GERMAN, settings.language)
        assertEquals(AppThemeMode.SYSTEM_DEFAULT, settings.themeMode)
        assertEquals(AppFontSize.NORMAL, settings.fontSizeScale)
        assertTrue(settings.keepScreenOnWhileRunning)
    }
}
