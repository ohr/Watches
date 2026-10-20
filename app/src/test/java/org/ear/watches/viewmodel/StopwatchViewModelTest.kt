package org.ear.watches.viewmodel

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.repository.SettingsRepository
import org.ear.watches.data.util.TimeProvider
import org.ear.watches.ui.viewmodel.StopwatchViewModel
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
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

class TestTimeProvider(
    var currentTimeNanos: Long = 1_000_000_000L,
    var currentTimeMillis: Long = 1_000L
) : TimeProvider {
    override fun currentTimeNanos(): Long = currentTimeNanos
    override fun currentTimeMillis(): Long = currentTimeMillis

    fun advanceTimeBy(nanos: Long) {
        currentTimeNanos += nanos
        currentTimeMillis += nanos / 1_000_000L
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class StopwatchViewModelTest {

    @TempDir
    lateinit var tempDir: File

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var viewModel: StopwatchViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        timeProvider = TestTimeProvider(1_000_000_000L, 1_000L)
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
        viewModel.addStopwatch("Athlete")

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
        viewModel.addStopwatch("Athlete", "Runner 1")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        viewModel.startStopwatch(watchId)
        assertTrue(viewModel.uiState.value.stopwatches.first().isRunning)

        timeProvider.advanceTimeBy(5_000_000_000L)
        val elapsedRunning = viewModel.uiState.value.stopwatches.first().getElapsedMillis(timeProvider.currentTimeNanos())
        assertEquals(5000L, elapsedRunning)

        viewModel.pauseStopwatch(watchId)
        val pausedWatch = viewModel.uiState.value.stopwatches.first()
        assertFalse(pausedWatch.isRunning)
        assertEquals(5000L, pausedWatch.getElapsedMillis(timeProvider.currentTimeNanos()))

        timeProvider.advanceTimeBy(3_000_000_000L)
        val elapsedPaused = viewModel.uiState.value.stopwatches.first().getElapsedMillis(timeProvider.currentTimeNanos())
        assertEquals(5000L, elapsedPaused)

        viewModel.resetStopwatchFull(watchId)
        val resetWatch = viewModel.uiState.value.stopwatches.first()
        assertEquals(0L, resetWatch.getElapsedMillis(timeProvider.currentTimeNanos()))
        assertEquals(0, resetWatch.currentRunLaps.size)
    }

    @Test
    fun lapStopwatch_recordsCorrectLapTimes() {
        viewModel.addStopwatch("Athlete", "Runner 1")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(4_000_000_000L)
        viewModel.lapStopwatch(watchId)

        timeProvider.advanceTimeBy(6_000_000_000L)
        viewModel.lapStopwatch(watchId)

        val watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.currentRunLaps.size)

        assertEquals(1, watch.currentRunLaps[0].lapNumber)
        assertEquals(4_000_000_000L, watch.currentRunLaps[0].lapTimeNanos)
        assertEquals(4_000_000_000L, watch.currentRunLaps[0].totalTimeNanos)
        assertEquals(null, watch.currentRunLaps[0].lapDeltaNanos)
        assertEquals(4000L, watch.currentRunLaps[0].lapTimeMillis)
        assertEquals(4000L, watch.currentRunLaps[0].totalTimeMillis)
        assertEquals(null, watch.currentRunLaps[0].lapDeltaMillis)

        assertEquals(2, watch.currentRunLaps[1].lapNumber)
        assertEquals(6_000_000_000L, watch.currentRunLaps[1].lapTimeNanos)
        assertEquals(10_000_000_000L, watch.currentRunLaps[1].totalTimeNanos)
        assertEquals(2_000_000_000L, watch.currentRunLaps[1].lapDeltaNanos)
        assertEquals(6000L, watch.currentRunLaps[1].lapTimeMillis)
        assertEquals(10000L, watch.currentRunLaps[1].totalTimeMillis)
        assertEquals(2000L, watch.currentRunLaps[1].lapDeltaMillis)
    }

    @Test
    fun lapStopwatch_calculatesLapDeltaMillis_fasterSlowerEqualAndNewSession() {
        viewModel.addStopwatch("Athlete", "Runner Delta")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        viewModel.startStopwatch(watchId)

        // Lap 1: 14,2s (14200ms = 14_200_000_000ns) -> no preceding lap -> lapDeltaNanos = null
        timeProvider.advanceTimeBy(14_200_000_000L)
        viewModel.lapStopwatch(watchId)

        // Lap 2: 13,0s (13000ms = 13_000_000_000ns) -> faster by 1,2s (-1200ms = -1_200_000_000ns)
        timeProvider.advanceTimeBy(13_000_000_000L)
        viewModel.lapStopwatch(watchId)

        // Lap 3: 13,8s (13800ms = 13_800_000_000ns) -> slower by 0,8s (+800ms = +800_000_000ns)
        timeProvider.advanceTimeBy(13_800_000_000L)
        viewModel.lapStopwatch(watchId)

        // Lap 4: 13,8s (13800ms = 13_800_000_000ns) -> equal (0ns)
        timeProvider.advanceTimeBy(13_800_000_000L)
        viewModel.lapStopwatch(watchId)

        val watch = viewModel.uiState.value.stopwatches.first()
        val laps = watch.currentRunLaps
        assertEquals(4, laps.size)

        assertEquals(null, laps[0].lapDeltaNanos)
        assertEquals(-1_200_000_000L, laps[1].lapDeltaNanos)
        assertEquals(800_000_000L, laps[2].lapDeltaNanos)
        assertEquals(0L, laps[3].lapDeltaNanos)

        assertEquals(null, laps[0].lapDeltaMillis)
        assertEquals(-1200L, laps[1].lapDeltaMillis)
        assertEquals(800L, laps[2].lapDeltaMillis)
        assertEquals(0L, laps[3].lapDeltaMillis)

        // Start Run 2 (New Session)
        viewModel.pauseStopwatch(watchId)
        viewModel.startStopwatch(watchId)

        // Lap 1 of Run 2: 10_000_000_000ns -> 1st lap in active run session -> lapDeltaNanos = null
        timeProvider.advanceTimeBy(10_000_000_000L)
        viewModel.lapStopwatch(watchId)

        val run2Laps = viewModel.uiState.value.stopwatches.first().currentRunLaps
        assertEquals(1, run2Laps.size)
        assertEquals(null, run2Laps[0].lapDeltaNanos)
        assertEquals(null, run2Laps[0].lapDeltaMillis)
    }

    @Test
    fun deleteStopwatch_updatesFocusIfDeleted() {
        viewModel.batchAddStopwatches(2, "Athlete")
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
        viewModel.batchAddStopwatches(2, "Athlete")

        viewModel.startAll()
        assertTrue(viewModel.uiState.value.stopwatches.all { it.isRunning })

        timeProvider.advanceTimeBy(10_000_000_000L)
        viewModel.pauseAll()
        assertFalse(viewModel.uiState.value.stopwatches.any { it.isRunning })
        val now = timeProvider.currentTimeNanos()
        assertEquals(10000L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(10000L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))

        viewModel.resetAll()
        assertEquals(0L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(0L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))
    }

    @Test
    fun resetAll_vs_resetAllFull_behavior() {
        viewModel.batchAddStopwatches(2, "Athlete")

        // Start all and run for 5 seconds
        viewModel.startAll()
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.pauseAll()

        val watch0Before = viewModel.uiState.value.stopwatches[0]
        val watch1Before = viewModel.uiState.value.stopwatches[1]
        assertEquals(1, watch0Before.runs.size)
        assertEquals(1, watch1Before.runs.size)

        // Short Reset: resetAll() resets display time to 0, preserves run sessions
        viewModel.resetAll()

        val now = timeProvider.currentTimeNanos()
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
        viewModel.batchAddStopwatches(2, "Athlete")
        viewModel.handleVolumeUp()

        assertTrue(viewModel.uiState.value.stopwatches.all { it.isRunning })
    }

    @Test
    fun handleVolumeUp_predictsNextFocusedWatchAndRecordsLap() {
        viewModel.batchAddStopwatches(2, "Athlete")
        viewModel.startAll()

        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        // First Volume Up: records lap for focused watch id0
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.handleVolumeUp()

        val watch0AfterFirst = viewModel.uiState.value.stopwatches.find { it.id == id0 }!!
        assertEquals(1, watch0AfterFirst.currentRunLaps.size)

        // Now watch0 has 1 lap (5000ms), watch1 has 0 laps.
        // 0-lap watch id1 gets top priority for focus!
        assertEquals(id1, viewModel.uiState.value.activeFocusedWatchId)

        // Second Volume Up: records lap for focused watch id1
        timeProvider.advanceTimeBy(3_000_000_000L)
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
        viewModel.batchAddStopwatches(2, "Athlete")
        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        viewModel.startAll()

        // Record 2 laps for watch0
        timeProvider.advanceTimeBy(4_000_000_000L)
        viewModel.lapStopwatch(id0)
        timeProvider.advanceTimeBy(4_000_000_000L)
        viewModel.lapStopwatch(id0)

        // Watch0 has 2 laps, Watch1 has 0 laps -> Watch1 (0 laps) must be focused
        assertEquals(id1, viewModel.uiState.value.activeFocusedWatchId)
        val nextFocus = StopwatchItem.calculateNextFocusedWatchId(viewModel.uiState.value.stopwatches)
        assertEquals(id1, nextFocus)
    }

    @Test
    fun predictionCalculation_selectsSoonestExpectedNextLap() {
        viewModel.batchAddStopwatches(2, "Athlete")
        val id0 = viewModel.uiState.value.stopwatches[0].id
        val id1 = viewModel.uiState.value.stopwatches[1].id

        viewModel.startAll()

        // Lap watch0 at 10_000_000_000ns (expected next lap = 20_000_000_000ns)
        timeProvider.advanceTimeBy(10_000_000_000L)
        viewModel.lapStopwatch(id0)

        // Lap watch1 at 25_000_000_000ns total (expected next lap = 50_000_000_000ns)
        timeProvider.advanceTimeBy(15_000_000_000L)
        viewModel.lapStopwatch(id1)

        // Both have 1 lap. watch0 expected next lap < watch1
        assertEquals(id0, viewModel.uiState.value.activeFocusedWatchId)

        // Advance time and record lap 2 for watch0 at 45_000_000_000ns total
        timeProvider.advanceTimeBy(20_000_000_000L)
        viewModel.lapStopwatch(id0)

        // Focus automatically updates to watch1
        assertEquals(id1, viewModel.uiState.value.activeFocusedWatchId)
    }

    @Test
    fun autoSortingBehavior_byFastestLapAndByNameAndNone() {
        viewModel.batchAddStopwatchesWithNames(listOf("Charlie", "Alice", "Bob"))
        val charlieId = viewModel.uiState.value.stopwatches.find { it.name == "Charlie" }!!.id
        val aliceId = viewModel.uiState.value.stopwatches.find { it.name == "Alice" }!!.id
        val bobId = viewModel.uiState.value.stopwatches.find { it.name == "Bob" }!!.id

        viewModel.startAll()

        // Lap Alice at 40_000_000_000ns
        timeProvider.advanceTimeBy(40_000_000_000L)
        viewModel.lapStopwatch(aliceId)

        // Reset Bob and record lap at earlier time (20s)
        viewModel.resetStopwatchFull(bobId)
        viewModel.startStopwatch(bobId)
        timeProvider.advanceTimeBy(20_000_000_000L)
        viewModel.lapStopwatch(bobId)

        // Set auto-sort BY_FASTEST_LAP
        viewModel.setAutoSortOption(AppAutoSortOption.BY_FASTEST_LAP)
        assertEquals(AppAutoSortOption.BY_FASTEST_LAP, viewModel.uiState.value.settings.autoSortOption)

        var sortedList = viewModel.uiState.value.stopwatches
        assertEquals(3, sortedList.size)
        // 1st: Charlie (0-lap watch at top)
        // 2nd: Bob (1 lap, expected next 40s)
        // 3rd: Alice (1 lap, expected next 80s)
        assertEquals(charlieId, sortedList[0].id)
        assertEquals(bobId, sortedList[1].id)
        assertEquals(aliceId, sortedList[2].id)

        // Set auto-sort BY_NAME
        viewModel.setAutoSortOption(AppAutoSortOption.BY_NAME)
        assertEquals(AppAutoSortOption.BY_NAME, viewModel.uiState.value.settings.autoSortOption)

        sortedList = viewModel.uiState.value.stopwatches
        assertEquals(listOf("Alice", "Bob", "Charlie"), sortedList.map { it.name })

        // Set auto-sort NONE
        viewModel.setAutoSortOption(AppAutoSortOption.NONE)
        assertEquals(AppAutoSortOption.NONE, viewModel.uiState.value.settings.autoSortOption)
    }

    @Test
    fun handleVolumeDown_pausesFocusedRunningOrResetsAllWhenStopped() {
        viewModel.batchAddStopwatches(2, "Athlete")
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
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.handleVolumeDown()
        val now = timeProvider.currentTimeNanos()
        assertEquals(0L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(0L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))
    }

    @Test
    fun settingsToggles_updateStateCorrectly() {
        viewModel.toggleSound()
        assertFalse(viewModel.uiState.value.settings.soundEnabled)
        assertTrue(viewModel.uiState.value.settings.hapticsEnabled)

        viewModel.toggleHaptics()
        assertFalse(viewModel.uiState.value.settings.hapticsEnabled)

        viewModel.setSoundEnabled(true)
        assertTrue(viewModel.uiState.value.settings.soundEnabled)

        viewModel.setHapticsEnabled(true)
        assertTrue(viewModel.uiState.value.settings.hapticsEnabled)

        viewModel.toggleHardwareKeys()
        assertFalse(viewModel.uiState.value.settings.hardwareVolumeKeysEnabled)

        viewModel.setAutoSortOption(AppAutoSortOption.BY_NAME)
        assertEquals(AppAutoSortOption.BY_NAME, viewModel.uiState.value.settings.autoSortOption)

        viewModel.toggleKeepScreenOn()
        assertFalse(viewModel.uiState.value.settings.keepScreenOnWhileRunning)

        viewModel.setKeepScreenOnWhileRunning(true)
        assertTrue(viewModel.uiState.value.settings.keepScreenOnWhileRunning)

        viewModel.setRunningDisplayPrecision(AppPrecision.SECONDS)
        assertEquals(AppPrecision.SECONDS, viewModel.uiState.value.settings.runningDisplayPrecision)

        viewModel.setPrecision(AppPrecision.HUNDREDTHS)
        assertEquals(AppPrecision.HUNDREDTHS, viewModel.uiState.value.settings.timePrecision)

        viewModel.setLanguage(AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, viewModel.uiState.value.settings.language)

        viewModel.toggleDynamicColor()
        assertFalse(viewModel.uiState.value.settings.useDynamicColor)

        viewModel.setUseDynamicColor(true)
        assertTrue(viewModel.uiState.value.settings.useDynamicColor)

        viewModel.resetSettingsToDefault()
        assertEquals(AppSettings(), viewModel.uiState.value.settings)
    }

    @Test
    fun updateStopwatch_changesNameAndColor() {
        viewModel.addStopwatch("Athlete", "Old Name")
        val id = viewModel.uiState.value.stopwatches.first().id

        viewModel.updateStopwatch(id, "New Name", 0xFF123456L)
        val updated = viewModel.uiState.value.stopwatches.first()
        assertEquals("New Name", updated.name)
        assertEquals(0xFF123456L, updated.pastelColorHex)
    }

    @Test
    fun multiRunCreation_startsNewRunSessionAfterStop() {
        viewModel.addStopwatch("Athlete", "Multi-Run Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(1, watch.runs.size)
        assertEquals(1, watch.runs[0].runIndex)
        assertEquals(5_000_000_000L, watch.runs[0].accumulatedNanos)
        assertEquals(5000L, watch.runs[0].accumulatedMillis)
        assertEquals(1, watch.runs[0].laps.size)

        // Run 2
        timeProvider.advanceTimeBy(2_000_000_000L)
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(3_000_000_000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)
        assertEquals(2, watch.runs[1].runIndex)
        assertEquals(3_000_000_000L, watch.runs[1].accumulatedNanos)
        assertEquals(3000L, watch.runs[1].accumulatedMillis)
        assertEquals(1, watch.runs[1].laps.size)

        // Active run elapsed time = 3000ms
        assertEquals(3000L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
        // Total accumulated time across runs = 5000 + 3000 = 8000ms
        assertEquals(8000L, watch.totalCumulativeMillis)
    }

    @Test
    fun shortClickReset_pausesActiveRunAndPreservesRunSessions() {
        viewModel.addStopwatch("Athlete", "Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(4_000_000_000L)
        viewModel.pauseStopwatch(watchId)

        // Run 2
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(6_000_000_000L)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)
        assertTrue(watch.isRunning)

        // Short click reset: finalizes active run and prepares next run session
        viewModel.resetStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(3, watch.runs.size)
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
        assertEquals(10000L, watch.getTotalCumulativeMillis(timeProvider.currentTimeNanos()))

        // Next startStopwatch call uses the prepared RunSession (Run 3)
        timeProvider.advanceTimeBy(1_000_000_000L)
        viewModel.startStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertTrue(watch.isRunning)
        assertEquals(3, watch.runs.size)
        assertEquals(3, watch.runs.last().runIndex)
    }

    @Test
    fun shortClickReset_onStoppedStopwatch_resetsMainDisplayToZero() {
        viewModel.addStopwatch("Athlete", "Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1 completed
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.pauseStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(5000L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))

        // Short Reset on stopped stopwatch
        viewModel.resetStopwatch(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
        assertEquals(5000L, watch.getTotalCumulativeMillis(timeProvider.currentTimeNanos()))

        // Subsequently starting stopwatch activates prepared run (Run 2)
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(2_000_000_000L)

        watch = viewModel.uiState.value.stopwatches.first()
        assertTrue(watch.isRunning)
        assertEquals(2, watch.runs.size)
        assertEquals(2000L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
        assertEquals(7000L, watch.getTotalCumulativeMillis(timeProvider.currentTimeNanos()))
    }

    @Test
    fun longPressReset_clearsAllRunsLapsAndAccumulatedTime() {
        viewModel.addStopwatch("Athlete", "Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(4_000_000_000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        // Run 2
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(6_000_000_000L)
        viewModel.lapStopwatch(watchId)
        viewModel.pauseStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)

        // Long press reset: full wipe
        viewModel.resetStopwatchFull(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertTrue(watch.runs.isEmpty())
        assertTrue(watch.allLaps.isEmpty())
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
    }

    @Test
    fun longPressReset_whenAlreadyZeroed_clearsAllRunsAndHistory() {
        viewModel.addStopwatch("Athlete", "Runner")
        val watchId = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.pauseStopwatch(watchId)

        // Short reset (zeroes current time display, preserves history)
        viewModel.resetStopwatch(watchId)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
        assertFalse(watch.runs.isEmpty())

        // Long press reset on zeroed watch (full wipe)
        viewModel.resetStopwatchFull(watchId)

        watch = viewModel.uiState.value.stopwatches.first()
        assertFalse(watch.isRunning)
        assertTrue(watch.runs.isEmpty())
        assertTrue(watch.allLaps.isEmpty())
        assertEquals(0L, watch.getElapsedMillis(timeProvider.currentTimeNanos()))
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
        assertTrue(vm.uiState.value.settings.soundEnabled)
        assertTrue(vm.uiState.value.settings.hapticsEnabled)

        // Toggle sound/haptics
        vm.toggleSound()
        assertFalse(vm.uiState.value.settings.soundEnabled)
        assertFalse(settingsRepository.settingsFlow.first().soundEnabled)
        assertTrue(vm.uiState.value.settings.hapticsEnabled)

        vm.toggleHaptics()
        assertFalse(vm.uiState.value.settings.hapticsEnabled)
        assertFalse(settingsRepository.settingsFlow.first().hapticsEnabled)

        // Toggle hardware keys
        vm.toggleHardwareKeys()
        assertFalse(vm.uiState.value.settings.hardwareVolumeKeysEnabled)
        assertFalse(settingsRepository.settingsFlow.first().hardwareVolumeKeysEnabled)

        // Set running display precision & time precision & language
        vm.setRunningDisplayPrecision(AppPrecision.SECONDS)
        assertEquals(AppPrecision.SECONDS, vm.uiState.value.settings.runningDisplayPrecision)
        assertEquals(AppPrecision.SECONDS, settingsRepository.settingsFlow.first().runningDisplayPrecision)

        vm.setPrecision(AppPrecision.HUNDREDTHS)
        assertEquals(AppPrecision.HUNDREDTHS, vm.uiState.value.settings.timePrecision)
        assertEquals(AppPrecision.HUNDREDTHS, settingsRepository.settingsFlow.first().timePrecision)

        vm.setLanguage(AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, vm.uiState.value.settings.language)
        assertEquals(AppLanguage.GERMAN, settingsRepository.settingsFlow.first().language)

        // Set auto sort option
        vm.setAutoSortOption(AppAutoSortOption.BY_NAME)
        assertEquals(AppAutoSortOption.BY_NAME, vm.uiState.value.settings.autoSortOption)
        assertEquals(AppAutoSortOption.BY_NAME, settingsRepository.settingsFlow.first().autoSortOption)

        // Toggle keep screen awake
        vm.toggleKeepScreenOn()
        assertFalse(vm.uiState.value.settings.keepScreenOnWhileRunning)
        assertFalse(settingsRepository.settingsFlow.first().keepScreenOnWhileRunning)

        // Set theme mode & font size scale
        vm.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, vm.uiState.value.settings.themeMode)
        assertEquals(AppThemeMode.DARK, settingsRepository.settingsFlow.first().themeMode)

        vm.setFontSizeScale(AppFontSize.LARGE)
        assertEquals(AppFontSize.LARGE, vm.uiState.value.settings.fontSizeScale)
        assertEquals(AppFontSize.LARGE, settingsRepository.settingsFlow.first().fontSizeScale)

        // Dynamic color & reset to default
        vm.toggleDynamicColor()
        assertFalse(vm.uiState.value.settings.useDynamicColor)
        assertFalse(settingsRepository.settingsFlow.first().useDynamicColor)

        vm.resetSettingsToDefault()
        assertEquals(AppSettings(), vm.uiState.value.settings)
        assertEquals(AppSettings(), settingsRepository.settingsFlow.first())
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
            soundEnabled = false,
            hapticsEnabled = false,
            hardwareVolumeKeysEnabled = false,
            runningDisplayPrecision = AppPrecision.SECONDS,
            timePrecision = AppPrecision.HUNDREDTHS,
            language = AppLanguage.ENGLISH,
            autoSortOption = AppAutoSortOption.BY_FASTEST_LAP
        )

        settingsRepository.saveSettings(updatedSettings)

        assertEquals(updatedSettings, vm.uiState.value.settings)
    }

    @Test
    fun batchAddStopwatchesWithNames_createsStopwatchesWithNamesAndDistinctPastelColors() {
        val names = listOf("Alice", "Bob", "Charlie")
        viewModel.batchAddStopwatchesWithNames(names)

        val state = viewModel.uiState.value
        assertEquals(3, state.stopwatches.size)
        assertEquals("Alice", state.stopwatches[0].name)
        assertEquals("Bob", state.stopwatches[1].name)
        assertEquals("Charlie", state.stopwatches[2].name)

        assertEquals(state.stopwatches[0].id, state.activeFocusedWatchId)
        assertTrue(state.stopwatches[0].isFocused)
    }

    @Test
    fun updateStopwatch_automaticallyPersistsEditedNameToProposedAthleteNames() = runTest(testDispatcher) {
        val testFile = File(tempDir, "vm_update_name_settings.preferences_pb")
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

        vm.addStopwatch("Athlete")
        val id = vm.uiState.value.stopwatches.first().id

        vm.updateStopwatch(id, "  Coach Dave  ", 0xFF123456L)

        val updatedWatch = vm.uiState.value.stopwatches.first()
        assertEquals("Coach Dave", updatedWatch.name)

        // Verifies name is trimmed and added to proposedAthleteNames in UI state and repository
        assertTrue(vm.uiState.value.settings.proposedAthleteNames.contains("Coach Dave"))
        val repoSettings = settingsRepository.settingsFlow.first()
        assertTrue(repoSettings.proposedAthleteNames.contains("Coach Dave"))
    }

    @Test
    fun removeProposedAthleteName_removesNameFromProposedList() = runTest(testDispatcher) {
        val testFile = File(tempDir, "vm_remove_proposed.preferences_pb")
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

        vm.addProposedAthleteName("Runner One")
        vm.addProposedAthleteName("Runner Two")
        assertTrue(vm.uiState.value.settings.proposedAthleteNames.contains("Runner One"))

        vm.removeProposedAthleteName("Runner One")
        assertFalse(vm.uiState.value.settings.proposedAthleteNames.contains("Runner One"))
        assertTrue(vm.uiState.value.settings.proposedAthleteNames.contains("Runner Two"))

        val repoSettings = settingsRepository.settingsFlow.first()
        assertFalse(repoSettings.proposedAthleteNames.contains("Runner One"))
        assertTrue(repoSettings.proposedAthleteNames.contains("Runner Two"))
    }

    @Test
    fun startStopwatch_withSessionName_and_updateActiveRunSessionName() {
        viewModel.addStopwatch("Athlete")
        val id = viewModel.uiState.value.stopwatches.first().id

        viewModel.startStopwatch(id, sessionName = "Warm-up")

        val watch1 = viewModel.uiState.value.stopwatches.first()
        assertEquals("Warm-up", watch1.activeRun?.name)

        viewModel.updateActiveRunSessionName(id, "Heat 1")

        val watch2 = viewModel.uiState.value.stopwatches.first()
        assertEquals("Heat 1", watch2.activeRun?.name)
    }

    @Test
    fun startAll_withSessionName_and_updateAllActiveRunSessionNames() {
        viewModel.batchAddStopwatches(2, "Runner")

        viewModel.startAll(sessionName = "Heat 1")

        val watches1 = viewModel.uiState.value.stopwatches
        assertEquals("Heat 1", watches1[0].activeRun?.name)
        assertEquals("Heat 1", watches1[1].activeRun?.name)

        viewModel.updateAllActiveRunSessionNames("Finals")

        val watches2 = viewModel.uiState.value.stopwatches
        assertEquals("Finals", watches2[0].activeRun?.name)
        assertEquals("Finals", watches2[1].activeRun?.name)
    }

    @Test
    fun updateRunSessionName_retroactiveByRunIndex() {
        viewModel.addStopwatch("Athlete")
        val id = viewModel.uiState.value.stopwatches.first().id

        // Run 1
        viewModel.startStopwatch(id, "Run 1 Name")
        timeProvider.advanceTimeBy(5_000_000_000L)
        viewModel.pauseStopwatch(id)

        // Run 2
        viewModel.startStopwatch(id, "Run 2 Name")
        timeProvider.advanceTimeBy(3_000_000_000L)
        viewModel.pauseStopwatch(id)

        var watch = viewModel.uiState.value.stopwatches.first()
        assertEquals(2, watch.runs.size)
        assertEquals("Run 1 Name", watch.runs[0].name)
        assertEquals("Run 2 Name", watch.runs[1].name)

        // Retroactive rename of Run 1
        viewModel.updateRunSessionName(id, runIndex = 1, newName = "Warm-up Final")

        watch = viewModel.uiState.value.stopwatches.first()
        assertEquals("Warm-up Final", watch.runs[0].name)
        assertEquals("Run 2 Name", watch.runs[1].name)
    }

    @Test
    fun addProposedSessionName_and_removeProposedSessionName_updatesStateAndRepository() = runTest(testDispatcher) {
        val testFile = File(tempDir, "vm_proposed_session.preferences_pb")
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

        vm.addProposedSessionName("  Session A  ")
        vm.addProposedSessionName("Session B")
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Session A"))
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Session B"))

        vm.removeProposedSessionName("Session A")
        assertFalse(vm.uiState.value.settings.proposedSessionNames.contains("Session A"))
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Session B"))

        val repoSettings = settingsRepository.settingsFlow.first()
        assertFalse(repoSettings.proposedSessionNames.contains("Session A"))
        assertTrue(repoSettings.proposedSessionNames.contains("Session B"))
    }

    @Test
    fun sessionNameCreationAndUpdates_automaticallyAddsToProposedSessionNames() = runTest(testDispatcher) {
        val testFile = File(tempDir, "vm_auto_session.preferences_pb")
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

        vm.addStopwatch("Athlete")
        val id = vm.uiState.value.stopwatches.first().id

        // 1. startStopwatch with name
        vm.startStopwatch(id, sessionName = "Warm-up")
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Warm-up"))
        assertTrue(settingsRepository.settingsFlow.first().proposedSessionNames.contains("Warm-up"))

        // 2. updateActiveRunSessionName
        vm.updateActiveRunSessionName(id, "Heat 1")
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Heat 1"))

        // 3. startAll with name
        vm.startAll(sessionName = "Heat 2")
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Heat 2"))

        // 4. updateAllActiveRunSessionNames
        vm.updateAllActiveRunSessionNames("Finals")
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Finals"))

        // 5. updateRunSessionName
        vm.updateRunSessionName(id, runIndex = 1, newName = "Interval 1")
        assertTrue(vm.uiState.value.settings.proposedSessionNames.contains("Interval 1"))
    }

    @Test
    fun reorderStopwatches_movesItem_whenAutoSortNone() {
        viewModel.batchAddStopwatches(3, "Runner")
        val initialList = viewModel.uiState.value.stopwatches
        assertEquals(3, initialList.size)
        val id0 = initialList[0].id
        val id1 = initialList[1].id
        val id2 = initialList[2].id

        assertEquals(AppAutoSortOption.NONE, viewModel.uiState.value.settings.autoSortOption)

        viewModel.reorderStopwatches(0, 2)

        val updatedList = viewModel.uiState.value.stopwatches
        assertEquals(3, updatedList.size)
        assertEquals(id1, updatedList[0].id)
        assertEquals(id2, updatedList[1].id)
        assertEquals(id0, updatedList[2].id)
    }

    @Test
    fun reorderStopwatches_ignored_whenAutoSortNotNone() {
        viewModel.batchAddStopwatches(3, "Runner")
        viewModel.setAutoSortOption(AppAutoSortOption.BY_NAME)
        assertEquals(AppAutoSortOption.BY_NAME, viewModel.uiState.value.settings.autoSortOption)

        val initialList = viewModel.uiState.value.stopwatches
        val id0 = initialList[0].id
        val id1 = initialList[1].id
        val id2 = initialList[2].id

        viewModel.reorderStopwatches(0, 2)

        val updatedList = viewModel.uiState.value.stopwatches
        assertEquals(3, updatedList.size)
        assertEquals(id0, updatedList[0].id)
        assertEquals(id1, updatedList[1].id)
        assertEquals(id2, updatedList[2].id)
    }
}
