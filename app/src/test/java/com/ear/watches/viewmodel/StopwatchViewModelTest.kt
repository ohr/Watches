package com.ear.watches.viewmodel

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ear.watches.data.model.AppFontSize
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import com.ear.watches.data.model.AppThemeMode
import com.ear.watches.data.repository.SettingsRepository
import com.ear.watches.data.util.TimeProvider
import com.ear.watches.ui.viewmodel.StopwatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TestTimeProvider(var currentTime: Long = 1000L) : TimeProvider {
    override fun currentTimeMillis(): Long = currentTime

    fun advanceTimeBy(millis: Long) {
        currentTime += millis
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StopwatchViewModelTest {

    @TempDir
    lateinit var tempDir: File

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
    fun addStopwatch_defaultNameAndFocus() {
        viewModel.addStopwatch()

        val state = viewModel.uiState.value
        assertEquals(1, state.stopwatches.size)
        val watch = state.stopwatches.first()
        assertEquals("Athlete 1", watch.name)
        assertEquals(watch.id, state.activeFocusedWatchId)
        assertTrue(watch.isFocused)
    }

    @Test
    fun batchAddStopwatches_createsMultipleWithDistinctColorsAndNames() {
        viewModel.batchAddStopwatches(3, "Runner")

        val state = viewModel.uiState.value
        assertEquals(3, state.stopwatches.size)
        assertEquals("Runner 1", state.stopwatches[0].name)
        assertEquals("Runner 2", state.stopwatches[1].name)
        assertEquals("Runner 3", state.stopwatches[2].name)

        assertEquals(state.stopwatches[0].id, state.activeFocusedWatchId)
        assertTrue(state.stopwatches[0].isFocused)
        assertFalse(state.stopwatches[1].isFocused)
    }

    @Test
    fun startPauseResetStopwatch_tracksTimeAccurately() {
        viewModel.addStopwatch("Runner 1")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        viewModel.startStopwatch(watchId)
        assertTrue(viewModel.uiState.value.stopwatches.first().isRunning)

        timeProvider.advanceTimeBy(5000L)
        val elapsedRunning = viewModel.uiState.value.stopwatches.first().getElapsedMillis(timeProvider.currentTimeMillis())
        assertEquals(5000L, elapsedRunning)

        viewModel.pauseStopwatch(watchId)
        val pausedWatch = viewModel.uiState.value.stopwatches.first()
        assertFalse(pausedWatch.isRunning)
        assertEquals(5000L, pausedWatch.getElapsedMillis(timeProvider.currentTimeMillis()))

        timeProvider.advanceTimeBy(3000L)
        val elapsedPaused = viewModel.uiState.value.stopwatches.first().getElapsedMillis(timeProvider.currentTimeMillis())
        assertEquals(5000L, elapsedPaused)

        viewModel.resetStopwatchFull(watchId)
        val resetWatch = viewModel.uiState.value.stopwatches.first()
        assertEquals(0L, resetWatch.getElapsedMillis(timeProvider.currentTimeMillis()))
        assertEquals(0, resetWatch.currentRunLaps.size)
    }

    @Test
    fun lapStopwatch_recordsCorrectLapTimes() {
        viewModel.addStopwatch("Runner 1")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(4000L)
        viewModel.lapStopwatch(watchId)

        timeProvider.advanceTimeBy(6000L)
        viewModel.lapStopwatch(watchId)

        val watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.currentRunLaps.size)

        assertEquals(1, watch.currentRunLaps[0].lapNumber)
        assertEquals(4000L, watch.currentRunLaps[0].lapTimeMillis)
        assertEquals(4000L, watch.currentRunLaps[0].totalTimeMillis)

        assertEquals(2, watch.currentRunLaps[1].lapNumber)
        assertEquals(6000L, watch.currentRunLaps[1].lapTimeMillis)
        assertEquals(10000L, watch.currentRunLaps[1].totalTimeMillis)
    }

    @Test
    fun deleteStopwatch_updatesFocusIfDeleted() {
        viewModel.batchAddStopwatches(2)
        val state1 = viewModel.uiState.value
        val id0 = state1.stopwatches[0].id
        val id1 = state1.stopwatches[1].id

        assertEquals(id0, state1.activeFocusedWatchId)

        viewModel.deleteStopwatch(id0)
        val state2 = viewModel.uiState.value
        assertEquals(1, state2.stopwatches.size)
        assertEquals(id1, state2.activeFocusedWatchId)
        assertTrue(state2.stopwatches.first().isFocused)
    }

    @Test
    fun globalActions_startAllPauseAllResetAll() {
        viewModel.batchAddStopwatches(2)

        viewModel.startAll()
        assertTrue(viewModel.uiState.value.stopwatches.all { it.isRunning })

        timeProvider.advanceTimeBy(10000L)
        viewModel.pauseAll()
        assertFalse(viewModel.uiState.value.stopwatches.any { it.isRunning })
        val now = timeProvider.currentTimeMillis()
        assertEquals(10000L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(10000L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))

        viewModel.resetAll()
        assertEquals(0L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(0L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))
    }

    @Test
    fun resetAll_vs_resetAllFull_behavior() {
        viewModel.batchAddStopwatches(2)

        // Start all and run for 5 seconds
        viewModel.startAll()
        timeProvider.advanceTimeBy(5000L)
        viewModel.pauseAll()

        val watch0Before = viewModel.uiState.value.stopwatches[0]
        val watch1Before = viewModel.uiState.value.stopwatches[1]
        assertEquals(1, watch0Before.runs.size)
        assertEquals(1, watch1Before.runs.size)

        // Short Reset: resetAll() resets display time to 0, preserves run sessions
        viewModel.resetAll()

        val now = timeProvider.currentTimeMillis()
        val watch0AfterShort = viewModel.uiState.value.stopwatches[0]
        val watch1AfterShort = viewModel.uiState.value.stopwatches[1]

        assertFalse(watch0AfterShort.isRunning)
        assertFalse(watch1AfterShort.isRunning)
        assertEquals(0L, watch0AfterShort.getElapsedMillis(now))
        assertEquals(0L, watch1AfterShort.getElapsedMillis(now))

        // Runs and total history are preserved
        assertFalse(watch0AfterShort.runs.isEmpty())
        assertFalse(watch1AfterShort.runs.isEmpty())
        assertEquals(5000L, watch0AfterShort.getTotalCumulativeMillis(now))
        assertEquals(5000L, watch1AfterShort.getTotalCumulativeMillis(now))

        // Long Reset: resetAllFull() clears all run sessions and laps completely
        viewModel.resetAllFull()

        val watch0AfterFull = viewModel.uiState.value.stopwatches[0]
        val watch1AfterFull = viewModel.uiState.value.stopwatches[1]

        assertFalse(watch0AfterFull.isRunning)
        assertFalse(watch1AfterFull.isRunning)
        assertEquals(0L, watch0AfterFull.getElapsedMillis(now))
        assertEquals(0L, watch1AfterFull.getElapsedMillis(now))

        // All run sessions cleared completely
        assertTrue(watch0AfterFull.runs.isEmpty())
        assertTrue(watch1AfterFull.runs.isEmpty())
        assertTrue(watch0AfterFull.allLaps.isEmpty())
        assertTrue(watch1AfterFull.allLaps.isEmpty())
        assertEquals(0L, watch0AfterFull.getTotalCumulativeMillis(now))
        assertEquals(0L, watch1AfterFull.getTotalCumulativeMillis(now))
    }

    @Test
    fun handleVolumeUp_whenAllStopped_startsAll() {
        viewModel.batchAddStopwatches(2)
        viewModel.handleVolumeUp()

        assertTrue(viewModel.uiState.value.stopwatches.all { it.isRunning })
    }

    @Test
    fun handleVolumeUp_predictsNextFocusedWatchAndRecordsLap() {
        viewModel.batchAddStopwatches(2)
        viewModel.startAll()

        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        // First Volume Up: records lap for focused watch id0
        timeProvider.advanceTimeBy(5000L)
        viewModel.handleVolumeUp()

        val watch0AfterFirst = viewModel.uiState.value.stopwatches.find { it.id == id0 }!!
        assertEquals(1, watch0AfterFirst.currentRunLaps.size)

        // Now watch0 has 1 lap (5000ms), watch1 has 0 laps.
        // 0-lap watch id1 gets top priority for focus!
        assertEquals(id1, viewModel.uiState.value.activeFocusedWatchId)

        // Second Volume Up: records lap for focused watch id1
        timeProvider.advanceTimeBy(3000L)
        viewModel.handleVolumeUp()

        val watch1AfterSecond = viewModel.uiState.value.stopwatches.find { it.id == id1 }!!
        assertEquals(1, watch1AfterSecond.currentRunLaps.size)

        // Now both have 1 lap!
        // watch0 expected next lap = 5000 + 5000 = 10000ms
        // watch1 expected next lap = 8000 + 8000 = 16000ms
        // Focus moves to watch0 (soonest expected next lap time)
        assertEquals(id0, viewModel.uiState.value.activeFocusedWatchId)
    }

    @Test
    fun zeroLapWatch_hasTopPriorityForFocusOverMultiLapWatches() {
        viewModel.batchAddStopwatches(2)
        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        viewModel.startAll()

        // Record 2 laps for watch0
        timeProvider.advanceTimeBy(4000L)
        viewModel.lapStopwatch(id0)
        timeProvider.advanceTimeBy(4000L)
        viewModel.lapStopwatch(id0)

        // Watch0 has 2 laps, Watch1 has 0 laps -> Watch1 (0 laps) must be focused
        assertEquals(id1, viewModel.uiState.value.activeFocusedWatchId)
        val nextFocus = viewModel.calculateNextFocusedWatchId()
        assertEquals(id1, nextFocus)
    }

    @Test
    fun predictionCalculation_selectsSoonestExpectedNextLap() {
        viewModel.batchAddStopwatches(2)
        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        viewModel.startAll()

        // Lap watch0 at 10,000ms (expected next lap = 20,000ms)
        timeProvider.advanceTimeBy(10000L)
        viewModel.lapStopwatch(id0)

        // Lap watch1 at 25,000ms total (expected next lap = 50,000ms)
        timeProvider.advanceTimeBy(15000L)
        viewModel.lapStopwatch(id1)

        // Both have 1 lap. watch0 expected next lap (20,000ms) < watch1 (50,000ms)
        assertEquals(id0, viewModel.uiState.value.activeFocusedWatchId)

        // Advance time and record lap 2 for watch0 at 45,000ms total
        timeProvider.advanceTimeBy(20000L)
        viewModel.lapStopwatch(id0)

        // watch0 now has 2 laps (total 45,000ms, avg lap 22,500ms, expected next = 67,500ms)
        // watch1 expected next lap is 50,000ms
        // Focus automatically updates to watch1 (50,000ms < 67,500ms)
        assertEquals(id1, viewModel.uiState.value.activeFocusedWatchId)
    }

    @Test
    fun autoSortingBehavior_whenEnabled() {
        viewModel.batchAddStopwatches(3, "Athlete")
        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id
        val id2 = viewModel.uiState.value.stopwatches[2].id

        viewModel.startAll()

        // Lap watch1 at 40,000ms (expected next = 80,000ms)
        timeProvider.advanceTimeBy(40000L)
        viewModel.lapStopwatch(id1)

        // Lap watch2 at 20,000ms total relative to lap 1, so 60,000ms total (expected next = 120,000ms)
        // Let me manually add lap at 20,000ms total for watch2 to test exact ordering:
        // watch0: 0 laps
        // watch1: 1 lap (40,000ms, expected next = 80,000ms)
        // watch2: 1 lap (20,000ms, expected next = 40,000ms)

        // Reset watch2 and record lap at earlier time
        viewModel.resetStopwatchFull(id2)
        viewModel.startStopwatch(id2)
        timeProvider.advanceTimeBy(20000L)
        viewModel.lapStopwatch(id2)

        // Enable auto-sort
        viewModel.toggleAutoSort()
        assertTrue(viewModel.uiState.value.settings.autoSortEnabled)

        val sortedList = viewModel.uiState.value.stopwatches
        assertEquals(3, sortedList.size)

        // 1st: watch0 (0-lap watch at top)
        // 2nd: watch2 (1 lap, expected next 40,000ms)
        // 3rd: watch1 (1 lap, expected next 80,000ms)
        assertEquals(id0, sortedList[0].id)
        assertEquals(id2, sortedList[1].id)
        assertEquals(id1, sortedList[2].id)

        // Disable auto-sort -> reverts to unsorted/original list order
        viewModel.toggleAutoSort()
        assertFalse(viewModel.uiState.value.settings.autoSortEnabled)
    }

    @Test
    fun handleVolumeDown_pausesFocusedRunningOrResetsAllWhenStopped() {
        viewModel.batchAddStopwatches(2)
        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        viewModel.startAll()
        viewModel.setFocusedWatch(id0)

        // Volume Down while running: pauses focused watch (id0)
        viewModel.handleVolumeDown()
        assertFalse(viewModel.uiState.value.stopwatches.find { it.id == id0 }!!.isRunning)
        assertTrue(viewModel.uiState.value.stopwatches.find { it.id == id1 }!!.isRunning)

        // Focus id1 and press Volume Down: pauses id1
        viewModel.setFocusedWatch(id1)
        viewModel.handleVolumeDown()
        assertFalse(viewModel.uiState.value.stopwatches.find { it.id == id1 }!!.isRunning)

        // Now all stopped. Volume Down resets all
        timeProvider.advanceTimeBy(5000L)
        viewModel.handleVolumeDown()
        val now = timeProvider.currentTimeMillis()
        assertEquals(0L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(0L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))
    }

    @Test
    fun settingsToggles_updateStateCorrectly() {
        viewModel.toggleSoundHaptics()
        assertFalse(viewModel.uiState.value.settings.soundAndHapticsEnabled)

        viewModel.toggleHardwareKeys()
        assertFalse(viewModel.uiState.value.settings.hardwareVolumeKeysEnabled)

        viewModel.toggleAutoSort()
        assertTrue(viewModel.uiState.value.settings.autoSortEnabled)

        viewModel.setPrecision(AppPrecision.HUNDREDTHS)
        assertEquals(AppPrecision.HUNDREDTHS, viewModel.uiState.value.settings.timePrecision)

        viewModel.setLanguage(AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, viewModel.uiState.value.settings.language)
    }

    @Test
    fun updateStopwatch_changesNameAndColor() {
        viewModel.addStopwatch("Old Name")
        val id = viewModel.uiState.value.stopwatches.first().id

        viewModel.updateStopwatch(id, "New Name", 0xFF123456L)
        val updated = viewModel.uiState.value.stopwatches.first()
        assertEquals("New Name", updated.name)
        assertEquals(0xFF123456L, updated.pastelColorHex)
    }

    @Test
    fun multiRunCreation_startsNewRunSessionAfterStop() {
        viewModel.addStopwatch("Multi-Run Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(5000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(1, watch.runs.size)
        assertEquals(1, watch.runs[0].runIndex)
        assertEquals(5000L, watch.runs[0].accumulatedMillis)
        assertEquals(1, watch.runs[0].laps.size)

        // Run 2
        timeProvider.advanceTimeBy(2000L)
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(3000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)
        assertEquals(2, watch.runs[1].runIndex)
        assertEquals(3000L, watch.runs[1].accumulatedMillis)
        assertEquals(1, watch.runs[1].laps.size)

        // Active run elapsed time = 3000ms
        assertEquals(3000L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
        // Total accumulated time across runs = 5000 + 3000 = 8000ms
        assertEquals(8000L, watch.totalCumulativeMillis)
    }

    @Test
    fun shortClickReset_pausesActiveRunAndPreservesRunSessions() {
        viewModel.addStopwatch("Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(4000L)
        viewModel.pauseStopwatch(watchId)

        // Run 2
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(6000L)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)
        assertTrue(watch.isRunning)

        // Short click reset: finalizes active run and prepares next run session
        viewModel.resetStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(3, watch.runs.size)
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
        assertEquals(10000L, watch.getTotalCumulativeMillis(timeProvider.currentTimeMillis()))

        // Next startStopwatch call uses the prepared RunSession (Run 3)
        timeProvider.advanceTimeBy(1000L)
        viewModel.startStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertTrue(watch.isRunning)
        assertEquals(3, watch.runs.size)
        assertEquals(3, watch.runs.last().runIndex)
    }

    @Test
    fun shortClickReset_onStoppedStopwatch_resetsMainDisplayToZero() {
        viewModel.addStopwatch("Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1 completed
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(5000L)
        viewModel.pauseStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(5000L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))

        // Short Reset on stopped stopwatch
        viewModel.resetStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
        assertEquals(5000L, watch.getTotalCumulativeMillis(timeProvider.currentTimeMillis()))

        // Subsequently starting stopwatch activates prepared run (Run 2)
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(2000L)

        watch = viewModel.uiState.value.stopwatches.first()
        assertTrue(watch.isRunning)
        assertEquals(2, watch.runs.size)
        assertEquals(2000L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
        assertEquals(7000L, watch.getTotalCumulativeMillis(timeProvider.currentTimeMillis()))
    }

    @Test
    fun longPressReset_clearsAllRunsLapsAndAccumulatedTime() {
        viewModel.addStopwatch("Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(4000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        // Run 2
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(6000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)

        // Long press reset: full wipe
        viewModel.resetStopwatchFull(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertTrue(watch.runs.isEmpty())
        assertTrue(watch.allLaps.isEmpty())
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
    }

    @Test
    fun longPressReset_whenAlreadyZeroed_clearsAllRunsAndHistory() {
        viewModel.addStopwatch("Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(5000L)
        viewModel.pauseStopwatch(watchId)

        // Short reset (zeroes current time display, preserves history)
        viewModel.resetStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
        assertFalse(watch.runs.isEmpty())

        // Long press reset on zeroed watch (full wipe)
        viewModel.resetStopwatchFull(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertTrue(watch.runs.isEmpty())
        assertTrue(watch.allLaps.isEmpty())
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeMillis()))
    }

    @Test
    fun viewModel_withSettingsRepository_togglesAndPersistsSettings() = runTest(testDispatcher) {
        val testFile = File(tempDir, "vm_test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { testFile }
        )
        val settingsRepository = SettingsRepository(dataStore)

        val vm = StopwatchViewModel(
            settingsRepository = settingsRepository,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )

        // Initial settings
        assertTrue(vm.uiState.value.settings.soundAndHapticsEnabled)

        // Toggle sound/haptics
        vm.toggleSoundHaptics()
        assertFalse(vm.uiState.value.settings.soundAndHapticsEnabled)
        assertFalse(settingsRepository.settingsFlow.first().soundAndHapticsEnabled)

        // Toggle hardware keys
        vm.toggleHardwareKeys()
        assertFalse(vm.uiState.value.settings.hardwareVolumeKeysEnabled)
        assertFalse(settingsRepository.settingsFlow.first().hardwareVolumeKeysEnabled)

        // Set precision & language
        vm.setPrecision(AppPrecision.HUNDREDTHS)
        assertEquals(AppPrecision.HUNDREDTHS, vm.uiState.value.settings.timePrecision)
        assertEquals(AppPrecision.HUNDREDTHS, settingsRepository.settingsFlow.first().timePrecision)

        vm.setLanguage(AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, vm.uiState.value.settings.language)
        assertEquals(AppLanguage.GERMAN, settingsRepository.settingsFlow.first().language)

        // Toggle auto sort
        vm.toggleAutoSort()
        assertTrue(vm.uiState.value.settings.autoSortEnabled)
        assertTrue(settingsRepository.settingsFlow.first().autoSortEnabled)

        // Set theme mode & font size scale
        vm.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, vm.uiState.value.settings.themeMode)
        assertEquals(AppThemeMode.DARK, settingsRepository.settingsFlow.first().themeMode)

        vm.setFontSizeScale(AppFontSize.LARGE)
        assertEquals(AppFontSize.LARGE, vm.uiState.value.settings.fontSizeScale)
        assertEquals(AppFontSize.LARGE, settingsRepository.settingsFlow.first().fontSizeScale)
    }

    @Test
    fun viewModel_observesSettingsRepositoryFlowUpdates() = runTest(testDispatcher) {
        val testFile = File(tempDir, "vm_observe_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = this,
            produceFile = { testFile }
        )
        val settingsRepository = SettingsRepository(dataStore)

        val vm = StopwatchViewModel(
            settingsRepository = settingsRepository,
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )

        val updatedSettings = AppSettings(
            soundAndHapticsEnabled = false,
            hardwareVolumeKeysEnabled = false,
            timePrecision = AppPrecision.SECONDS,
            language = AppLanguage.ENGLISH,
            autoSortEnabled = true
        )

        settingsRepository.saveSettings(updatedSettings)

        assertEquals(updatedSettings, vm.uiState.value.settings)
    }
}
