package com.ear.watches.ui

import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.ui.viewmodel.StopwatchViewModel
import com.ear.watches.util.RaceResultsFormatter
import com.ear.watches.viewmodel.TestTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainStopwatchUiTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var viewModel: StopwatchViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        timeProvider = TestTimeProvider(1000L)
        viewModel = StopwatchViewModel(
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiWorkflow_addWatchAndBatchAdd() {
        // Initial state should be empty and showHelpDialog false
        assertTrue(viewModel.uiState.value.stopwatches.isEmpty())
        assertFalse(viewModel.uiState.value.showHelpDialog)

        // Add single watch
        viewModel.addStopwatch("Athlete", "Sprinter 1")
        assertEquals(1, viewModel.uiState.value.stopwatches.size)
        assertEquals("Sprinter 1", viewModel.uiState.value.stopwatches[0].name)

        // Batch add 4 watches
        viewModel.batchAddStopwatches(4, "Squad")
        assertEquals(5, viewModel.uiState.value.stopwatches.size)
        assertEquals("Squad 2", viewModel.uiState.value.stopwatches[1].name)
        assertEquals("Squad 5", viewModel.uiState.value.stopwatches[4].name)
    }

    @Test
    fun uiWorkflow_editWatchDetails() {
        viewModel.addStopwatch("Athlete", "Original")
        val watchId = viewModel.uiState.value.stopwatches[0].id

        viewModel.updateStopwatch(watchId, "Renamed Athlete", 0xFFFFE082L)
        val updated = viewModel.uiState.value.stopwatches[0]
        assertEquals("Renamed Athlete", updated.name)
        assertEquals(0xFFFFE082L, updated.pastelColorHex)
    }

    @Test
    fun uiWorkflow_globalControlsAndShareExport() {
        viewModel.batchAddStopwatches(2, "Runner")

        // Start all
        viewModel.startAll()
        assertTrue(viewModel.uiState.value.stopwatches.all { it.isRunning })

        timeProvider.advanceTimeBy(3500L)

        // Lap first runner
        val id0 = viewModel.uiState.value.stopwatches[0].id
        viewModel.lapStopwatch(id0)

        // Pause all
        viewModel.pauseAll()
        assertFalse(viewModel.uiState.value.stopwatches.any { it.isRunning })

        // Check race results formatting
        val report = RaceResultsFormatter.formatRaceResults(
            stopwatches = viewModel.uiState.value.stopwatches,
            precision = AppPrecision.TENTHS,
            nowMillis = timeProvider.currentTimeMillis()
        )

        assertTrue(report.contains("Runner 1"))
        assertTrue(report.contains("Runner 2"))
        assertTrue(report.contains("MULTI-STOPWATCH RESULTS"))
    }

    @Test
    fun uiWorkflow_settingsModifications() {
        // Default settings
        val initial = viewModel.uiState.value.settings
        assertTrue(initial.soundAndHapticsEnabled)
        assertTrue(initial.hardwareVolumeKeysEnabled)
        assertEquals(AppPrecision.TENTHS, initial.runningDisplayPrecision)
        assertEquals(AppPrecision.TENTHS, initial.timePrecision)
        assertEquals(AppLanguage.SYSTEM, initial.language)

        // Toggle sound/haptics
        viewModel.toggleSoundHaptics()
        assertFalse(viewModel.uiState.value.settings.soundAndHapticsEnabled)

        // Toggle hardware keys
        viewModel.toggleHardwareKeys()
        assertFalse(viewModel.uiState.value.settings.hardwareVolumeKeysEnabled)

        // Change running display precision
        viewModel.setRunningDisplayPrecision(AppPrecision.SECONDS)
        assertEquals(AppPrecision.SECONDS, viewModel.uiState.value.settings.runningDisplayPrecision)

        // Change precision
        viewModel.setPrecision(AppPrecision.HUNDREDTHS)
        assertEquals(AppPrecision.HUNDREDTHS, viewModel.uiState.value.settings.timePrecision)

        // Change language
        viewModel.setLanguage(AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, viewModel.uiState.value.settings.language)
    }
}
