package com.ear.watches.ui.components

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ear.watches.R
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.LapRecord
import com.ear.watches.data.model.RunSession
import com.ear.watches.data.model.StopwatchItem
import com.ear.watches.data.util.TimeFormatter
import com.ear.watches.ui.theme.WatchesTheme

@Composable
fun StopwatchTile(
    watch: StopwatchItem,
    precision: AppPrecision,
    nowMillis: Long,
    onStart: (String) -> Unit,
    onPause: (String) -> Unit,
    onLap: (String) -> Unit,
    onReset: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (StopwatchItem) -> Unit,
    onOpenDetails: (StopwatchItem) -> Unit,
    onSelectFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
    onFullReset: (String) -> Unit = onReset
) {
    val haptic = LocalHapticFeedback.current
    var showCurrentLapTime by rememberSaveable { mutableStateOf(false) }

    val isFocused = watch.isFocused
    val borderThickness by animateDpAsState(
        targetValue = if (isFocused) 3.5.dp else 1.dp,
        label = "borderThickness"
    )

    val cardBackgroundColor = Color(watch.pastelColorHex)
    val darkContentColor = Color(0xFF1F1F1F)

    val currentTotalTime = watch.getElapsedMillis(nowMillis)
    val lastLapRecordedTotalTime = watch.currentRunLaps.lastOrNull()?.totalTimeMillis ?: 0L
    val displayedTimeMillis = if (showCurrentLapTime) {
        (currentTotalTime - lastLapRecordedTotalTime).coerceAtLeast(0L)
    } else {
        currentTotalTime
    }
    val subsecondSeparator = stringResource(R.string.subsecond_separator)
    val formattedTime = TimeFormatter.formatTime(displayedTimeMillis, precision, subsecondSeparator)

    val lastLapTime = watch.currentRunLaps.lastOrNull()?.let {
        TimeFormatter.formatTime(it.lapTimeMillis, precision, subsecondSeparator)
    } ?: "-"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isFocused) 8.dp else 3.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelectFocus(watch.id) },
        shape = RoundedCornerShape(20.dp),
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
                .padding(16.dp)
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.cd_edit_watch),
                        tint = darkContentColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFocused) {
                        Surface(
                            color = Color(0xFF1E88E5),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(12.dp)
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
                            modifier = Modifier.size(18.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Running Time Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
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

            Spacer(modifier = Modifier.height(10.dp))

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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (watch.isRunning) stringResource(R.string.status_running) else stringResource(R.string.status_stopped),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (watch.isRunning) Color(0xFF1B5E20) else Color.DarkGray
                        )
                    )
                }

                // Last Lap Display
                Text(
                    text = stringResource(R.string.label_last_lap_format, lastLapTime, watch.currentRunLaps.size),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = darkContentColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start / Stop Toggle Button
                if (watch.isRunning) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFB0BEC5),
                            disabledContentColor = Color(0xFF546E7A)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .onInstantPress(enabled = true) { onPause(watch.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = stringResource(R.string.btn_stop),
                            modifier = Modifier.size(18.dp)
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
                } else {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFB0BEC5),
                            disabledContentColor = Color(0xFF546E7A)
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .onInstantPress(enabled = true) { onStart(watch.id) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.btn_start),
                            modifier = Modifier.size(18.dp)
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
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .onInstantPress(enabled = watch.isRunning) { onLap(watch.id) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = stringResource(R.string.btn_lap),
                        modifier = Modifier.size(16.dp)
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
                        .clip(RoundedCornerShape(12.dp))
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.btn_reset),
                            modifier = Modifier.size(16.dp)
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
fun Modifier.onInstantPress(
    enabled: Boolean = true,
    onPress: () -> Unit
): Modifier {
    if (!enabled) return this
    val currentOnPress by rememberUpdatedState(onPress)
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
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
    if (!enabled) return this
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnFullReset by rememberUpdatedState(onFullReset)
    val currentOnLongPressHaptic by rememberUpdatedState(onLongPressHaptic)

    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press || event.changes.any { it.pressed }) {
                    var isReleased = false
                    val releasedBeforeTimeout = withTimeoutOrNull(500L) {
                        while (true) {
                            val nextEvent = awaitPointerEvent()
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
                            startTimeMillis = 0L,
                            laps = listOf(
                                LapRecord(1, 12400L, 12400L)
                            )
                        )
                    )
                ),
                precision = AppPrecision.TENTHS,
                nowMillis = 15000L,
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

