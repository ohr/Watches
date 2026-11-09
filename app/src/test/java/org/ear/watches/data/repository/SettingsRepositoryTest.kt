package org.ear.watches.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
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
        assertEquals(AppAutoSortOption.NONE, settings.autoSortOption)
        assertEquals(AppThemeMode.SYSTEM_DEFAULT, settings.themeMode)
        assertEquals(AppFontSize.NORMAL, settings.fontSizeScale)
        assertTrue(settings.keepScreenOnWhileRunning)
        assertTrue(settings.useDynamicColor)
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
    fun setAutoSortOption_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setAutoSortOption(AppAutoSortOption.BY_NAME)
        val settings = repository.settingsFlow.first()
        assertEquals(AppAutoSortOption.BY_NAME, settings.autoSortOption)
    }

    @Test
    fun setKeepScreenOnWhileRunning_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setKeepScreenOnWhileRunning(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.keepScreenOnWhileRunning)
    }

    @Test
    fun setUseDynamicColor_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setUseDynamicColor(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.useDynamicColor)
    }

    @Test
    fun resetSettingsToDefault_restoresDefaultAppSettings() = testScope.runTest {
        repository.saveSettings(
            AppSettings(
                soundEnabled = false,
                hapticsEnabled = false,
                hardwareVolumeKeysEnabled = false,
                runningDisplayPrecision = AppPrecision.SECONDS,
                timePrecision = AppPrecision.HUNDREDTHS,
                language = AppLanguage.GERMAN,
                autoSortOption = AppAutoSortOption.BY_FASTEST_LAP,
                themeMode = AppThemeMode.DARK,
                fontSizeScale = AppFontSize.LARGE,
                keepScreenOnWhileRunning = false,
                useDynamicColor = false
            )
        )
        val modified = repository.settingsFlow.first()
        assertFalse(modified.soundEnabled)
        assertFalse(modified.useDynamicColor)

        repository.resetSettingsToDefault()
        val restored = repository.settingsFlow.first()
        assertEquals(AppSettings(), restored)
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
            autoSortOption = AppAutoSortOption.BY_NAME,
            themeMode = AppThemeMode.DARK,
            fontSizeScale = AppFontSize.SMALL,
            keepScreenOnWhileRunning = false,
            useDynamicColor = false
        )
        repository.saveSettings(customSettings)
        val loaded = repository.settingsFlow.first()
        assertEquals(customSettings, loaded)
    }

    @Test
    fun legacyMigration_withoutJson_buildsSettingsFromLegacyKeysAndRemovesThem() = testScope.runTest {
        val legacy = mutablePreferencesOf(
            SettingsLegacyMigration.LEGACY_SOUND_HAPTICS_ENABLED to false,
            SettingsLegacyMigration.HARDWARE_KEYS_ENABLED to true,
            SettingsLegacyMigration.RUNNING_DISPLAY_PRECISION to AppPrecision.SECONDS.name,
            SettingsLegacyMigration.TIME_PRECISION to AppPrecision.HUNDREDTHS.name,
            SettingsLegacyMigration.AUTO_SORT_ENABLED to true,
            SettingsLegacyMigration.APP_LANGUAGE to AppLanguage.GERMAN.name
        )
        assertTrue(SettingsLegacyMigration.shouldMigrate(legacy))

        val migrated = SettingsLegacyMigration.migrate(legacy)

        assertEquals(setOf(SettingsRepository.APP_SETTINGS_JSON), migrated.asMap().keys)
        assertFalse(SettingsLegacyMigration.shouldMigrate(migrated))
        val settings = SettingsRepository.decodeSettings(migrated)
        assertFalse(settings.soundEnabled)
        assertFalse(settings.hapticsEnabled)
        assertTrue(settings.hardwareVolumeKeysEnabled)
        assertEquals(AppPrecision.SECONDS, settings.runningDisplayPrecision)
        assertEquals(AppPrecision.HUNDREDTHS, settings.timePrecision)
        assertEquals(AppAutoSortOption.BY_FASTEST_LAP, settings.autoSortOption)
        assertEquals(AppLanguage.GERMAN, settings.language)
        assertEquals(AppThemeMode.SYSTEM_DEFAULT, settings.themeMode)
        assertEquals(AppFontSize.NORMAL, settings.fontSizeScale)
        assertTrue(settings.keepScreenOnWhileRunning)
        assertTrue(settings.useDynamicColor)
    }

    @Test
    fun legacyMigration_mapsAutoSortEnabledTrueToByFastestLapAndFalseToNone() = testScope.runTest {
        val enabled = SettingsLegacyMigration.migrate(mutablePreferencesOf(SettingsLegacyMigration.AUTO_SORT_ENABLED to true))
        assertEquals(AppAutoSortOption.BY_FASTEST_LAP, SettingsRepository.decodeSettings(enabled).autoSortOption)

        val disabled = SettingsLegacyMigration.migrate(mutablePreferencesOf(SettingsLegacyMigration.AUTO_SORT_ENABLED to false))
        assertEquals(AppAutoSortOption.NONE, SettingsRepository.decodeSettings(disabled).autoSortOption)
    }

    @Test
    fun legacyMigration_jsonFieldsWin_missingJsonFieldsComeFromLegacyKeys() = testScope.runTest {
        // JSON from an older version without soundEnabled/useDynamicColor/autoSortOption.
        val oldJson = """{"hapticsEnabled":true,"timePrecision":"SECONDS","autoSortEnabled":true}"""
        val legacy = mutablePreferencesOf(
            SettingsRepository.APP_SETTINGS_JSON to oldJson,
            SettingsLegacyMigration.SOUND_ENABLED to false,
            SettingsLegacyMigration.HAPTICS_ENABLED to false,
            SettingsLegacyMigration.USE_DYNAMIC_COLOR to false,
            SettingsLegacyMigration.TIME_PRECISION to AppPrecision.HUNDREDTHS.name
        )

        val settings = SettingsRepository.decodeSettings(SettingsLegacyMigration.migrate(legacy))

        assertFalse(settings.soundEnabled) // absent in JSON -> legacy key
        assertTrue(settings.hapticsEnabled) // present in JSON -> JSON wins
        assertFalse(settings.useDynamicColor)
        assertEquals(AppPrecision.SECONDS, settings.timePrecision)
        assertEquals(AppAutoSortOption.BY_FASTEST_LAP, settings.autoSortOption)
    }

    @Test
    fun legacyMigration_runsAutomaticallyOnDataStoreWithMigrations() = runTest {
        val file = File(tempDir, "legacy.preferences_pb")
        // An old app version wrote individual keys.
        val oldScope = CoroutineScope(Job() + testDispatcher)
        PreferenceDataStoreFactory.create(scope = oldScope, produceFile = { file }).edit {
            it[SettingsLegacyMigration.SOUND_ENABLED] = false
            it[SettingsLegacyMigration.APP_LANGUAGE] = AppLanguage.FRENCH.name
        }
        oldScope.cancel()
        oldScope.coroutineContext[Job]!!.join()

        val migratingStore = PreferenceDataStoreFactory.create(
            migrations = listOf(SettingsLegacyMigration),
            scope = backgroundScope,
            produceFile = { file }
        )
        val settings = SettingsRepository(migratingStore).settingsFlow.first()

        assertFalse(settings.soundEnabled)
        assertEquals(AppLanguage.FRENCH, settings.language)
        assertEquals(setOf(SettingsRepository.APP_SETTINGS_JSON), migratingStore.data.first().asMap().keys)
    }

    @Test
    fun writes_storeOnlyTheJsonBlob() = testScope.runTest {
        repository.setSoundEnabled(false)
        repository.setAppLanguage(AppLanguage.GERMAN)

        assertEquals(setOf(SettingsRepository.APP_SETTINGS_JSON), dataStore.data.first().asMap().keys)
    }

    @Test
    fun addProposedAthleteName_addsTrimmedNonBlankUniqueNames() = testScope.runTest {
        repository.addProposedAthleteName("  Athlete A  ")
        var settings = repository.settingsFlow.first()
        assertEquals(listOf("Athlete A"), settings.proposedAthleteNames)

        // Blank name ignored
        repository.addProposedAthleteName("   ")
        settings = repository.settingsFlow.first()
        assertEquals(listOf("Athlete A"), settings.proposedAthleteNames)

        // Duplicate name not added again
        repository.addProposedAthleteName("Athlete A")
        settings = repository.settingsFlow.first()
        assertEquals(listOf("Athlete A"), settings.proposedAthleteNames)

        // Distinct new name added
        repository.addProposedAthleteName("Athlete B")
        settings = repository.settingsFlow.first()
        assertEquals(listOf("Athlete A", "Athlete B"), settings.proposedAthleteNames)
    }

    @Test
    fun removeProposedAthleteName_removesExistingNameFromList() = testScope.runTest {
        repository.addProposedAthleteName("Athlete A")
        repository.addProposedAthleteName("Athlete B")

        repository.removeProposedAthleteName("Athlete A")
        val settings = repository.settingsFlow.first()
        assertEquals(listOf("Athlete B"), settings.proposedAthleteNames)
    }

    @Test
    fun addProposedSessionName_addsTrimmedNonBlankUniqueNames() = testScope.runTest {
        repository.addProposedSessionName("  Warm-up  ")
        var settings = repository.settingsFlow.first()
        assertEquals(listOf("Warm-up"), settings.proposedSessionNames)

        // Blank name ignored
        repository.addProposedSessionName("   ")
        settings = repository.settingsFlow.first()
        assertEquals(listOf("Warm-up"), settings.proposedSessionNames)

        // Duplicate name not added again
        repository.addProposedSessionName("Warm-up")
        settings = repository.settingsFlow.first()
        assertEquals(listOf("Warm-up"), settings.proposedSessionNames)

        // Distinct new name added
        repository.addProposedSessionName("Heat 1")
        settings = repository.settingsFlow.first()
        assertEquals(listOf("Warm-up", "Heat 1"), settings.proposedSessionNames)
    }

    @Test
    fun removeProposedSessionName_removesExistingNameFromList() = testScope.runTest {
        repository.addProposedSessionName("Warm-up")
        repository.addProposedSessionName("Heat 1")

        repository.removeProposedSessionName("Warm-up")
        val settings = repository.settingsFlow.first()
        assertEquals(listOf("Heat 1"), settings.proposedSessionNames)
    }
}
