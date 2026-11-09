package org.ear.watches.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ear.watches.R
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppFontSize
import org.ear.watches.data.model.AppLanguage
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.AppThemeMode
import org.ear.watches.data.model.PersistedStopwatchState
import org.ear.watches.data.model.StopwatchBackupPayload
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.model.StopwatchUiState
import org.ear.watches.data.repository.SettingsRepository
import org.ear.watches.data.repository.StopwatchStateStore
import org.ear.watches.data.util.SystemTimeProvider
import org.ear.watches.data.util.TimeProvider
import org.ear.watches.util.LocaleController
import org.ear.watches.util.StopwatchBackupHelper
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Primary ViewModel for the application, responsible for maintaining the state of all stopwatches,
 * driving the high-frequency UI tick, handling hardware button interactions (volume keys),
 * and communicating with the [SettingsRepository].
 */
class StopwatchViewModel(
    private val settingsRepository: SettingsRepository? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val stateStore: StopwatchStateStore? = null,
    private val persistDebounceMillis: Long = PERSIST_DEBOUNCE_MILLIS,
    private val localeController: LocaleController? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    private val _tickerFlow = MutableStateFlow(timeProvider.currentTimeNanos())
    val tickerFlow: StateFlow<Long> = _tickerFlow.asStateFlow()

    private var tickerJob: Job? = null

    /**
     * Number of started (visible) screens. The high-frequency ticker only runs while at least one
     * is visible, so a background session (kept alive by the foreground service) does not burn CPU
     * for redraws.
     *
     * A counter rather than a flag: when one activity instance replaces another, the new one is
     * started *before* the old one is stopped (e.g. the system relaunching the app after an update).
     * A boolean would end up `false` while the new screen is showing and freeze the display.
     */
    private val startedUiCount = AtomicInteger(0)

    /** Until the UI reports its lifecycle (e.g. in tests), assume it is visible. */
    @Volatile
    private var uiLifecycleTracked = false

    private val isUiVisible: Boolean
        get() = !uiLifecycleTracked || startedUiCount.get() > 0

    /** Call from each screen's `onStart`. */
    fun onUiStarted() {
        uiLifecycleTracked = true
        startedUiCount.incrementAndGet()
        _tickerFlow.value = timeProvider.currentTimeNanos()
        checkTicker()
    }

    /** Call from each screen's `onStop`. */
    fun onUiStopped() {
        startedUiCount.updateAndGet { (it - 1).coerceAtLeast(0) }
        checkTicker()
    }

    private val _isRestored = MutableStateFlow(stateStore == null)

    /** `true` once persisted stopwatches have been restored (immediately if persistence is disabled). */
    val isRestored: StateFlow<Boolean> = _isRestored.asStateFlow()

    init {
        syncFocusState()
        observeSettings()
        stateStore?.let { store ->
            viewModelScope.launch(dispatcher) {
                restorePersistedState(store)
                _isRestored.value = true
                checkTicker()
                observeAndPersist(store) // Never completes: keeps saving changes.
            }
        }
    }

    private suspend fun restorePersistedState(store: StopwatchStateStore) {
        val restored = store.load() ?: return
        if (restored.stopwatches.isEmpty()) return
        _uiState.update { state ->
            // Keep anything the user created while the restore was in flight.
            val restoredIds = restored.stopwatches.mapTo(HashSet()) { it.id }
            val combined = restored.stopwatches + state.stopwatches.filterNot { it.id in restoredIds }
            val focusedId = restored.focusedWatchId?.takeIf { id -> combined.any { it.id == id } }
                ?: state.activeFocusedWatchId
            state.copy(stopwatches = combined, activeFocusedWatchId = focusedId)
                .applyFocusAndSort(newFocusedId = focusedId)
        }
    }

    private fun StopwatchUiState.toPersistedState() = PersistedStopwatchState(
        focusedWatchId = activeFocusedWatchId,
        stopwatches = stopwatches
    )

    @OptIn(FlowPreview::class)
    private suspend fun observeAndPersist(store: StopwatchStateStore) {
        _uiState
            .map { it.toPersistedState() }
            .distinctUntilChanged()
            .debounce(persistDebounceMillis)
            .collect { store.save(it) }
    }

    /**
     * Saves the current timing state immediately, bypassing the debounce.
     * Call when the app goes to the background, where the process may be killed at any time.
     */
    fun persistNow() {
        val store = stateStore ?: return
        if (!_isRestored.value) return
        val snapshot = _uiState.value.toPersistedState()
        viewModelScope.launch(dispatcher) { store.save(snapshot) }
    }

    private fun observeSettings() {
        if (settingsRepository != null) {
            viewModelScope.launch(dispatcher) {
                settingsRepository.settingsFlow.collect { stored ->
                    val language = localeController?.resolve(stored.language) ?: stored.language
                    _uiState.update { state ->
                        state.copy(settings = stored.copy(language = language))
                            .applyFocusAndSort()
                    }
                    if (language != stored.language) settingsRepository.setAppLanguage(language)
                }
            }
        }
    }

    private fun checkTicker() {
        val anyRunning = _uiState.value.stopwatches.any { watch -> watch.isRunning }
        if (anyRunning && isUiVisible) {
            if (tickerJob?.isActive != true) {
                tickerJob = viewModelScope.launch(Dispatchers.Default) {
                    while (coroutineContext.isActive) {
                        if (!isUiVisible || !_uiState.value.stopwatches.any { it.isRunning }) break
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

        return copy(
            stopwatches = StopwatchItem.sortStopwatches(stopwatches, settings.autoSortOption),
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

        // Lap the highlighted watch if it is running, otherwise the predicted next one.
        targetWatchId(state)?.let { lapStopwatch(it) }
    }

    /**
     * The running watch that single-button controls (volume keys, notification actions) act on,
     * or `null` if nothing is running. See [StopwatchItem.resolveTargetWatchId].
     */
    fun targetWatchId(state: StopwatchUiState = _uiState.value): String? =
        StopwatchItem.resolveTargetWatchId(state.stopwatches, state.activeFocusedWatchId)

    /**
     * Short press of Volume-Down: stops the target watch (see [targetWatchId]).
     *
     * Resetting is deliberately not done here (see [handleVolumeDownLongPress]) so a single
     * accidental press can never wipe the timing data.
     *
     * @return `true` if a watch was stopped, `false` if nothing was running.
     */
    fun handleVolumeDown(): Boolean {
        val watchToStop = targetWatchId() ?: return false
        pauseStopwatch(watchToStop)
        return true
    }

    /**
     * Long press of Volume-Down while nothing is running: resets all watches (history is kept).
     *
     * @return `true` if the reset was performed, `false` if a watch is still running.
     */
    fun handleVolumeDownLongPress(): Boolean {
        if (_uiState.value.stopwatches.any { it.isRunning }) return false
        resetAll()
        return true
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

    fun toggleRunningNotification() {
        val enabled = !_uiState.value.settings.showRunningNotification
        _uiState.update { it.copy(settings = it.settings.copy(showRunningNotification = enabled)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setShowRunningNotification(enabled)
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
        localeController?.apply(language)
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

    /**
     * Re-reads the applied language, e.g. after the user changed it in the Android 13+ system
     * settings (which recreates the activity but keeps this app-scoped ViewModel).
     */
    fun refreshLanguage() {
        val controller = localeController ?: return
        val current = controller.current()
        if (current == _uiState.value.settings.language) return
        _uiState.update { it.copy(settings = it.settings.copy(language = current)) }
        viewModelScope.launch(dispatcher) {
            settingsRepository?.setAppLanguage(current)
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

    fun setPendingImport(payload: StopwatchBackupPayload) {
        _uiState.update { it.copy(pendingImportPayload = payload) }
    }

    fun dismissPendingImport() {
        _uiState.update { it.copy(pendingImportPayload = null) }
    }

    fun confirmImportReplace() {
        val payload = _uiState.value.pendingImportPayload ?: return
        val importedWatches = payload.stopwatches.map { watch ->
            watch.copy(
                id = UUID.randomUUID().toString(),
                isRunning = false
            )
        }
        val newFocusedId = StopwatchItem.calculateNextFocusedWatchId(importedWatches)
        _uiState.update { state ->
            state.copy(
                stopwatches = importedWatches,
                activeFocusedWatchId = newFocusedId,
                pendingImportPayload = null
            ).applyFocusAndSort(newFocusedId = newFocusedId)
        }
        checkTicker()
    }

    fun confirmImportAppend() {
        val payload = _uiState.value.pendingImportPayload ?: return
        if (payload.stopwatches.isEmpty()) {
            dismissPendingImport()
            return
        }
        val remapped = payload.stopwatches.map { watch ->
            watch.copy(
                id = UUID.randomUUID().toString(),
                isRunning = false
            )
        }
        _uiState.update { state ->
            val combined = state.stopwatches + remapped
            val newFocusedId = state.activeFocusedWatchId ?: StopwatchItem.calculateNextFocusedWatchId(combined)
            state.copy(
                stopwatches = combined,
                activeFocusedWatchId = newFocusedId,
                pendingImportPayload = null
            ).applyFocusAndSort(newFocusedId = newFocusedId)
        }
        checkTicker()
    }

    fun loadBackupFromUri(
        context: Context,
        uri: Uri,
        onSuccess: (count: Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Decide between "replace silently" and "ask" only after persisted watches are back.
            // (Not the restore coroutine itself: it keeps running to persist later changes.)
            _isRestored.first { it }
            val result = StopwatchBackupHelper.parseBackupFromUri(context, uri)
            result.onSuccess { payload ->
                withContext(dispatcher) {
                    if (payload.stopwatches.isEmpty()) {
                        onError(context.getString(R.string.import_empty_toast))
                    } else if (_uiState.value.stopwatches.isEmpty()) {
                        setPendingImport(payload)
                        confirmImportReplace()
                        onSuccess(payload.stopwatches.size)
                    } else {
                        setPendingImport(payload)
                    }
                }
            }.onFailure { error ->
                withContext(dispatcher) {
                    onError(error.localizedMessage ?: context.getString(R.string.import_error_invalid_file))
                }
            }
        }
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
            state.copy(
                stopwatches = StopwatchItem.sortStopwatches(updatedWatches, state.settings.autoSortOption),
                activeFocusedWatchId = activeFocusedWatchId
            )
        }
    }

    companion object {
        const val PERSIST_DEBOUNCE_MILLIS = 300L
    }
}
