package com.ear.watches

import com.ear.watches.data.model.AppPrecision
import com.ear.watches.ui.viewmodel.StopwatchViewModel
import com.ear.watches.util.RaceResultsFormatter
import com.ear.watches.viewmodel.TestTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HardwareAndShareTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var timeProvider: TestTimeProvider
    private lateinit var viewModel: StopwatchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        timeProvider = TestTimeProvider(1000L)
        viewModel = StopwatchViewModel(
            timeProvider = timeProvider,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun hardwareVolumeUp_whenAllStoppedAndNoLaps_startsAllStopwatches() {
        viewModel.batchAddStopwatches(3, "Athlete")
        assertFalse(viewModel.uiState.value.stopwatches.any { it.isRunning })

        viewModel.handleVolumeUp()

        assertTrue(viewModel.uiState.value.stopwatches.all { it.isRunning })
    }

    @Test
    fun hardwareVolumeUp_whenRunning_recordsSplitLapForTargetedWatch() {
        viewModel.batchAddStopwatches(2, "Runner")
        viewModel.startAll()

        val watch1Id = viewModel.uiState.value.stopwatches[1].id

        // 1. First volume up: Runner 0 receives a lap
        timeProvider.advanceTimeBy(3000L)
        viewModel.handleVolumeUp()

        val watch0Laps1 = viewModel.uiState.value.stopwatches[0].currentRunLaps
        assertEquals(1, watch0Laps1.size)
        assertEquals(3000L, watch0Laps1[0].lapTimeMillis)

        // 2. Give Runner 1 a lap so all running watches have recorded laps
        timeProvider.advanceTimeBy(2000L)
        viewModel.lapStopwatch(watch1Id)
        assertEquals(1, viewModel.uiState.value.stopwatches[1].currentRunLaps.size)

        // 3. Now all watches have laps: next volume up cycles round-robin from Runner 1 (index 1) to Runner 0 (index 0)
        viewModel.setFocusedWatch(watch1Id)
        timeProvider.advanceTimeBy(4000L)
        viewModel.handleVolumeUp()

        val watch0Laps2 = viewModel.uiState.value.stopwatches[0].currentRunLaps
        assertEquals(2, watch0Laps2.size)
    }

    @Test
    fun hardwareVolumeDown_pausesFocusedRunningWatchThenResetsAllWhenStopped() {
        viewModel.batchAddStopwatches(2, "Athlete")
        viewModel.startAll()

        val watch0Id = viewModel.uiState.value.stopwatches[0].id
        viewModel.setFocusedWatch(watch0Id)

        // Volume Down -> Pauses watch0
        viewModel.handleVolumeDown()
        assertFalse(viewModel.uiState.value.stopwatches[0].isRunning)
        assertTrue(viewModel.uiState.value.stopwatches[1].isRunning)

        // Volume Down -> Pauses watch1
        viewModel.handleVolumeDown()
        assertFalse(viewModel.uiState.value.stopwatches[1].isRunning)

        // Volume Down when all stopped -> Resets all
        viewModel.handleVolumeDown()
        val now = timeProvider.currentTimeMillis()
        assertEquals(0L, viewModel.uiState.value.stopwatches[0].getElapsedMillis(now))
        assertEquals(0L, viewModel.uiState.value.stopwatches[1].getElapsedMillis(now))
    }

    @Test
    fun raceResultsFormatting_generatesReportForSharing() {
        viewModel.batchAddStopwatches(2, "Runner")
        viewModel.startAll()
        timeProvider.advanceTimeBy(5000L)

        val id0 = viewModel.uiState.value.stopwatches[0].id
        viewModel.lapStopwatch(id0)

        val reportText = RaceResultsFormatter.formatRaceResults(
            stopwatches = viewModel.uiState.value.stopwatches,
            precision = AppPrecision.HUNDREDTHS,
            nowMillis = timeProvider.currentTimeMillis()
        )

        assertTrue(reportText.contains("MULTI-STOPWATCH RESULTS"))
        assertTrue(reportText.contains("Runner: Runner 1"))
        assertTrue(reportText.contains("Runner: Runner 2"))
        assertTrue(reportText.contains("Best Lap:"))
        assertTrue(reportText.contains("Lap Breakdown"))
    }

    @Test
    fun singleStopwatchSharing_formatsIndividualWatchDetails() {
        viewModel.addStopwatch("Solo Runner")
        val watchId = viewModel.uiState.value.stopwatches[0].id

        viewModel.startStopwatch(watchId)
        timeProvider.advanceTimeBy(3000L)
        viewModel.lapStopwatch(watchId)

        val watch = viewModel.uiState.value.stopwatches[0]
        val singleReport = RaceResultsFormatter.formatSingleStopwatchResults(
            watch = watch,
            precision = AppPrecision.HUNDREDTHS,
            nowMillis = timeProvider.currentTimeMillis()
        )

        assertTrue(singleReport.contains("Solo Runner"))
        assertTrue(singleReport.contains("Total (All Runs):") || singleReport.contains("Total Elapsed Time:"))
        assertTrue(singleReport.contains("Lap 1:"))
        assertTrue(watch.initialStartTimeMillis > 0L)
    }

    @Test
    fun hasRunData_evaluatesTrueOnlyWhenRunDataExists() {
        viewModel.addStopwatch("Clean Watch")
        val emptyWatch = viewModel.uiState.value.stopwatches[0]
        val emptyHasRunData = emptyWatch.runs.any { it.laps.isNotEmpty() || it.accumulatedMillis > 0L || it.endTimeMillis > 0L }
        assertFalse(emptyHasRunData)

        viewModel.startStopwatch(emptyWatch.id)
        timeProvider.advanceTimeBy(1000L)
        viewModel.lapStopwatch(emptyWatch.id)
        val watchWithData = viewModel.uiState.value.stopwatches[0]
        val activeHasRunData = watchWithData.runs.any { it.laps.isNotEmpty() || it.accumulatedMillis > 0L || it.endTimeMillis > 0L }
        assertTrue(activeHasRunData)
    }
}
