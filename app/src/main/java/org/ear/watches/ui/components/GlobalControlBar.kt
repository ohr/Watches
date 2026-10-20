package org.ear.watches.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ear.watches.R
import org.ear.watches.data.model.StopwatchUiState
import org.ear.watches.ui.theme.WatchesTheme

private object GlobalControlBarDefaults {
    val ButtonShape = RoundedCornerShape(12.dp)
    val ButtonContentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    val ButtonIconSize = 18.dp
    val HeaderIconSize = 24.dp
    val RowSpacing = 8.dp
    val RowPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)

    val StartButtonColor = Color(0xFF2E7D32)
    val StopButtonColor = Color(0xFFD32F2F)
    val ResetButtonColor = Color(0xFFE65100)
    val DisabledButtonContainerColor = Color(0xFFB0BEC5)
    val DisabledButtonContentColor = Color(0xFF546E7A)

    val ButtonTextStyle: TextStyle
        @Composable get() = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
}

/**
 * A fixed UI bar, typically at the top of the screen, providing macro-level actions
 * that affect all stopwatches simultaneously (e.g., Global Start, Stop, Reset). Also houses
 * the actions for adding new watches, sharing results, and accessing settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalControlBar(
    uiState: StopwatchUiState,
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onResetAll: () -> Unit,
    onFullResetAll: () -> Unit,
    onAddWatch: () -> Unit,
    onBatchAdd: () -> Unit,
    onShareResults: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    onNameAllSessions: () -> Unit = {}
) {
    val hasWatches = uiState.stopwatches.isNotEmpty()
    val anyRunning = uiState.stopwatches.any { it.isRunning }
    val allStopped = uiState.stopwatches.none { it.isRunning }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(GlobalControlBarDefaults.HeaderIconSize)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.action_about)
                        )
                    }
                    IconButton(onClick = onOpenHelp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.HelpOutline,
                            contentDescription = stringResource(R.string.cd_help)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Global Control Buttons in a single scrollable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(GlobalControlBarDefaults.RowPadding),
                horizontalArrangement = Arrangement.spacedBy(GlobalControlBarDefaults.RowSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Watch
                Button(
                    onClick = onAddWatch,
                    shape = GlobalControlBarDefaults.ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = GlobalControlBarDefaults.ButtonContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_add_watch),
                        style = GlobalControlBarDefaults.ButtonTextStyle
                    )
                }

                // Batch Add
                FilledTonalButton(
                    onClick = onBatchAdd,
                    shape = GlobalControlBarDefaults.ButtonShape,
                    contentPadding = GlobalControlBarDefaults.ButtonContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_batch_add),
                        style = GlobalControlBarDefaults.ButtonTextStyle
                    )
                }

                // Start All / Stop All
                val haptic = LocalHapticFeedback.current
                Box(
                    modifier = Modifier.onStartInstantPressWithLongPressNaming(
                        enabled = hasWatches,
                        isRunning = anyRunning,
                        onStart = onStartAll,
                        onPause = onStopAll,
                        onLongPressNameSession = onNameAllSessions,
                        onLongPressHaptic = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                ) {
                    if (anyRunning) {
                        Surface(
                            shape = GlobalControlBarDefaults.ButtonShape,
                            color = GlobalControlBarDefaults.StopButtonColor,
                            contentColor = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(GlobalControlBarDefaults.ButtonContentPadding),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_stop_all),
                                    style = GlobalControlBarDefaults.ButtonTextStyle
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = GlobalControlBarDefaults.ButtonShape,
                            color = if (hasWatches) GlobalControlBarDefaults.StartButtonColor else GlobalControlBarDefaults.DisabledButtonContainerColor,
                            contentColor = if (hasWatches) Color.White else GlobalControlBarDefaults.DisabledButtonContentColor
                        ) {
                            Row(
                                modifier = Modifier.padding(GlobalControlBarDefaults.ButtonContentPadding),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_start_all),
                                    style = GlobalControlBarDefaults.ButtonTextStyle
                                )
                            }
                        }
                    }
                }

                // Reset All Button
                val resetEnabled = hasWatches && allStopped
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.hint_hold_to_reset_all))
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    Surface(
                        modifier = Modifier
                            .clip(GlobalControlBarDefaults.ButtonShape)
                            .onResetInstantPress(
                                enabled = resetEnabled,
                                onReset = onResetAll,
                                onFullReset = onFullResetAll,
                                onLongPressHaptic = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ),
                        color = if (resetEnabled) GlobalControlBarDefaults.ResetButtonColor else GlobalControlBarDefaults.DisabledButtonContainerColor,
                        contentColor = if (resetEnabled) Color.White else GlobalControlBarDefaults.DisabledButtonContentColor,
                        shape = GlobalControlBarDefaults.ButtonShape
                    ) {
                        Row(
                            modifier = Modifier.padding(GlobalControlBarDefaults.ButtonContentPadding),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_reset_all),
                                style = GlobalControlBarDefaults.ButtonTextStyle
                            )
                        }
                    }
                }

                // Share Results
                FilledTonalButton(
                    onClick = onShareResults,
                    shape = GlobalControlBarDefaults.ButtonShape,
                    contentPadding = GlobalControlBarDefaults.ButtonContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.action_share),
                        style = GlobalControlBarDefaults.ButtonTextStyle
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GlobalControlBarPreview() {
    WatchesTheme {
        GlobalControlBar(
            uiState = StopwatchUiState(),
            onStartAll = {},
            onStopAll = {},
            onResetAll = {},
            onFullResetAll = {},
            onAddWatch = {},
            onBatchAdd = {},
            onShareResults = {},
            onOpenSettings = {},
            onOpenHelp = {},
            onOpenAbout = {}
        )
    }
}
