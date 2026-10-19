package org.ear.watches.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppThemeMode
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.model.StopwatchUiState
import org.ear.watches.data.repository.SettingsRepository
import org.ear.watches.data.util.SystemTimeProvider
import org.ear.watches.data.util.TimeProvider
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

/**
 * Primary ViewModel for the application, responsible for maintaining the state of all stopwatches,
 * driving the high-frequency UI tick, handling hardware button interactions (volume keys),
 * and communicating with the [SettingsRepository].
 */
class StopwatchViewModel(
    private val settingsRepository: SettingsRepository? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private val _tickerFlow = MutableStateFlow(timeProvider.currentTimeNanos())
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
                        val delayMillis = _uiState.value.settings.runningDisplayPrecision.refreshMillis
                        delay(delayMillis.milliseconds)
                        _tickerFlow.value = timeProvider.currentTimeNanos()
                    }
                }
            }
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun StopwatchUiState.applyFocusAndSort(
        newFocusedId: String? = activeFocusedWatchId
    ): StopwatchUiState {
        val determinedFocusedId = newFocusedId
            ?: StopwatchItem.calculateNextFocusedWatchId(stopwatches, activeFocusedWatchId)

        val sortedList = if (settings.autoSortEnabled) StopwatchItem.sortStopwatches(stopwatches) else stopwatches

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

    fun batchAddStopwatchesWithNames(names: List<String>) {
        if (names.isEmpty()) return
        _uiState.update { state ->
            val existingCount = state.stopwatches.size
            val newItems = names.mapIndexed { index, name ->
                val totalNumber = existingCount + index
                val color = StopwatchItem.DEFAULT_PASTEL_COLORS[totalNumber % StopwatchItem.DEFAULT_PASTEL_COLORS.size]
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
            val nextFocusedId = focusCandidate ?: StopwatchItem.calculateNextFocusedWatchId(newList)

            state.copy(stopwatches = newList, activeFocusedWatchId = nextFocusedId)
                .applyFocusAndSort(newFocusedId = nextFocusedId)
        }
        checkTicker()
    }

    fun updateStopwatch(id: String, newName: String, newColorHex: Long) {
        val trimmedName = newName.trim()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id) {
                    item.copy(name = trimmedName, pastelColorHex = newColorHex)
                } else item
            }
            state.copy(stopwatches = newList)
                .applyFocusAndSort()
        }
        if (trimmedName.isNotBlank()) {
            addProposedAthleteName(trimmedName)
        }
    }

    fun startStopwatch(id: String) {
        val nowNanos = timeProvider.currentTimeNanos()
        val nowMillis = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id && !item.isRunning) {
                    val newRuns = if (item.runs.isEmpty()) {
                        listOf(
                            RunSession(
                                runIndex = 1,
                                startTimeNanos = nowNanos,
                                startTimeMillis = nowMillis
                            )
                        )
                    } else {
                        val lastRun = item.runs.last()
                        if (lastRun.endTimeNanos > 0L) {
                            val runIndex = item.runs.size + 1
                            item.runs + RunSession(
                                runIndex = runIndex,
                                startTimeNanos = nowNanos,
                                startTimeMillis = nowMillis
                            )
                        } else {
                            val updatedLastRun = lastRun.copy(
                                startTimeNanos = nowNanos,
                                startTimeMillis = if (lastRun.startTimeMillis == 0L) nowMillis else lastRun.startTimeMillis
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
        val nowNanos = timeProvider.currentTimeNanos()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id && item.isRunning) {
                    val newRuns = if (item.runs.isNotEmpty()) {
                        val activeRun = item.runs.last()
                        val runDuration = (nowNanos - activeRun.startTimeNanos).coerceAtLeast(0L)
                        val completedRun = activeRun.copy(
                            endTimeNanos = nowNanos,
                            accumulatedNanos = runDuration
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
        val nowNanos = timeProvider.currentTimeNanos()
        val nowMillis = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id && item.isRunning) {
                    val activeRun = item.activeRun ?: RunSession(
                        runIndex = 1,
                        startTimeNanos = nowNanos,
                        startTimeMillis = nowMillis
                    )
                    val currentRunTime = (nowNanos - activeRun.startTimeNanos).coerceAtLeast(0L)
                    val lastLap = activeRun.laps.lastOrNull()
                    val lastTotalTime = lastLap?.totalTimeNanos ?: 0L
                    val lapTime = currentRunTime - lastTotalTime
                    val lapDeltaNanos = lastLap?.let { lapTime - it.lapTimeNanos }
                    val newLap = LapRecord(
                        lapNumber = activeRun.laps.size + 1,
                        lapTimeNanos = lapTime,
                        totalTimeNanos = currentRunTime,
                        lapDeltaNanos = lapDeltaNanos
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
            val nextFocusedId = StopwatchItem.calculateNextFocusedWatchId(newList)
            state.copy(stopwatches = newList, activeFocusedWatchId = nextFocusedId)
                .applyFocusAndSort(newFocusedId = nextFocusedId)
        }
    }

    fun resetStopwatch(id: String) {
        val nowNanos = timeProvider.currentTimeNanos()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.id == id) {
                    val finalizedRuns = if (item.runs.isNotEmpty()) {
                        val lastRun = item.runs.last()
                        if (lastRun.endTimeNanos == 0L && lastRun.startTimeNanos > 0L) {
                            val duration = (nowNanos - lastRun.startTimeNanos).coerceAtLeast(0L)
                            item.runs.dropLast(1) + lastRun.copy(
                                endTimeNanos = nowNanos,
                                accumulatedNanos = duration
                            )
                        } else {
                            item.runs
                        }
                    } else {
                        emptyList()
                    }

                    val preparedRuns = if (finalizedRuns.isNotEmpty() &&
                        finalizedRuns.last().startTimeNanos == 0L &&
                        finalizedRuns.last().endTimeNanos == 0L
                    ) {
                        finalizedRuns
                    } else {
                        finalizedRuns + RunSession(
                            runIndex = finalizedRuns.size + 1,
                            startTimeNanos = 0L,
                            endTimeNanos = 0L,
                            accumulatedNanos = 0L,
                            startTimeMillis = 0L,
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
        val nowNanos = timeProvider.currentTimeNanos()
        val nowMillis = timeProvider.currentTimeMillis()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (!item.isRunning) {
                    val newRuns = if (item.runs.isEmpty()) {
                        listOf(
                            RunSession(
                                runIndex = 1,
                                startTimeNanos = nowNanos,
                                startTimeMillis = nowMillis
                            )
                        )
                    } else {
                        val lastRun = item.runs.last()
                        if (lastRun.endTimeNanos > 0L) {
                            val runIndex = item.runs.size + 1
                            item.runs + RunSession(
                                runIndex = runIndex,
                                startTimeNanos = nowNanos,
                                startTimeMillis = nowMillis
                            )
                        } else {
                            val updatedLastRun = lastRun.copy(
                                startTimeNanos = nowNanos,
                                startTimeMillis = if (lastRun.startTimeMillis == 0L) nowMillis else lastRun.startTimeMillis
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
        val nowNanos = timeProvider.currentTimeNanos()
        _uiState.update { state ->
            val newList = state.stopwatches.map { item ->
                if (item.isRunning) {
                    val newRuns = if (item.runs.isNotEmpty()) {
                        val activeRun = item.runs.last()
                        val runDuration = (nowNanos - activeRun.startTimeNanos).coerceAtLeast(0L)
                        val completedRun = activeRun.copy(
                            endTimeNanos = nowNanos,
                            accumulatedNanos = runDuration
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

        val targetWatchId = StopwatchItem.calculateNextFocusedWatchId(stopwatches) ?: runningWatches.first().id

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

    fun toggleSound() {
        val newValue = !_uiState.value.settings.soundEnabled
        setSoundEnabled(newValue)
    }

    fun toggleHaptics() {
        val newValue = !_uiState.value.settings.hapticsEnabled
        setHapticsEnabled(newValue)
    }

    fun setSoundEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(soundEnabled = enabled))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setSoundEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(hapticsEnabled = enabled))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setHapticsEnabled(enabled)
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

    fun toggleKeepScreenOn() {
        val newValue = !_uiState.value.settings.keepScreenOnWhileRunning
        setKeepScreenOnWhileRunning(newValue)
    }

    fun setKeepScreenOnWhileRunning(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(settings = state.settings.copy(keepScreenOnWhileRunning = enabled))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setKeepScreenOnWhileRunning(enabled)
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

    fun addProposedAthleteName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            if (state.settings.proposedAthleteNames.contains(trimmed)) {
                state
            } else {
                state.copy(
                    settings = state.settings.copy(
                        proposedAthleteNames = state.settings.proposedAthleteNames + trimmed
                    )
                )
            }
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.addProposedAthleteName(trimmed)
        }
    }

    fun removeProposedAthleteName(name: String) {
        _uiState.update { state ->
            state.copy(
                settings = state.settings.copy(
                    proposedAthleteNames = state.settings.proposedAthleteNames - name
                )
            )
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.removeProposedAthleteName(name)
        }
    }

    fun dismissHelpDialog() {
        _uiState.update { it.copy(showHelpDialog = false) }
    }

    fun openAboutDialog() {
        _uiState.update { it.copy(showAboutDialog = true) }
    }

    fun dismissAboutDialog() {
        _uiState.update { it.copy(showAboutDialog = false) }
    }

    private fun syncFocusState() {
        _uiState.update { state ->
            state.applyFocusAndSort()
        }
    }
}
