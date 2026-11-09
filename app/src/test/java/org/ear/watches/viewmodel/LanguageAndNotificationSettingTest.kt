package org.ear.watches.viewmodel

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.StopwatchUiState
import org.ear.watches.data.repository.SettingsRepository
import org.ear.watches.service.RunningNotificationModel
import org.ear.watches.ui.viewmodel.StopwatchViewModel
import org.ear.watches.util.LocaleController
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

private class FakeLocaleController(var applied: AppLanguage = AppLanguage.SYSTEM) : LocaleController {
    val applyCalls = mutableListOf<AppLanguage>()
    override fun current() = applied
    override fun apply(language: AppLanguage) {
        applyCalls += language
        applied = language
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class LanguageAndNotificationSettingTest {

    @TempDir
    lateinit var tempDir: File

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(controller: LocaleController, repository: SettingsRepository? = null) = StopwatchViewModel(
        settingsRepository = repository,
        timeProvider = TestTimeProvider(),
        dispatcher = testDispatcher,
        localeController = controller
    )

    @Test
    fun setLanguage_appliesViaController() {
        val controller = FakeLocaleController()
        val vm = viewModel(controller)

        vm.setLanguage(AppLanguage.FRENCH)

        assertEquals(listOf(AppLanguage.FRENCH), controller.applyCalls)
        assertEquals(AppLanguage.FRENCH, vm.uiState.value.settings.language)
    }

    @Test
    fun refreshLanguage_picksUpLanguageChangedInSystemSettings() {
        val controller = FakeLocaleController()
        val vm = viewModel(controller)

        controller.applied = AppLanguage.GERMAN
        vm.refreshLanguage()

        assertEquals(AppLanguage.GERMAN, vm.uiState.value.settings.language)
        assertTrue(controller.applyCalls.isEmpty())
    }

    @Test
    fun appliedPlatformLanguage_winsOverStoredSetting_andIsWrittenBack() = runTest {
        val repository = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { File(tempDir, "s.preferences_pb") })
        )
        repository.setAppLanguage(AppLanguage.ENGLISH)
        val controller = FakeLocaleController(applied = AppLanguage.GERMAN)

        val vm = viewModel(controller, repository)
        val settings = vm.uiState.first { it.settings.language == AppLanguage.GERMAN }.settings

        assertEquals(AppLanguage.GERMAN, settings.language)
        assertEquals(AppLanguage.GERMAN, repository.settingsFlow.first { it.language == AppLanguage.GERMAN }.language)
        assertTrue(controller.applyCalls.isEmpty())
    }

    @Test
    fun resolve_migratesStoredLanguageWhenNothingApplied() {
        // JVM tests run with Build.VERSION.SDK_INT == 0, i.e. the pre-Android-13 path.
        val controller = FakeLocaleController()

        assertEquals(AppLanguage.FRENCH, controller.resolve(AppLanguage.FRENCH))
        assertEquals(listOf(AppLanguage.FRENCH), controller.applyCalls)
    }

    @Test
    fun runningNotification_canBeSwitchedOff() {
        val vm = viewModel(FakeLocaleController())
        vm.addStopwatch("Athlete")
        vm.startAll()
        assertNotNull(RunningNotificationModel.from(vm.uiState.value, 2_000_000_000L, 5_000L))

        vm.toggleRunningNotification()

        val state: StopwatchUiState = vm.uiState.value
        assertFalse(state.settings.showRunningNotification)
        assertNull(RunningNotificationModel.from(state, 2_000_000_000L, 5_000L))
    }

    @Test
    fun runningNotificationSetting_isPersisted() = runTest {
        val repository = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { File(tempDir, "n.preferences_pb") })
        )
        assertTrue(repository.settingsFlow.first().showRunningNotification)

        repository.setShowRunningNotification(false)

        assertFalse(repository.settingsFlow.first().showRunningNotification)
    }
}
