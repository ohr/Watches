package org.ear.watches.viewmodel

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.PersistedStopwatchState
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.repository.StopwatchStateRepository
import org.ear.watches.data.repository.StopwatchStateStore
import org.ear.watches.ui.viewmodel.StopwatchViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

private class FakeStateStore(initial: PersistedStopwatchState? = null) : StopwatchStateStore {
    var stored: PersistedStopwatchState? = initial
    var saveCount = 0
    var loadGate: CompletableDeferred<Unit>? = null

    override suspend fun load(): PersistedStopwatchState? {
        loadGate?.await()
        return stored
    }

    override suspend fun save(state: PersistedStopwatchState) {
        saveCount++
        stored = state
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class StopwatchPersistenceTest {

    @TempDir
    lateinit var tempDir: File

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var timeProvider: TestTimeProvider

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        timeProvider = TestTimeProvider(1_000_000_000L, 1_000L)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(store: StopwatchStateStore) = StopwatchViewModel(
        timeProvider = timeProvider,
        dispatcher = testDispatcher,
        stateStore = store
    )

    private fun advancePastDebounce() {
        testDispatcher.scheduler.advanceTimeBy(StopwatchViewModel.PERSIST_DEBOUNCE_MILLIS + 1)
        testDispatcher.scheduler.runCurrent()
    }

    // --- ViewModel ---

    @Test
    fun changes_arePersistedAfterDebounce_andRestoredIncludingRunningState() {
        val store = FakeStateStore()
        val first = createViewModel(store)
        assertTrue(first.isRestored.value)

        first.batchAddStopwatches(2, "Athlete")
        val id0 = first.uiState.value.stopwatches[0].id
        first.startAll()
        timeProvider.advanceTimeBy(5_000_000_000L)
        first.lapStopwatch(id0)
        advancePastDebounce()

        val saved = store.stored!!
        assertEquals(2, saved.stopwatches.size)
        assertTrue(saved.stopwatches.all { it.isRunning })

        // Simulated process death: a fresh ViewModel restores from the same store.
        val second = createViewModel(store)
        val restored = second.uiState.value
        assertEquals(2, restored.stopwatches.size)
        assertTrue(restored.stopwatches.all { it.isRunning })
        assertEquals(1, restored.stopwatches.first { it.id == id0 }.currentRunLaps.size)
        assertEquals(saved.focusedWatchId, restored.activeFocusedWatchId)

        // Running time continues seamlessly on the same boot.
        timeProvider.advanceTimeBy(2_000_000_000L)
        val now = timeProvider.currentTimeNanos()
        assertEquals(7_000L, restored.stopwatches.first { it.id == id0 }.getElapsedMillis(now))
    }

    @Test
    fun persistNow_savesImmediatelyWithoutWaitingForDebounce() {
        val store = FakeStateStore()
        val viewModel = createViewModel(store)
        viewModel.addStopwatch("Athlete")

        viewModel.persistNow()

        assertEquals(1, store.stored?.stopwatches?.size)
    }

    @Test
    fun nothingIsSavedBeforeRestoreCompletes_andWatchesAddedMeanwhileAreKept() {
        val persisted = PersistedStopwatchState(
            stopwatches = listOf(StopwatchItem(name = "Saved", pastelColorHex = 1L))
        )
        val store = FakeStateStore(persisted).apply { loadGate = CompletableDeferred() }
        val viewModel = createViewModel(store)
        assertFalse(viewModel.isRestored.value)

        viewModel.addStopwatch("Athlete", name = "Early")
        advancePastDebounce()
        viewModel.persistNow()
        assertEquals(0, store.saveCount)

        store.loadGate!!.complete(Unit)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.isRestored.value)
        assertEquals(listOf("Saved", "Early"), viewModel.uiState.value.stopwatches.map { it.name })
    }

    // --- Reboot reconciliation ---

    @Test
    fun reconcile_sameBoot_keepsRunningWatchesUntouched() {
        val running = StopwatchItem(name = "A", pastelColorHex = 1L).start(1_000L, 10_000L)
        val state = PersistedStopwatchState(bootCount = 7, stopwatches = listOf(running))

        assertEquals(state, state.reconcile(currentBootCount = 7, nowMillis = 99_000L))
        assertEquals(state, state.reconcile(currentBootCount = null, nowMillis = 99_000L))
    }

    @Test
    fun reconcile_afterReboot_stopsRunningWatchesUsingWallClock() {
        val running = StopwatchItem(name = "A", pastelColorHex = 1L)
            .start(nowNanos = 500_000_000L, nowMillis = 10_000L)
            .lap(nowNanos = 3_500_000_000L)
        val paused = StopwatchItem(name = "B", pastelColorHex = 2L)
            .start(500_000_000L, 10_000L)
            .pause(2_500_000_000L)
        val state = PersistedStopwatchState(bootCount = 7, stopwatches = listOf(running, paused))

        val reconciled = state.reconcile(currentBootCount = 8, nowMillis = 70_000L)

        val a = reconciled.stopwatches[0]
        assertFalse(a.isRunning)
        assertTrue(a.activeRun!!.isFinished)
        assertEquals(60_000L, a.activeRun!!.accumulatedMillis)
        assertEquals(1, a.currentRunLaps.size)
        assertEquals(paused, reconciled.stopwatches[1])
        assertEquals(8, reconciled.bootCount)
    }

    @Test
    fun reconcile_afterReboot_neverShorterThanLastLap() {
        // Wall clock moved backwards: duration falls back to the last recorded lap total.
        val running = StopwatchItem(name = "A", pastelColorHex = 1L)
            .start(nowNanos = 500_000_000L, nowMillis = 100_000L)
            .lap(nowNanos = 4_500_000_000L)
        val state = PersistedStopwatchState(bootCount = 1, stopwatches = listOf(running))

        val reconciled = state.reconcile(currentBootCount = 2, nowMillis = 50_000L)

        assertEquals(4_000L, reconciled.stopwatches[0].activeRun!!.accumulatedMillis)
    }

    // --- DataStore repository ---

    @Test
    fun repository_roundTripsState_andReconcilesOnBootChange() = runTest {
        var bootCount = 3
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempDir, "stopwatch_state.preferences_pb") }
        )
        val repository = StopwatchStateRepository(dataStore, { bootCount }, timeProvider)
        assertNull(repository.load())

        val running = StopwatchItem(name = "A", pastelColorHex = 1L).start(1_000_000_000L, 1_000L)
        repository.save(PersistedStopwatchState(focusedWatchId = running.id, stopwatches = listOf(running)))

        val sameBoot = repository.load()!!
        assertEquals(3, sameBoot.bootCount)
        assertEquals(running.id, sameBoot.focusedWatchId)
        assertTrue(sameBoot.stopwatches.single().isRunning)

        bootCount = 4
        timeProvider.currentTimeMillis = 31_000L
        val afterReboot = repository.load()!!
        val watch = afterReboot.stopwatches.single()
        assertFalse(watch.isRunning)
        assertEquals(30_000L, watch.activeRun!!.accumulatedMillis)
    }

    @Test
    fun repository_returnsNullForCorruptData() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempDir, "corrupt.preferences_pb") }
        )
        dataStore.edit { it[StopwatchStateRepository.STATE_JSON] = "{not json" }
        val repository = StopwatchStateRepository(dataStore, { 1 }, timeProvider)

        assertNull(repository.load())
    }

    // --- Focus / sorting with staggered starts (#3) and Volume-Up target (#2) ---

    @Test
    fun staggeredStarts_focusAndSortUseAbsoluteExpectedLapTime() {
        val viewModel = createViewModel(FakeStateStore())
        viewModel.batchAddStopwatchesWithNames(listOf("Early", "Late"))
        val early = viewModel.uiState.value.stopwatches.first { it.name == "Early" }.id
        val late = viewModel.uiState.value.stopwatches.first { it.name == "Late" }.id

        // Early starts at t=0 and laps after 10s -> next lap expected at t=20s.
        // Late starts at t=8s and laps after 8s -> next lap expected at t=24s
        // (relative 16s < 20s, but absolutely later than Early).
        viewModel.startStopwatch(early)
        timeProvider.advanceTimeBy(8_000_000_000L)
        viewModel.startStopwatch(late)
        timeProvider.advanceTimeBy(2_000_000_000L)
        viewModel.lapStopwatch(early)
        timeProvider.advanceTimeBy(6_000_000_000L)
        viewModel.lapStopwatch(late)

        assertEquals(early, viewModel.uiState.value.activeFocusedWatchId)

        viewModel.setAutoSortOption(AppAutoSortOption.BY_FASTEST_LAP)
        assertEquals(listOf("Early", "Late"), viewModel.uiState.value.stopwatches.map { it.name })
    }

    @Test
    fun volumeUp_lapsHighlightedZeroLapWatch_notFirstOne() {
        val viewModel = createViewModel(FakeStateStore())
        viewModel.batchAddStopwatches(3, "Athlete")
        viewModel.startAll()
        val third = viewModel.uiState.value.stopwatches[2].id
        viewModel.setFocusedWatch(third)

        timeProvider.advanceTimeBy(1_000_000_000L)
        viewModel.handleVolumeUp()

        val state = viewModel.uiState.value
        assertEquals(1, state.stopwatches.first { it.id == third }.currentRunLaps.size)
        assertEquals(1, state.stopwatches.sumOf { it.currentRunLaps.size })
    }

    @Test
    fun ticker_pausesWhileUiHidden_andResumesWhenVisible() {
        val viewModel = createViewModel(FakeStateStore())
        viewModel.addStopwatch("Athlete")
        viewModel.startAll()

        viewModel.onUiStarted()
        viewModel.onUiStopped()
        val hiddenTick = viewModel.tickerFlow.value
        timeProvider.advanceTimeBy(3_000_000_000L)
        Thread.sleep(150) // Real-time ticker on Dispatchers.Default would have ticked by now.
        assertEquals(hiddenTick, viewModel.tickerFlow.value)

        viewModel.onUiStarted()
        assertEquals(timeProvider.currentTimeNanos(), viewModel.tickerFlow.value)
    }

    @Test
    fun ticker_keepsRunning_whenNewScreenStartsBeforeOldOneStops() {
        // Activity replacement order: new onStart, then old onStop (seen after an app update).
        val viewModel = createViewModel(FakeStateStore())
        viewModel.onUiStarted() // old screen
        viewModel.addStopwatch("Athlete")
        viewModel.startAll()

        viewModel.onUiStarted() // new screen
        viewModel.onUiStopped() // old screen goes away

        val before = viewModel.tickerFlow.value
        timeProvider.advanceTimeBy(1_000_000_000L)
        val deadline = System.currentTimeMillis() + 2_000
        while (viewModel.tickerFlow.value == before && System.currentTimeMillis() < deadline) Thread.sleep(20)
        assertTrue(viewModel.tickerFlow.value > before, "Display ticker stopped although a screen is visible")
    }

    @Test
    fun volumeDownLongPress_isIgnoredWhileWatchesRun() {
        val viewModel = createViewModel(FakeStateStore())
        viewModel.addStopwatch("Athlete")
        viewModel.startAll()

        assertFalse(viewModel.handleVolumeDownLongPress())
        assertTrue(viewModel.uiState.value.stopwatches.single().isRunning)
    }
}
