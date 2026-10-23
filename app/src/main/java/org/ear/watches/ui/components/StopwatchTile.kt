package org.ear.watches.ui.components

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import org.ear.watches.R
import org.ear.watches.data.model.AppAutoSortOption
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.LapRecord
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.util.TimeFormatter
import org.ear.watches.ui.theme.WatchesTheme

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
    onNameSession: (String) -> Unit = {},
    autoSortOption: AppAutoSortOption = AppAutoSortOption.NONE
) {
    var showCurrentLapTime by rememberSaveable { mutableStateOf(false) }

    val isFocused = watch.isFocused
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) StopwatchTileDefaults.FocusBorderWidth else 1.dp,
        label = "animatedBorderWidth"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) StopwatchTileDefaults.FocusBorderColor else Color(0x1F000000),
        label = "animatedBorderColor"
    )

    val cardBackgroundColor = remember(watch.pastelColorHex) { Color(watch.pastelColorHex) }
    val darkContentColor = remember { Color(0xFF1F1F1F) }

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
            .drawWithCache {
                val strokePx = animatedBorderWidth.toPx()
                val color = animatedBorderColor
                val cornerRadiusPx = 20.dp.toPx()
                val halfStroke = strokePx / 2f
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = strokePx)
                    )
                }
            }
            .pointerInput(watch.id) {
                detectTapGestures(
                    onTap = { onSelectFocus(watch.id) }
                )
            },
        shape = StopwatchTileDefaults.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 6.dp else StopwatchTileDefaults.CardElevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StopwatchTileDefaults.TilePaddingValues)
        ) {
            StopwatchTileHeader(
                watch = watch,
                darkContentColor = darkContentColor,
                autoSortOption = autoSortOption,
                onEdit = onEdit,
                onOpenDetails = onOpenDetails,
                onDelete = onDelete
            )

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            StopwatchTimeDisplay(
                formattedTime = formattedTime,
                darkContentColor = darkContentColor,
                showCurrentLapTime = showCurrentLapTime,
                onToggleLapDisplay = { showCurrentLapTime = !showCurrentLapTime }
            )

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            StopwatchStatusAndPaceRow(
                watch = watch,
                lastLapTime = lastLapTime,
                darkContentColor = darkContentColor,
                timePrecision = timePrecision,
                subsecondSeparator = subsecondSeparator
            )

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            StopwatchActionControls(
                watch = watch,
                onStart = onStart,
                onPause = onPause,
                onLap = onLap,
                onReset = onReset,
                onFullReset = onFullReset,
                onNameSession = onNameSession
            )
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
