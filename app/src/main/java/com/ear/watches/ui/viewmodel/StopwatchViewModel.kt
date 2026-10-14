package com.ear.watches.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ear.watches.data.model.AppFontSize
import com.ear.watches.data.model.AppLanguage
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import com.ear.watches.data.model.AppThemeMode
import com.ear.watches.data.model.LapRecord
import com.ear.watches.data.model.RunSession
import com.ear.watches.data.model.StopwatchItem
import com.ear.watches.data.model.StopwatchUiState
import com.ear.watches.data.repository.SettingsRepository
import com.ear.watches.data.util.SystemTimeProvider
import com.ear.watches.data.util.TimeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class StopwatchViewModel(
    private val settingsRepository: SettingsRepository? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private val _tickerFlow = MutableStateFlow(timeProvider.currentTimeMillis())
    val tickerFlow: StateFlow<Long> = _tickerFlow.asStateFlow()

    private var tickerJob: Job? = null

    init {
        syncFocusState()
        observeSettings()
    }

    private fun observeSettings() {
        if (settingsRepository != null) {
            viewModelScope.launch(dispatcher) {
                settingsRepository.settingsFlow.collect { newSettings ->
                    _uiState.update { state ->
                        state.copy(settings = newSettings)
                            .applyFocusAndSort()
                    }
                }
            }
        }
    }

    private fun checkTicker() {
        val anyRunning = _uiState.value.stopwatches.any { it.isRunning }
        if (anyRunning) {
            if (tickerJob?.isActive != true) {
                tickerJob = viewModelScope.launch(dispatcher) {
                    while (coroutineContext.isActive) {
                        if (!_uiState.value.stopwatches.any { it.isRunning }) break
                        val delayMillis = when (_uiState.value.settings.runningDisplayPrecision) {
                            AppPrecision.SECONDS -> 500L
                            AppPrecision.TENTHS -> 50L
                            AppPrecision.HUNDREDTHS -> 16L
                        }
                        delay(delayMillis.milliseconds)
                        _tickerFlow.value = timeProvider.currentTimeMillis()
                    }
                }
            }
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    fun calculateExpectedNextLapTime(
        item: StopwatchItem
    ): Long {
        if (item.currentRunLaps.isEmpty()) return 0L
        val lastTotalTime = item.currentRunLaps.last().totalTimeMillis
        val lapCount = item.currentRunLaps.size
        return lastTotalTime + (lastTotalTime / lapCount)
    }

    fun calculateNextFocusedWatchId(
        stopwatches: List<StopwatchItem> = _uiState.value.stopwatches,
        currentFocusedId: String? = _uiState.value.activeFocusedWatchId
    ): String? {
        if (stopwatches.isEmpty()) return null

        val runningWatches = stopwatches.filter { it.isRunning }
        val candidates = runningWatches.ifEmpty { stopwatches }

        val zeroLapCandidates = candidates.filter { it.currentRunLaps.isEmpty() }
        if (zeroLapCandidates.isNotEmpty()) {
            val currentFocused0Lap = zeroLapCandidates.find { it.id == currentFocusedId }
            return (currentFocused0Lap ?: zeroLapCandidates.first()).id
        }

        val bestWatch = candidates.minByOrNull { watch ->
            calculateExpectedNextLapTime(watch)
        }

        return bestWatch?.id
    }

    fun sortStopwatches(
        stopwatches: List<StopwatchItem>,
        autoSortEnabled: Boolean
    ): List<StopwatchItem> {
        if (!autoSortEnabled) return stopwatches
        return stopwatches.sortedWith(
            compareBy<StopwatchItem> { watch ->
                if (watch.currentRunLaps.isEmpty()) 0 else 1
            }.thenBy { watch ->
                if (watch.currentRunLaps.isEmpty()) 0L
                else calculateExpectedNextLapTime(watch)
            }
        )
    }

    private fun StopwatchUiState.applyFocusAndSort(
        newFocusedId: String? = activeFocusedWatchId
    ): StopwatchUiState {
        val determinedFocusedId = newFocusedId
            ?: calculateNextFocusedWatchId(stopwatches, activeFocusedWatchId)

        val sortedList = sortStopwatches(stopwatches, settings.autoSortEnabled)

        val updatedList = sortedList.map { item ->
            item.copy(isFocused = (item.id == determinedFocusedId))
        }

        return copy(
            stopwatches = updatedList,
            activeFocusedWatchId = determinedFocusedId
        )
    }

    fun addStopwatch(defaultNamePrefix: String, name: String? = null, colorHex: Long? = null) {
        _uiState.update { state ->
            val count = state.stopwatches.size
            val defaultName = name?.takeIf { it.isNotBlank() } ?: "$defaultNamePrefix ${count + 1}"
            val color = colorHex ?: StopwatchItem.DEFAULT_PASTEL_COLORS[count % StopwatchItem.DEFAULT_PASTEL_COLORS.size]

            val newItem = StopwatchItem(
                name = defaultName,
                pastelColorHex = color
            )
            val newList = state.stopwatches + newItem
            val newFocusedId = state.activeFocusedWatchId ?: newItem.id

            state.copy(stopwatches = newList, activeFocusedWatchId = newFocusedId)
                .applyFocusAndSort(newFocusedId = newFocusedId)
        }
    }

    fun batchAddStopwatches(count: Int, namePrefix: String) {
        if (count <= 0) return
        _uiState.update { state ->
            val existingCount = state.stopwatches.size
            val newItems = (1..count).map { index ->
                val totalNumber = existingCount + index
                val name = "$namePrefix $totalNumber"
                val color = StopwatchItem.DEFAULT_PASTEL_COLORS[(totalNumber - 1) % StopwatchItem.DEFAULT_PASTEL_COLORS.size]
                StopwatchItem(
                    name = name,
                    pastelColorHex = color
                )
            }
            val newList = state.stopwatches + newItems
            val newFocusedId = state.activeFocusedWatchId ?: newItems.first().id

            state.copy(stopwatches = newList, activeFocusedWatchId = newFocusedId)
                .applyFocusAndSort(newFocusedId = newFocusedId)
        }
    }

    fun deleteStopwatch(id: String) {
        _uiState.update { state ->
            val newList = state.stopwatches.filterNot { it.id == id }
            val focusCandidate = if (state.activeFocusedWatchId == id) null else state.activeFocusedWatchId
            val nextFocusedId = focusCandidate ?: calculateNextFocusedWatchId(newList)

            state.copy(stopwatches = newList, activeFocusedWatchId = nextFocusedId)
                .applyFocusAndSort(newFocusedId = nextFocusedId)
        }
        checkTicker()
    }

    fun updateStopwatch(id: String, newName: String, newColorHex: Long) {
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id) {
                    item.copy(name = newName, pastelColorHex = newColorHex)
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
    }

    fun startStopwatch(id: String) {
        val now = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id && !item.isRunning) {
                    val newRuns = if (item.runs.isEmpty()) {
                        listOf(
                            RunSession(
                                runIndex = 1,
                                startTimeMillis = now
                            )
                        )
                    } else {
                        val lastRun = item.runs.last()
                        if (lastRun.endTimeMillis > 0L) {
                            val runIndex = item.runs.size + 1
                            item.runs + RunSession(
                                runIndex = runIndex,
                                startTimeMillis = now
                            )
                        } else {
                            val updatedLastRun = lastRun.copy(
                                startTimeMillis = now
                            )
                            item.runs.dropLast(1) + updatedLastRun
                        }
                    }
                    item.copy(
                        isRunning = true,
                        runs = newRuns
                    )
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        checkTicker()
    }

    fun pauseStopwatch(id: String) {
        val now = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id && item.isRunning) {
                    val newRuns = if (item.runs.isNotEmpty()) {
                        val activeRun = item.runs.last()
                        val runDuration = (now - activeRun.startTimeMillis).coerceAtLeast(0L)
                        val completedRun = activeRun.copy(
                            endTimeMillis = now,
                            accumulatedMillis = runDuration
                        )
                        item.runs.dropLast(1) + completedRun
                    } else {
                        emptyList()
                    }
                    item.copy(
                        isRunning = false,
                        runs = newRuns
                    )
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        checkTicker()
    }

    fun lapStopwatch(id: String) {
        val now = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id && item.isRunning) {
                    val activeRun = item.activeRun ?: RunSession(
                        runIndex = 1,
                        startTimeMillis = now
                    )
                    val currentRunTime = (now - activeRun.startTimeMillis).coerceAtLeast(0L)
                    val lastLap = activeRun.laps.lastOrNull()
                    val lastTotalTime = lastLap?.totalTimeMillis ?: 0L
                    val lapTime = currentRunTime - lastTotalTime
                    val lapDeltaMillis = lastLap?.let { lapTime - it.lapTimeMillis }
                    val newLap = LapRecord(
                        lapNumber = activeRun.laps.size + 1,
                        lapTimeMillis = lapTime,
                        totalTimeMillis = currentRunTime,
                        lapDeltaMillis = lapDeltaMillis
                    )
                    val updatedLaps = activeRun.laps + newLap
                    val updatedRun = activeRun.copy(laps = updatedLaps)
                    val newRuns = if (item.runs.isNotEmpty()) {
                        item.runs.dropLast(1) + updatedRun
                    } else {
                        listOf(updatedRun)
                    }
                    item.copy(runs = newRuns)
                } else item
            }
            val nextFocusedId = calculateNextFocusedWatchId(newList)
            state.copy(stopwatches = newList, activeFocusedWatchId = nextFocusedId)
                .applyFocusAndSort(newFocusedId = nextFocusedId)
        }
    }

    fun resetStopwatch(id: String) {
        val now = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id) {
                    val finalizedRuns = if (item.runs.isNotEmpty()) {
                        val lastRun = item.runs.last()
                        if (lastRun.endTimeMillis == 0L && lastRun.startTimeMillis > 0L) {
                            val duration = (now - lastRun.startTimeMillis).coerceAtLeast(0L)
                            item.runs.dropLast(1) + lastRun.copy(
                                endTimeMillis = now,
                                accumulatedMillis = duration
                            )
                        } else {
                            item.runs
                        }
                    } else {
                        emptyList()
                    }

                    val preparedRuns = if (finalizedRuns.isNotEmpty() &&
                        finalizedRuns.last().startTimeMillis == 0L &&
                        finalizedRuns.last().endTimeMillis == 0L
                    ) {
                        finalizedRuns
                    } else {
                        finalizedRuns + RunSession(
                            runIndex = finalizedRuns.size + 1,
                            startTimeMillis = 0L,
                            endTimeMillis = 0L,
                            accumulatedMillis = 0L,
                            laps = emptyList()
                        )
                    }

                    item.copy(
                        isRunning = false,
                        runs = preparedRuns
                    )
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        checkTicker()
    }

    fun resetStopwatchFull(id: String) {
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id) {
                    item.copy(
                        isRunning = false,
                        runs = emptyList()
                    )
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        checkTicker()
    }

    fun startAll() {
        val now = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (!item.isRunning) {
                    val newRuns = if (item.runs.isEmpty()) {
                        listOf(
                            RunSession(
                                runIndex = 1,
                                startTimeMillis = now
                            )
                        )
                    } else {
                        val lastRun = item.runs.last()
                        if (lastRun.endTimeMillis > 0L) {
                            val runIndex = item.runs.size + 1
                            item.runs + RunSession(
                                runIndex = runIndex,
                                startTimeMillis = now
                            )
                        } else {
                            val updatedLastRun = lastRun.copy(
                                startTimeMillis = now
                            )
                            item.runs.dropLast(1) + updatedLastRun
                        }
                    }
                    item.copy(
                        isRunning = true,
                        runs = newRuns
                    )
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        checkTicker()
    }

    fun pauseAll() {
        val now = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.isRunning) {
                    val newRuns = if (item.runs.isNotEmpty()) {
                        val activeRun = item.runs.last()
                        val runDuration = (now - activeRun.startTimeMillis).coerceAtLeast(0L)
                        val completedRun = activeRun.copy(
                            endTimeMillis = now,
                            accumulatedMillis = runDuration
                        )
                        item.runs.dropLast(1) + completedRun
                    } else {
                        emptyList()
                    }
                    item.copy(
                        isRunning = false,
                        runs = newRuns
                    )
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        checkTicker()
    }

    fun resetAll() {
        val ids = _uiState.value.stopwatches.map { it.id }
        ids.forEach { id ->
            resetStopwatch(id)
        }
    }

    fun resetAllFull() {
        val ids = _uiState.value.stopwatches.map { it.id }
        ids.forEach { id ->
            resetStopwatchFull(id)
        }
    }

    fun setFocusedWatch(id: String) {
        _uiState.update { state ->
            state.copy(activeFocusedWatchId = id)
                .applyFocusAndSort(newFocusedId = id)
        }
    }

    fun handleVolumeUp() {
        val state = _uiState.value
        val stopwatches = state.stopwatches
        if (stopwatches.none { it.isRunning }) {
            startAll()
            return
        }

        val runningWatches = stopwatches.filter { it.isRunning }
        if (runningWatches.isEmpty()) return

        val targetWatchId = calculateNextFocusedWatchId(stopwatches) ?: runningWatches.first().id

        lapStopwatch(targetWatchId)
    }

    fun handleVolumeDown() {
        val state = _uiState.value
        val stopwatches = state.stopwatches
        if (stopwatches.none { it.isRunning }) {
            resetAll()
            return
        }

        val focusedRunningWatch = stopwatches.find { it.id == state.activeFocusedWatchId && it.isRunning }
        val watchToStop = focusedRunningWatch ?: stopwatches.firstOrNull { it.isRunning }

        if (watchToStop != null) {
            pauseStopwatch(watchToStop.id)
        }
    }

    fun toggleSoundHaptics() {
        val newValue = !_uiState.value.settings.soundAndHapticsEnabled
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(soundAndHapticsEnabled = newValue))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setSoundHapticsEnabled(newValue)
        }
    }

    fun toggleHardwareKeys() {
        val newValue = !_uiState.value.settings.hardwareVolumeKeysEnabled
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(hardwareVolumeKeysEnabled = newValue))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setHardwareKeysEnabled(newValue)
        }
    }

    fun toggleAutoSort() {
        val newValue = !_uiState.value.settings.autoSortEnabled
        _uiState.update { state ->
            val newSettings = state.settings.copy(autoSortEnabled = newValue)
            state.copy(settings = newSettings)
                .applyFocusAndSort()
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setAutoSortEnabled(newValue)
        }
    }

    fun setRunningDisplayPrecision(precision: AppPrecision) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(runningDisplayPrecision = precision))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setRunningDisplayPrecision(precision)
        }
    }

    fun setPrecision(precision: AppPrecision) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(timePrecision = precision))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setTimePrecision(precision)
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(language = language))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setAppLanguage(language)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(themeMode = themeMode))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setThemeMode(themeMode)
        }
    }

    fun setFontSizeScale(fontSizeScale: AppFontSize) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(fontSizeScale = fontSizeScale))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setFontSizeScale(fontSizeScale)
        }
    }

    fun saveSettings(settings: AppSettings) {
        _uiState.update { state ->
            state.copy(settings = settings)
                .applyFocusAndSort()
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.saveSettings(settings)
        }
    }

    private fun syncFocusState() {
        _uiState.update { state ->
            state.applyFocusAndSort()
        }
    }
}
