package org.ear.watches.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ear.watches.R
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.util.TimeFormatter
import org.ear.watches.ui.theme.WatchesTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull

private object StopwatchTileDefaults {
    val CardShape = RoundedCornerShape(20.dp)
    val CardElevation = 2.dp
    val FocusBorderWidth = 3.5.dp
    val ActionButtonShape = RoundedCornerShape(12.dp)
    val ActionButtonContentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    val ActionButtonIconSize = 18.dp
    val SmallIconSize = 16.dp
    val TileContentPadding = 16.dp
    val SectionSpacing = 10.dp
    val ElementSpacing = 6.dp
}

/**
 * A reusable UI component that displays the state and controls for a single stopwatch.
 * Shows the current elapsed time, lap controls, play/pause buttons, and handles selection
 * state for volume key focus.
 */
@Composable
fun StopwatchTile(
    watch: StopwatchItem,
    timePrecision: AppPrecision,
    tickerFlow: StateFlow<Long>,
    onStart: (String) -> Unit,
    onPause: (String) -> Unit,
    onLap: (String) -> Unit,
    onReset: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (StopwatchItem) -> Unit,
    onOpenDetails: (StopwatchItem) -> Unit,
    onSelectFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
    runningDisplayPrecision: AppPrecision = timePrecision,
    onFullReset: (String) -> Unit = onReset,
    onNameSession: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var showCurrentLapTime by rememberSaveable { mutableStateOf(false) }

    val isFocused = watch.isFocused
    val borderThickness by animateDpAsState(
        targetValue = if (isFocused) StopwatchTileDefaults.FocusBorderWidth else 1.dp,
        label = "borderThickness"
    )

    val cardBackgroundColor = Color(watch.pastelColorHex)
    val darkContentColor = Color(0xFF1F1F1F)

    val currentTick by produceState(initialValue = SystemClock.elapsedRealtimeNanos(), watch.isRunning, tickerFlow) {
        if (watch.isRunning) {
            tickerFlow.collect { value = it }
        }
    }
    val nowNanos = if (watch.isRunning) currentTick else SystemClock.elapsedRealtimeNanos()

    val currentTotalTime = watch.getElapsedMillis(nowNanos)
    val lastLapRecordedTotalTime = watch.currentRunLaps.lastOrNull()?.totalTimeMillis ?: 0L
    val displayedTimeMillis = if (showCurrentLapTime) {
        (currentTotalTime - lastLapRecordedTotalTime).coerceAtLeast(0L)
    } else {
        currentTotalTime
    }
    val subsecondSeparator = stringResource(R.string.subsecond_separator)
    val activeDisplayPrecision = if (watch.isRunning) runningDisplayPrecision else timePrecision
    val formattedTime = TimeFormatter.formatTime(displayedTimeMillis, activeDisplayPrecision, subsecondSeparator)

    val lastLapTime = watch.currentRunLaps.lastOrNull()?.let {
        TimeFormatter.formatTime(it.lapTimeMillis, timePrecision, subsecondSeparator)
    } ?: "-"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isFocused) 8.dp else StopwatchTileDefaults.CardElevation,
                shape = StopwatchTileDefaults.CardShape
            )
            .clickable { onSelectFocus(watch.id) },
        shape = StopwatchTileDefaults.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        border = BorderStroke(
            width = borderThickness,
            color = if (isFocused) Color(0xFF1E88E5) else Color.Black.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StopwatchTileDefaults.TileContentPadding)
        ) {
            // Header Row: Name, Focus Tag, Info Details, Edit & Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit(watch) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFocused) {
                        Surface(
                            color = Color(0xFF1E88E5),
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

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            // Running Time Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(StopwatchTileDefaults.ActionButtonShape)
                    .background(Color.White.copy(alpha = 0.65f))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showCurrentLapTime) {
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

                    Text(
                        text = formattedTime,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = darkContentColor
                        ),
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { showCurrentLapTime = !showCurrentLapTime },
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

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            // Status and Last Lap Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (watch.isRunning) Color(0xFF2E7D32) else Color.Gray
                            )
                    )
                    Spacer(modifier = Modifier.width(StopwatchTileDefaults.ElementSpacing))
                    Text(
                        text = if (watch.isRunning) stringResource(R.string.status_running) else stringResource(R.string.status_stopped),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (watch.isRunning) Color(0xFF1B5E20) else Color.DarkGray
                        )
                    )
                }

                // Last Lap Display & Pace Trend Indicator
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
                                Color(0xFF2E7D32),
                                R.string.cd_pace_faster
                            )
                            delta > 0 -> Triple(
                                "▲ ${TimeFormatter.formatDeltaTime(delta, timePrecision, subsecondSeparator)}",
                                Color(0xFFD32F2F),
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

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            // Action Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start / Stop Toggle Button
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .onStartInstantPressWithLongPressNaming(
                            enabled = true,
                            isRunning = watch.isRunning,
                            onStart = { onStart(watch.id) },
                            onPause = { onPause(watch.id) },
                            onLongPressNameSession = { onNameSession(watch.id) },
                            onLongPressHaptic = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                ) {
                    if (watch.isRunning) {
                        Surface(
                            color = Color(0xFFD32F2F),
                            contentColor = Color.White,
                            shape = StopwatchTileDefaults.ActionButtonShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(StopwatchTileDefaults.ActionButtonContentPadding),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = stringResource(R.string.btn_stop),
                                    modifier = Modifier.size(StopwatchTileDefaults.ActionButtonIconSize)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_stop).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = Color(0xFF2E7D32),
                            contentColor = Color.White,
                            shape = StopwatchTileDefaults.ActionButtonShape,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(StopwatchTileDefaults.ActionButtonContentPadding),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.btn_start),
                                    modifier = Modifier.size(StopwatchTileDefaults.ActionButtonIconSize)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.btn_start).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Lap Button
                Button(
                    onClick = {},
                    enabled = watch.isRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00796B),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB0BEC5),
                        disabledContentColor = Color(0xFF546E7A)
                    ),
                    contentPadding = StopwatchTileDefaults.ActionButtonContentPadding,
                    modifier = Modifier
                        .weight(1f)
                        .onInstantPress(enabled = watch.isRunning) { onLap(watch.id) },
                    shape = StopwatchTileDefaults.ActionButtonShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = stringResource(R.string.btn_lap),
                        modifier = Modifier.size(StopwatchTileDefaults.SmallIconSize)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.btn_lap).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Reset Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(StopwatchTileDefaults.ActionButtonShape)
                        .onResetInstantPress(
                            enabled = !watch.isRunning,
                            onReset = { onReset(watch.id) },
                            onFullReset = { onFullReset(watch.id) },
                            onLongPressHaptic = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        ),
                    color = if (!watch.isRunning) Color(0xFFE65100) else Color(0xFFB0BEC5),
                    contentColor = if (!watch.isRunning) Color.White else Color(0xFF546E7A),
                    shape = StopwatchTileDefaults.ActionButtonShape
                ) {
                    Row(
                        modifier = Modifier.padding(StopwatchTileDefaults.ActionButtonContentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_reset),
                            modifier = Modifier.size(StopwatchTileDefaults.SmallIconSize)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.btn_reset).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.onStartInstantPressWithLongPressNaming(
    enabled: Boolean = true,
    isRunning: Boolean = false,
    onStart: () -> Unit,
    onPause: () -> Unit = {},
    onLongPressNameSession: () -> Unit,
    onLongPressHaptic: () -> Unit = {}
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentIsRunning by rememberUpdatedState(isRunning)
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnLongPressNameSession by rememberUpdatedState(onLongPressNameSession)
    val currentOnLongPressHaptic by rememberUpdatedState(onLongPressHaptic)

    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press || event.changes.any { it.pressed }) {
                    event.changes.forEach { it.consume() }
                    val wasRunning = currentIsRunning
                    if (wasRunning) {
                        currentOnPause()
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    } else {
                        currentOnStart()
                        var isReleased = false
                        val releasedBeforeTimeout = withTimeoutOrNull(500L) {
                            while (true) {
                                val nextEvent = awaitPointerEvent()
                                nextEvent.changes.forEach { it.consume() }
                                if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                    isReleased = true
                                    break
                                }
                            }
                            true
                        }

                        if (releasedBeforeTimeout == null && !isReleased) {
                            currentOnLongPressHaptic()
                            currentOnLongPressNameSession()
                            while (true) {
                                val nextEvent = awaitPointerEvent()
                                nextEvent.changes.forEach { it.consume() }
                                if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Modifier.onInstantPress(
    enabled: Boolean = true,
    onPress: () -> Unit
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnPress by rememberUpdatedState(onPress)
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press) {
                    event.changes.forEach { it.consume() }
                    currentOnPress()
                }
            }
        }
    }
}

@Composable
fun Modifier.onResetInstantPress(
    enabled: Boolean = true,
    onReset: () -> Unit,
    onFullReset: () -> Unit,
    onLongPressHaptic: () -> Unit = {}
): Modifier {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnFullReset by rememberUpdatedState(onFullReset)
    val currentOnLongPressHaptic by rememberUpdatedState(onLongPressHaptic)

    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (!currentEnabled) continue
                if (event.type == PointerEventType.Press || event.changes.any { it.pressed }) {
                    event.changes.forEach { it.consume() }
                    var isReleased = false
                    val releasedBeforeTimeout = withTimeoutOrNull(500L) {
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                isReleased = true
                                break
                            }
                        }
                        true
                    }

                    if (releasedBeforeTimeout != null) {
                        if (isReleased) {
                            currentOnReset()
                        }
                    } else {
                        currentOnLongPressHaptic()
                        currentOnFullReset()
                        while (true) {
                            val nextEvent = awaitPointerEvent()
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.type == PointerEventType.Release || nextEvent.changes.all { !it.pressed }) {
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StopwatchTilePreview() {
    WatchesTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            StopwatchTile(
                watch = StopwatchItem(
                    name = "Runner 1",
                    pastelColorHex = 0xFF90CAF9L,
                    isRunning = true,
                    isFocused = true,
                    runs = listOf(
                        RunSession(
                            runIndex = 1,
                            startTimeNanos = 0L,
                            startTimeMillis = 0L,
                            laps = listOf(
                                LapRecord(1, 12_400_000_000L, 12_400_000_000L)
                            )
                        )
                    )
                ),
                timePrecision = AppPrecision.TENTHS,
                runningDisplayPrecision = AppPrecision.TENTHS,
                tickerFlow = kotlinx.coroutines.flow.MutableStateFlow(15_000_000_000L),
                onStart = {},
                onPause = {},
                onLap = {},
                onReset = {},
                onFullReset = {},
                onDelete = {},
                onEdit = {},
                onOpenDetails = {},
                onSelectFocus = {}
            )
        }
    }
}

