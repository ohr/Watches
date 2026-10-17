package org.ear.watches.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.ear.watches.R
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.ui.components.GlobalControlBar
import org.ear.watches.ui.components.StopwatchTile
import org.ear.watches.ui.dialogs.AboutDialog
import org.ear.watches.ui.dialogs.BatchAddDialog
import org.ear.watches.ui.dialogs.EditWatchDialog
import org.ear.watches.ui.dialogs.HelpDialog
import org.ear.watches.ui.dialogs.SettingsDialog
import org.ear.watches.ui.dialogs.StopwatchDetailModal
import org.ear.watches.ui.viewmodel.StopwatchViewModel
import org.ear.watches.util.FeedbackManager
import org.ear.watches.util.RaceResultsFormatter
import org.ear.watches.util.shareRaceResults

/**
 * The root Jetpack Compose screen that orchestrates the layout of the application.
 * Contains the [GlobalControlBar], the grid of [StopwatchTile]s, and manages the visibility of
 * various overlay dialogs (Settings, Add/Edit Watch, Reports).
 */
@Composable
fun MainStopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
    feedbackManager: FeedbackManager? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val defaultPrefix = stringResource(R.string.default_athlete_prefix)

    val haptics = feedbackManager ?: remember(context) { FeedbackManager(context) }

    var editingWatchItem by remember { mutableStateOf<StopwatchItem?>(null) }
    var detailWatchItem by remember { mutableStateOf<StopwatchItem?>(null) }
    var showBatchAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val lazyGridState = rememberLazyGridState()

    // Auto-Scroll Grid to Focused Tile
    LaunchedEffect(uiState.activeFocusedWatchId) {
        val focusedId = uiState.activeFocusedWatchId
        if (focusedId != null) {
            val index = uiState.stopwatches.indexOfFirst { it.id == focusedId }
            if (index >= 0) {
                lazyGridState.animateScrollToItem(index)
            }
        }
    }

    val currentDetailWatch = detailWatchItem?.let { item ->
        uiState.stopwatches.find { it.id == item.id } ?: item
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            GlobalControlBar(
                uiState = uiState,
                onStartAll = {
                    viewModel.startAll()
                    haptics.playStart(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                },
                onStopAll = {
                    viewModel.pauseAll()
                    haptics.playStop(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                },
                onResetAll = {
                    viewModel.resetAll()
                    haptics.playReset(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                },
                onFullResetAll = {
                    viewModel.resetAllFull()
                    haptics.playReset(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                },
                onAddWatch = { viewModel.addStopwatch(defaultNamePrefix = defaultPrefix) },
                onBatchAdd = { showBatchAddDialog = true },
                onShareResults = {
                    val reportText = RaceResultsFormatter.formatRaceResults(
                        context = context,
                        stopwatches = uiState.stopwatches,
                        precision = uiState.settings.timePrecision
                    )
                    shareRaceResults(context, reportText)
                },
                onOpenSettings = { showSettingsDialog = true },
                onOpenHelp = { showHelpDialog = true },
                onOpenAbout = { showAboutDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.stopwatches.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_state_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_state_message),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = { viewModel.addStopwatch(defaultNamePrefix = defaultPrefix) },
                                    modifier = Modifier.fillMaxWidth(0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_add_watch),
                                        maxLines = 1
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { showBatchAddDialog = true },
                                    modifier = Modifier.fillMaxWidth(0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GroupAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.action_batch_add),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Adaptive Tile Grid
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.stopwatches,
                        key = { it.id }
                    ) { watch ->
                        StopwatchTile(
                            watch = watch,
                            timePrecision = uiState.settings.timePrecision,
                            runningDisplayPrecision = uiState.settings.runningDisplayPrecision,
                            tickerFlow = viewModel.tickerFlow,
                            onStart = { id ->
                                viewModel.startStopwatch(id)
                                haptics.playStart(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                            },
                            onPause = { id ->
                                viewModel.pauseStopwatch(id)
                                haptics.playStop(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                            },
                            onLap = { id ->
                                viewModel.lapStopwatch(id)
                                haptics.playLap(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                            },
                            onReset = { id ->
                                viewModel.resetStopwatch(id)
                                haptics.playReset(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                            },
                            onFullReset = { id ->
                                viewModel.resetStopwatchFull(id)
                                haptics.playReset(uiState.settings.soundEnabled, uiState.settings.hapticsEnabled)
                            },
                            onDelete = { viewModel.deleteStopwatch(it) },
                            onEdit = { editingWatchItem = it },
                            onOpenDetails = { detailWatchItem = it },
                            onSelectFocus = { viewModel.setFocusedWatch(it) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    currentDetailWatch?.let { watch ->
        StopwatchDetailModal(
            watch = watch,
            settings = uiState.settings,
            tickerFlow = viewModel.tickerFlow,
            onDeleteRuns = { watchId -> viewModel.resetStopwatchFull(watchId) },
            onDismiss = { detailWatchItem = null }
        )
    }

    editingWatchItem?.let { watch ->
        EditWatchDialog(
            watch = watch,
            onConfirm = { id, newName, newColorHex ->
                viewModel.updateStopwatch(id, newName, newColorHex)
                editingWatchItem = null
            },
            onDismiss = { editingWatchItem = null }
        )
    }

    if (showBatchAddDialog) {
        BatchAddDialog(
            onConfirm = { count, prefix ->
                viewModel.batchAddStopwatches(count, prefix)
                showBatchAddDialog = false
            },
            onDismiss = { showBatchAddDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = uiState.settings,
            onToggleSound = { viewModel.toggleSound() },
            onToggleHaptics = { viewModel.toggleHaptics() },
            onToggleHardwareKeys = { viewModel.toggleHardwareKeys() },
            onToggleAutoSort = { viewModel.toggleAutoSort() },
            onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn() },
            onSetPrecision = { viewModel.setPrecision(it) },
            onSetRunningDisplayPrecision = { viewModel.setRunningDisplayPrecision(it) },
            onLanguageSelected = { viewModel.setLanguage(it) },
            onSetThemeMode = { viewModel.setThemeMode(it) },
            onSetFontSizeScale = { viewModel.setFontSizeScale(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showHelpDialog || uiState.showHelpDialog) {
        HelpDialog(
            onDismiss = {
                showHelpDialog = false
                viewModel.dismissHelpDialog()
            }
        )
    }

    if (showAboutDialog || uiState.showAboutDialog) {
        AboutDialog(
            onDismiss = {
                showAboutDialog = false
                viewModel.dismissAboutDialog()
            }
        )
    }
}
