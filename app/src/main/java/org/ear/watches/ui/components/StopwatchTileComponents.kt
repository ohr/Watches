package org.ear.watches.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ear.watches.R
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.util.TimeFormatter

internal object StopwatchTileDefaults {
    val CardShape = RoundedCornerShape(20.dp)
    val CardElevation = 2.dp
    val FocusBorderWidth = 3.5.dp
    val FocusBorderColor = Color(0xFF1976D2)
    val TimeDisplayShape = RoundedCornerShape(12.dp)
    val TimeDisplayPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    val ActionButtonShape = RoundedCornerShape(12.dp)
    val ActionButtonContentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    val ActionButtonIconSize = 18.dp
    val ActionButtonHeight = 40.dp
    val StartButtonColor = Color(0xFF2E7D32)
    val StopButtonColor = Color(0xFFD32F2F)
    val LapButtonColor = Color(0xFF00796B)
    val ResetButtonColor = Color(0xFFE65100)
    val DisabledButtonContainerColor = Color(0xFFB0BEC5)
    val DisabledButtonContentColor = Color(0xFF546E7A)
    val PaceFasterColor = Color(0xFF2E7D32)
    val PaceSlowerColor = Color(0xFFD32F2F)
    val SmallIconSize = 16.dp
    val TilePaddingValues = PaddingValues(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 12.dp)
    val SectionSpacing = 8.dp
    val ElementSpacing = 6.dp
    val TimeTextFontSize = 36.sp

    val ActionButtonTextStyle: TextStyle
        @Composable get() = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
}

@Composable
internal fun StopwatchTileHeader(
    watch: StopwatchItem,
    darkContentColor: Color,
    autoSortOption: AppAutoSortOption,
    onEdit: (StopwatchItem) -> Unit,
    onOpenDetails: (StopwatchItem) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TileTitleGroup(
            watch = watch,
            darkContentColor = darkContentColor,
            autoSortOption = autoSortOption,
            onEdit = onEdit,
            modifier = Modifier.weight(1f)
        )

        TileHeaderActionGroup(
            watch = watch,
            darkContentColor = darkContentColor,
            onOpenDetails = onOpenDetails,
            onDelete = onDelete
        )
    }
}

@Composable
private fun TileTitleGroup(
    watch: StopwatchItem,
    darkContentColor: Color,
    autoSortOption: AppAutoSortOption,
    onEdit: (StopwatchItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable { onEdit(watch) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (autoSortOption == AppAutoSortOption.NONE) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = stringResource(R.string.cd_drag_handle),
                tint = darkContentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(StopwatchTileDefaults.SmallIconSize)
            )
            Spacer(modifier = Modifier.width(StopwatchTileDefaults.ElementSpacing))
        }
        Text(
            text = watch.name,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = darkContentColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(StopwatchTileDefaults.ElementSpacing))
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.cd_edit_watch),
            tint = darkContentColor.copy(alpha = 0.6f),
            modifier = Modifier.size(StopwatchTileDefaults.SmallIconSize)
        )
    }
}

@Composable
private fun TileHeaderActionGroup(
    watch: StopwatchItem,
    darkContentColor: Color,
    onOpenDetails: (StopwatchItem) -> Unit,
    onDelete: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (watch.isFocused) {
            FocusBadge()
            Spacer(modifier = Modifier.width(4.dp))
        }

        IconButton(
            onClick = { onOpenDetails(watch) },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = darkContentColor.copy(alpha = 0.7f)
            ),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.cd_stopwatch_details),
                modifier = Modifier.size(StopwatchTileDefaults.ActionButtonIconSize)
            )
        }

        IconButton(
            onClick = { onDelete(watch.id) },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color(0xFFB71C1C)
            ),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_delete_watch),
                modifier = Modifier.size(StopwatchTileDefaults.ActionButtonIconSize)
            )
        }
    }
}

@Composable
private fun FocusBadge() {
    Surface(
        color = StopwatchTileDefaults.FocusBorderColor,
        contentColor = Color.White,
        shape = StopwatchTileDefaults.ActionButtonShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.badge_focus),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
internal fun StopwatchTimeDisplay(
    formattedTime: String,
    darkContentColor: Color,
    showCurrentLapTime: Boolean,
    onToggleLapDisplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(StopwatchTileDefaults.TimeDisplayShape)
            .background(Color.White.copy(alpha = 0.65f))
            .padding(StopwatchTileDefaults.TimeDisplayPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (showCurrentLapTime) {
                LapTagBadge()
            }

            Text(
                text = formattedTime,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = StopwatchTileDefaults.TimeTextFontSize,
                    color = darkContentColor
                ),
                maxLines = 1
            )
        }

        IconButton(
            onClick = onToggleLapDisplay,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (showCurrentLapTime) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                contentColor = if (showCurrentLapTime) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = stringResource(R.string.cd_toggle_time_display),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LapTagBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = stringResource(R.string.label_lap_tag),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
internal fun StopwatchStatusAndPaceRow(
    watch: StopwatchItem,
    lastLapTime: String,
    darkContentColor: Color,
    timePrecision: AppPrecision,
    subsecondSeparator: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StopwatchStatusBadge(isRunning = watch.isRunning)

        StopwatchLastLapAndPace(
            watch = watch,
            lastLapTime = lastLapTime,
            darkContentColor = darkContentColor,
            timePrecision = timePrecision,
            subsecondSeparator = subsecondSeparator
        )
    }
}

@Composable
private fun StopwatchStatusBadge(isRunning: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) StopwatchTileDefaults.StartButtonColor else Color.Gray
                )
        )
        Spacer(modifier = Modifier.width(StopwatchTileDefaults.ElementSpacing))
        Text(
            text = if (isRunning) stringResource(R.string.status_running) else stringResource(R.string.status_stopped),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isRunning) Color(0xFF1B5E20) else Color.DarkGray
            )
        )
    }
}

@Composable
private fun StopwatchLastLapAndPace(
    watch: StopwatchItem,
    lastLapTime: String,
    darkContentColor: Color,
    timePrecision: AppPrecision,
    subsecondSeparator: String
) {
    val lastLap = watch.currentRunLaps.lastOrNull()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.label_last_lap_format, lastLapTime, watch.currentRunLaps.size),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = darkContentColor.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        )

        lastLap?.lapDeltaMillis?.let { delta ->
            Spacer(modifier = Modifier.width(StopwatchTileDefaults.ElementSpacing))
            val (indicatorText, color, cdRes) = when {
                delta < 0 -> Triple(
                    "▼ ${TimeFormatter.formatDeltaTime(delta, timePrecision, subsecondSeparator)}",
                    StopwatchTileDefaults.PaceFasterColor,
                    R.string.cd_pace_faster
                )
                delta > 0 -> Triple(
                    "▲ ${TimeFormatter.formatDeltaTime(delta, timePrecision, subsecondSeparator)}",
                    StopwatchTileDefaults.PaceSlowerColor,
                    R.string.cd_pace_slower
                )
                else -> Triple(
                    TimeFormatter.formatDeltaTime(delta, timePrecision, subsecondSeparator),
                    Color.Gray,
                    null
                )
            }
            val cdText = cdRes?.let { stringResource(it) }
            Text(
                text = indicatorText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                modifier = if (cdText != null) {
                    Modifier.semantics { contentDescription = cdText }
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
internal fun StopwatchActionControls(
    watch: StopwatchItem,
    onStart: (String) -> Unit,
    onPause: (String) -> Unit,
    onLap: (String) -> Unit,
    onReset: (String) -> Unit,
    onFullReset: (String) -> Unit,
    onNameSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TileStartStopButton(
            watch = watch,
            onStart = onStart,
            onPause = onPause,
            onNameSession = onNameSession,
            modifier = Modifier.weight(1.2f)
        )

        TileLapButton(
            watch = watch,
            onLap = onLap,
            modifier = Modifier.weight(1f)
        )

        TileResetButton(
            watch = watch,
            onReset = onReset,
            onFullReset = onFullReset,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TileActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClickModifier: Modifier,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val effectiveContainerColor = if (enabled) containerColor else StopwatchTileDefaults.DisabledButtonContainerColor
    val effectiveContentColor = if (enabled) contentColor else StopwatchTileDefaults.DisabledButtonContentColor

    Surface(
        modifier = modifier
            .clip(StopwatchTileDefaults.ActionButtonShape)
            .then(if (enabled) onClickModifier else Modifier),
        color = effectiveContainerColor,
        contentColor = effectiveContentColor,
        shape = StopwatchTileDefaults.ActionButtonShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(StopwatchTileDefaults.ActionButtonHeight)
                .padding(StopwatchTileDefaults.ActionButtonContentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(StopwatchTileDefaults.ActionButtonIconSize)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text.uppercase(),
                style = StopwatchTileDefaults.ActionButtonTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TileStartStopButton(
    watch: StopwatchItem,
    onStart: (String) -> Unit,
    onPause: (String) -> Unit,
    onNameSession: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val gestureModifier = Modifier.onStartInstantPressWithLongPressNaming(
        enabled = true,
        isRunning = watch.isRunning,
        onStart = { onStart(watch.id) },
        onPause = { onPause(watch.id) },
        onLongPressNameSession = { onNameSession(watch.id) },
        onLongPressHaptic = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )

    if (watch.isRunning) {
        TileActionButton(
            text = stringResource(R.string.btn_stop),
            icon = Icons.Default.Pause,
            containerColor = StopwatchTileDefaults.StopButtonColor,
            contentColor = Color.White,
            enabled = true,
            onClickModifier = gestureModifier,
            contentDescription = stringResource(R.string.btn_stop),
            modifier = modifier
        )
    } else {
        TileActionButton(
            text = stringResource(R.string.btn_start),
            icon = Icons.Default.PlayArrow,
            containerColor = StopwatchTileDefaults.StartButtonColor,
            contentColor = Color.White,
            enabled = true,
            onClickModifier = gestureModifier,
            contentDescription = stringResource(R.string.btn_start),
            modifier = modifier
        )
    }
}

@Composable
private fun TileLapButton(
    watch: StopwatchItem,
    onLap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gestureModifier = Modifier.onInstantPress(enabled = watch.isRunning) {
        onLap(watch.id)
    }

    TileActionButton(
        text = stringResource(R.string.btn_lap),
        icon = Icons.Default.Flag,
        containerColor = StopwatchTileDefaults.LapButtonColor,
        contentColor = Color.White,
        enabled = watch.isRunning,
        onClickModifier = gestureModifier,
        contentDescription = stringResource(R.string.btn_lap),
        modifier = modifier
    )
}

@Composable
private fun TileResetButton(
    watch: StopwatchItem,
    onReset: (String) -> Unit,
    onFullReset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val gestureModifier = Modifier.onResetInstantPress(
        enabled = !watch.isRunning,
        onReset = { onReset(watch.id) },
        onFullReset = { onFullReset(watch.id) },
        onLongPressHaptic = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    )

    TileActionButton(
        text = stringResource(R.string.btn_reset),
        icon = Icons.Default.Refresh,
        containerColor = StopwatchTileDefaults.ResetButtonColor,
        contentColor = Color.White,
        enabled = !watch.isRunning,
        onClickModifier = gestureModifier,
        contentDescription = stringResource(R.string.btn_reset),
        modifier = modifier
    )
}
