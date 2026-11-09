package org.ear.watches.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.ear.watches.R
import org.ear.watches.data.model.StopwatchUiState
import org.ear.watches.ui.theme.WatchesTheme

private object GlobalControlBarDefaults {
    val ButtonShape = RoundedCornerShape(12.dp)
    val ButtonContentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    val ButtonIconSize = 18.dp
    val ButtonMinHeight = 40.dp
    val HeaderIconSize = 24.dp
    val HeaderIconSizeCompact = 20.dp
    val RowSpacing = 8.dp
    val RowPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    val CompactBreakpoint = 600.dp

    val StartButtonColor = Color(0xFF2E7D32)
    val StopButtonColor = Color(0xFFD32F2F)
    val ResetButtonColor = Color(0xFFBF360C) // >= 4.5:1 against the white label
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
    onNameAllSessions: () -> Unit = {},
    onImportWatches: () -> Unit = {}
) {
    val hasWatches = uiState.stopwatches.isNotEmpty()
    val anyRunning = uiState.stopwatches.any { it.isRunning }
    val allStopped = uiState.stopwatches.none { it.isRunning }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < GlobalControlBarDefaults.CompactBreakpoint
            val showButtonText = !isCompact

            Column(modifier = Modifier.fillMaxWidth()) {
                GlobalControlBarHeader(
                    onShareResults = onShareResults,
                    onImportWatches = onImportWatches,
                    importEnabled = allStopped,
                    onOpenAbout = onOpenAbout,
                    onOpenHelp = onOpenHelp,
                    onOpenSettings = onOpenSettings,
                    isCompact = isCompact
                )

                if (hasWatches) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(GlobalControlBarDefaults.RowPadding),
                        horizontalArrangement = Arrangement.spacedBy(GlobalControlBarDefaults.RowSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AddWatchButton(
                            modifier = Modifier.weight(1f),
                            onClick = onAddWatch,
                            showText = showButtonText
                        )

                        BatchAddButton(
                            modifier = Modifier.weight(1f),
                            onClick = onBatchAdd,
                            showText = showButtonText
                        )

                        GlobalStartStopButton(
                            modifier = Modifier.weight(if (showButtonText) 1.2f else 1f),
                            anyRunning = anyRunning,
                            onStartAll = onStartAll,
                            onStopAll = onStopAll,
                            onNameAllSessions = onNameAllSessions,
                            showText = showButtonText
                        )

                        GlobalResetButton(
                            modifier = Modifier.weight(if (showButtonText) 1.1f else 1f),
                            enabled = allStopped,
                            onResetAll = onResetAll,
                            onFullResetAll = onFullResetAll,
                            showText = showButtonText
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalControlBarHeader(
    onShareResults: () -> Unit,
    onImportWatches: () -> Unit,
    importEnabled: Boolean,
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenSettings: () -> Unit,
    isCompact: Boolean = false
) {
    val actionButtonModifier = if (isCompact) Modifier.size(36.dp) else Modifier

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(
                        if (isCompact) GlobalControlBarDefaults.HeaderIconSizeCompact
                        else GlobalControlBarDefaults.HeaderIconSize
                    )
                )
                Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = if (isCompact) {
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    },
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            IconButton(onClick = onShareResults, modifier = actionButtonModifier) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.action_share)
                )
            }
            IconButton(
                onClick = onImportWatches,
                enabled = importEnabled,
                modifier = actionButtonModifier
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = stringResource(R.string.action_import)
                )
            }
            IconButton(onClick = onOpenAbout, modifier = actionButtonModifier) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.action_about)
                )
            }
            IconButton(onClick = onOpenHelp, modifier = actionButtonModifier) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.HelpOutline,
                    contentDescription = stringResource(R.string.cd_help)
                )
            }
            IconButton(onClick = onOpenSettings, modifier = actionButtonModifier) {
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
}

@Composable
private fun AddWatchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = GlobalControlBarDefaults.ButtonMinHeight),
        shape = GlobalControlBarDefaults.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = GlobalControlBarDefaults.ButtonContentPadding
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = if (showText) null else stringResource(R.string.action_add_watch),
            modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
        )
        if (showText) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.action_add_watch),
                style = GlobalControlBarDefaults.ButtonTextStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BatchAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = GlobalControlBarDefaults.ButtonMinHeight),
        shape = GlobalControlBarDefaults.ButtonShape,
        contentPadding = GlobalControlBarDefaults.ButtonContentPadding
    ) {
        Icon(
            imageVector = Icons.Default.GroupAdd,
            contentDescription = if (showText) null else stringResource(R.string.action_batch_add),
            modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
        )
        if (showText) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.action_batch_add),
                style = GlobalControlBarDefaults.ButtonTextStyle,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GlobalStartStopButton(
    anyRunning: Boolean,
    onStartAll: () -> Unit,
    onStopAll: () -> Unit,
    onNameAllSessions: () -> Unit,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val buttonColor = if (anyRunning) {
        GlobalControlBarDefaults.StopButtonColor
    } else {
        GlobalControlBarDefaults.StartButtonColor
    }
    val icon = if (anyRunning) Icons.Default.Stop else Icons.Default.PlayArrow
    val textRes = if (anyRunning) R.string.action_stop_all else R.string.action_start_all

    Box(
        modifier = modifier
            .clip(GlobalControlBarDefaults.ButtonShape)
            .onStartInstantPressWithLongPressNaming(
                enabled = true,
                isRunning = anyRunning,
                onStart = onStartAll,
                onPause = onStopAll,
                onLongPressNameSession = onNameAllSessions,
                onLongPressHaptic = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = GlobalControlBarDefaults.ButtonMinHeight),
            shape = GlobalControlBarDefaults.ButtonShape,
            color = buttonColor,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(GlobalControlBarDefaults.ButtonContentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (showText) null else stringResource(textRes),
                    modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                )
                if (showText) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(textRes),
                        style = GlobalControlBarDefaults.ButtonTextStyle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalResetButton(
    enabled: Boolean,
    onResetAll: () -> Unit,
    onFullResetAll: () -> Unit,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(GlobalControlBarDefaults.ButtonShape)
            .onResetInstantPress(
                enabled = enabled,
                onReset = onResetAll,
                onFullReset = onFullResetAll,
                onLongPressHaptic = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
    ) {
        TooltipBox(
            modifier = Modifier.fillMaxWidth(),
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
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = GlobalControlBarDefaults.ButtonMinHeight),
                color = if (enabled) GlobalControlBarDefaults.ResetButtonColor else GlobalControlBarDefaults.DisabledButtonContainerColor,
                contentColor = if (enabled) Color.White else GlobalControlBarDefaults.DisabledButtonContentColor,
                shape = GlobalControlBarDefaults.ButtonShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GlobalControlBarDefaults.ButtonContentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = if (showText) null else stringResource(R.string.action_reset_all),
                        modifier = Modifier.size(GlobalControlBarDefaults.ButtonIconSize)
                    )
                    if (showText) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_reset_all),
                            style = GlobalControlBarDefaults.ButtonTextStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Phone (Compact)", widthDp = 360, showBackground = true)
@Preview(name = "Tablet (Expanded)", widthDp = 800, showBackground = true)
@Composable
fun GlobalControlBarPreview() {
    WatchesTheme {
        GlobalControlBar(
            uiState = StopwatchUiState(
                stopwatches = listOf(
                    org.ear.watches.data.model.StopwatchItem(
                        id = "1",
                        name = "Runner 1",
                        pastelColorHex = 0xFFFFCDD2L
                    )
                )
            ),
            onStartAll = {},
            onStopAll = {},
            onResetAll = {},
            onFullResetAll = {},
            onAddWatch = {},
            onBatchAdd = {},
            onShareResults = {},
            onOpenSettings = {},
            onOpenHelp = {},
            onOpenAbout = {},
            onImportWatches = {}
        )
    }
}
