package org.ear.watches.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.model.StopwatchUiState
import org.ear.watches.data.repository.SettingsRepository
import org.ear.watches.data.util.SystemTimeProvider
import org.ear.watches.data.util.TimeProvider

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
                settingsRepository.settingsFlow.collect {
                    _uiState.update { state ->
                        state.copy(settings = it)
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
                tickerJob = viewModelScope.launch(Dispatchers.Default) {
                    while (coroutineContext.isActive) {
                        if (!_uiState.value.stopwatches.any { it.isRunning }) break
                        _uiState.value.settings.runningDisplayPrecision.delayRefresh()
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

        val sortedWatchList = StopwatchItem.sortStopwatches(stopwatches, settings.autoSortOption)
        val updatedWatchList = sortedWatchList.map {
            it.copy(isFocused = (it.id == determinedFocusedId))
        }

        return copy(
            stopwatches = updatedWatchList,
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
        updateWatch(id) {
            it.copy(name = trimmedName, pastelColorHex = newColorHex)
        }
        if (trimmedName.isNotBlank()) {
            addProposedAthleteName(trimmedName)
        }
    }

    fun startStopwatch(id: String, sessionName: String? = null) {
        val nowNanos = timeProvider.currentTimeNanos()
        val nowMillis = timeProvider.currentTimeMillis()
        val trimmedSessionName = sessionName?.trim()
        updateWatch(id) { it.start(nowNanos, nowMillis, trimmedSessionName) }
        if (!trimmedSessionName.isNullOrBlank()) {
            addProposedSessionName(trimmedSessionName)
        }
        checkTicker()
    }

    fun updateActiveRunSessionName(id: String, newName: String) {
        val trimmed = newName.trim()
        updateWatch(id) { it.updateActiveRunSessionName(trimmed) }
        if (trimmed.isNotBlank()) {
            addProposedSessionName(trimmed)
        }
    }

    fun updateAllActiveRunSessionNames(newName: String) {
        val trimmed = newName.trim()
        updateAllWatches { it.updateActiveRunSessionName(trimmed) }
        if (trimmed.isNotBlank()) {
            addProposedSessionName(trimmed)
        }
    }

    fun updateRunSessionName(id: String, runIndex: Int, newName: String) {
        val trimmed = newName.trim()
        updateWatch(id) { it.updateRunSessionName(runIndex, trimmed) }
        if (trimmed.isNotBlank()) {
            addProposedSessionName(trimmed)
        }
    }

    fun pauseStopwatch(id: String) {
        val nowNanos = timeProvider.currentTimeNanos()
        updateWatch(id) { it.pause(nowNanos) }
        checkTicker()
    }

    fun lapStopwatch(id: String) {
        val nowNanos = timeProvider.currentTimeNanos()
        updateWatch(id) { it.lap(nowNanos) }
    }

    fun resetStopwatch(id: String) {
        val nowNanos = timeProvider.currentTimeNanos()
        updateWatch(id) { it.reset(nowNanos) }
        checkTicker()
    }

    fun resetStopwatchFull(id: String) {
        updateWatch(id) { it.resetFull() }
        checkTicker()
    }

    fun startAll(sessionName: String? = null) {
        val nowNanos = timeProvider.currentTimeNanos()
        val nowMillis = timeProvider.currentTimeMillis()
        val trimmedSessionName = sessionName?.trim()
        updateAllWatches { it.start(nowNanos, nowMillis, trimmedSessionName) }
        if (!trimmedSessionName.isNullOrBlank()) {
            addProposedSessionName(trimmedSessionName)
        }
        checkTicker()
    }

    fun pauseAll() {
        val nowNanos = timeProvider.currentTimeNanos()
        updateAllWatches { it.pause(nowNanos) }
        checkTicker()
    }

    fun resetAll() {
        val nowNanos = timeProvider.currentTimeNanos()
        updateAllWatches { it.reset(nowNanos) }
        checkTicker()
    }

    fun resetAllFull() {
        updateAllWatches { it.resetFull() }
        checkTicker()
    }

    fun setFocusedWatch(id: String) {
        _uiState.update {
            it.copy(activeFocusedWatchId = id)
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
        _uiState.update { it.copy(settings = it.settings.copy(soundEnabled = enabled)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setSoundEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(settings = it.settings.copy(hapticsEnabled = enabled)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setHapticsEnabled(enabled)
        }
    }

    fun toggleHardwareKeys() {
        val newValue = !_uiState.value.settings.hardwareVolumeKeysEnabled
        _uiState.update { it.copy(settings = it.settings.copy(hardwareVolumeKeysEnabled = newValue)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setHardwareKeysEnabled(newValue)
        }
    }

    fun setAutoSortOption(option: AppAutoSortOption) {
        _uiState.update {
            val newSettings = it.settings.copy(autoSortOption = option)
            it.copy(settings = newSettings)
                .applyFocusAndSort()
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setAutoSortOption(option)
        }
    }

    fun toggleAutoSort() {
        val nextOption = when (_uiState.value.settings.autoSortOption) {
            AppAutoSortOption.NONE -> AppAutoSortOption.BY_FASTEST_LAP
            AppAutoSortOption.BY_FASTEST_LAP -> AppAutoSortOption.BY_NAME
            AppAutoSortOption.BY_NAME -> AppAutoSortOption.NONE
        }
        setAutoSortOption(nextOption)
    }

    fun reorderStopwatches(fromIndex: Int, toIndex: Int) {
        if (_uiState.value.settings.autoSortOption != AppAutoSortOption.NONE) return
        val currentStopwatches = _uiState.value.stopwatches
        if (fromIndex !in currentStopwatches.indices || toIndex !in currentStopwatches.indices) return
        if (fromIndex == toIndex) return

        _uiState.update { state ->
            if (state.settings.autoSortOption != AppAutoSortOption.NONE) return@update state
            val stopwatches = state.stopwatches
            if (fromIndex !in stopwatches.indices || toIndex !in stopwatches.indices) return@update state

            val newList = stopwatches.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
            state.copy(stopwatches = newList)
        }
    }

    fun toggleKeepScreenOn() {
        val newValue = !_uiState.value.settings.keepScreenOnWhileRunning
        setKeepScreenOnWhileRunning(newValue)
    }

    fun setKeepScreenOnWhileRunning(enabled: Boolean) {
        _uiState.update {
            it.copy(settings = it.settings.copy(keepScreenOnWhileRunning = enabled))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setKeepScreenOnWhileRunning(enabled)
        }
    }

    fun toggleDynamicColor() {
        val newValue = !_uiState.value.settings.useDynamicColor
        setUseDynamicColor(newValue)
    }

    fun setUseDynamicColor(enabled: Boolean) {
        _uiState.update {
            it.copy(settings = it.settings.copy(useDynamicColor = enabled))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setUseDynamicColor(enabled)
        }
    }

    fun resetSettingsToDefault() {
        val defaultSettings = AppSettings()
        _uiState.update {
            it.copy(settings = defaultSettings)
                .applyFocusAndSort()
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.resetSettingsToDefault()
        }
    }

    fun setRunningDisplayPrecision(precision: AppPrecision) {
        _uiState.update { it.copy(settings = it.settings.copy(runningDisplayPrecision = precision)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setRunningDisplayPrecision(precision)
        }
    }

    fun setPrecision(precision: AppPrecision) {
        _uiState.update { it.copy(settings = it.settings.copy(timePrecision = precision)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setTimePrecision(precision)
        }
    }

    fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(settings = it.settings.copy(language = language)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setAppLanguage(language)
        }
    }

    fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { it.copy(settings = it.settings.copy(themeMode = themeMode)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setThemeMode(themeMode)
        }
    }

    fun setFontSizeScale(fontSizeScale: AppFontSize) {
        _uiState.update {
            it.copy(settings = it.settings.copy(fontSizeScale = fontSizeScale))
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setFontSizeScale(fontSizeScale)
        }
    }

    fun addProposedAthleteName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.update {
            if (it.settings.proposedAthleteNames.contains(trimmed)) {
                it
            } else {
                it.copy(
                    settings = it.settings.copy(
                        proposedAthleteNames = it.settings.proposedAthleteNames + trimmed
                    )
                )
            }
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.addProposedAthleteName(trimmed)
        }
    }

    fun removeProposedAthleteName(name: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(
                    proposedAthleteNames = it.settings.proposedAthleteNames - name
                )
            )
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.removeProposedAthleteName(name)
        }
    }

    fun addProposedSessionName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        _uiState.update {
            if (it.settings.proposedSessionNames.contains(trimmed)) {
                it
            } else {
                it.copy(
                    settings = it.settings.copy(
                        proposedSessionNames = it.settings.proposedSessionNames + trimmed
                    )
                )
            }
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.addProposedSessionName(trimmed)
        }
    }

    fun removeProposedSessionName(name: String) {
        _uiState.update {
            it.copy(
                settings = it.settings.copy(
                    proposedSessionNames = it.settings.proposedSessionNames - name
                )
            )
        }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.removeProposedSessionName(name)
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
        _uiState.update { it.applyFocusAndSort() }
    }

    private inline fun updateWatch(
        id: String,
        crossinline transform: (StopwatchItem) -> StopwatchItem
    ) {
        updateWatchesState { list ->
            list.map { item -> if (item.id == id) transform(item) else item }
        }
    }

    private inline fun updateAllWatches(
        crossinline transform: (StopwatchItem) -> StopwatchItem
    ) {
        updateWatchesState { list ->
            list.map { item -> transform(item) }
        }
    }

    private inline fun updateWatchesState(
        crossinline transformList: (List<StopwatchItem>) -> List<StopwatchItem>
    ) {
        _uiState.update { state ->
            val updatedWatches = transformList(state.stopwatches)
            val activeFocusedWatchId = if (state.activeFocusedWatchId == null || !updatedWatches.any { it.id == state.activeFocusedWatchId }) {
                StopwatchItem.calculateNextFocusedWatchId(updatedWatches)
            } else {
                StopwatchItem.calculateNextFocusedWatchId(updatedWatches, state.activeFocusedWatchId)
            }
            val sortedWatchList = StopwatchItem.sortStopwatches(updatedWatches, state.settings.autoSortOption)
            val refreshedWatchList = sortedWatchList.map {
                it.copy(isFocused = (it.id == activeFocusedWatchId))
            }
            state.copy(
                stopwatches = refreshedWatchList,
                activeFocusedWatchId = activeFocusedWatchId
            )
        }
    }
}
