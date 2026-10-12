package com.ear.watches.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        val testFile = tmpFolder.newFile("test_settings.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = SettingsRepository(dataStore)
    }

    @Test
    fun defaultSettings_returnsExpectedValues() = testScope.runTest {
        val settings = repository.settingsFlow.first()
        assertTrue(settings.soundAndHapticsEnabled)
        assertTrue(settings.hardwareVolumeKeysEnabled)
        assertEquals(AppPrecision.TENTHS, settings.timePrecision)
        assertEquals(AppLanguage.SYSTEM, settings.language)
        assertFalse(settings.autoSortEnabled)
    }

    @Test
    fun setSoundHapticsEnabled_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setSoundHapticsEnabled(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.soundAndHapticsEnabled)
    }

    @Test
    fun setHardwareKeysEnabled_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setHardwareKeysEnabled(false)
        val settings = repository.settingsFlow.first()
        assertFalse(settings.hardwareVolumeKeysEnabled)
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
    fun setAppLanguage_updatesDataStoreAndFlow() = testScope.runTest {
        repository.setAppLanguage(AppLanguage.GERMAN)
        val settings = repository.settingsFlow.first()
        assertEquals(AppLanguage.GERMAN, settings.language)
    }

    @Test
    fun saveSettings_serializesAndDeserializesAllFields() = testScope.runTest {
        val customSettings = AppSettings(
            soundAndHapticsEnabled = false,
            hardwareVolumeKeysEnabled = false,
            timePrecision = AppPrecision.SECONDS,
            language = AppLanguage.ENGLISH,
            autoSortEnabled = true
        )
        repository.saveSettings(customSettings)
        val loaded = repository.settingsFlow.first()
        assertEquals(customSettings, loaded)
    }
}
