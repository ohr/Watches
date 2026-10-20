package org.ear.watches.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
            .pointerInput(watch.id) {
                detectTapGestures(
                    onTap = { onSelectFocus(watch.id) }
                )
            },
        shape = StopwatchTileDefaults.CardShape,
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor
        ),
        border = BorderStroke(
            width = borderThickness,
            color = if (isFocused) StopwatchTileDefaults.FocusBorderColor else Color.Black.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(StopwatchTileDefaults.TilePaddingValues)
        ) {
            // Header Row: Name, Focus Tag, Details & Delete buttons
            StopwatchTileHeader(
                watch = watch,
                darkContentColor = darkContentColor,
                autoSortOption = autoSortOption,
                onEdit = onEdit,
                onOpenDetails = onOpenDetails,
                onDelete = onDelete
            )

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            // Running Time Display
            StopwatchTimeDisplay(
                formattedTime = formattedTime,
                darkContentColor = darkContentColor,
                showCurrentLapTime = showCurrentLapTime,
                onToggleLapDisplay = { showCurrentLapTime = !showCurrentLapTime }
            )

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            // Status and Last Lap Info Row
            StopwatchStatusAndPaceRow(
                watch = watch,
                lastLapTime = lastLapTime,
                darkContentColor = darkContentColor,
                timePrecision = timePrecision,
                subsecondSeparator = subsecondSeparator
            )

            Spacer(modifier = Modifier.height(StopwatchTileDefaults.SectionSpacing))

            // Action Controls Row
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
