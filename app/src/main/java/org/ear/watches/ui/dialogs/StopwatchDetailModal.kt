package org.ear.watches.ui.dialogs

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.ear.watches.R
import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.model.AppSettings
import org.ear.watches.data.model.LapRecord
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow
import org.ear.watches.data.model.RunSession
import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.util.TimeFormatter
import org.ear.watches.ui.theme.WatchesTheme
import org.ear.watches.util.ShareHelper
import java.text.SimpleDateFormat
import java.util.Date

/**
 * A detailed modal view for a single stopwatch, showing its full history of runs and laps.
 * Used for post-workout review, displaying calculated elapsed times and lap deltas.
 */
@Composable
fun StopwatchDetailModal(
    watch: StopwatchItem,
    settings: AppSettings,
    tickerFlow: StateFlow<Long>,
    onDeleteRuns: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val timeFormatter = remember(locale) { SimpleDateFormat("HH:mm:ss", locale) }
    val subsecondSeparator = stringResource(R.string.subsecond_separator)

    val currentTick by produceState(initialValue = SystemClock.elapsedRealtimeNanos(), watch.isRunning, tickerFlow) {
        if (watch.isRunning) {
            tickerFlow.collect { value = it }
        }
    }
    val nowNanos = if (watch.isRunning) currentTick else SystemClock.elapsedRealtimeNanos()

    var showShareFormatDialog by remember { mutableStateOf(false) }

    val effectiveRuns = watch.runs.filter { it.isStarted || it.isFinished || it.accumulatedNanos > 0L || it.laps.isNotEmpty() }
    val hasRunData = watch.runs.any { it.laps.isNotEmpty() || it.accumulatedNanos > 0L || it.isFinished }
    val canDeleteRuns = !watch.isRunning && watch.runs.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Fixed Header (Top): Watch Name, Pastel color badge, Close (X) icon button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(watch.pastelColorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = watch.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_close_details)
                        )
                    }
                }

                // Scrollable Middle Panel: LazyColumn with Modifier.weight(1f).fillMaxWidth()
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Cumulative Time across all runs
                    item {
                        val totalCumulativeMillis = watch.getTotalCumulativeMillis(nowNanos)
                        val totalPrecision = if (watch.isRunning) settings.runningDisplayPrecision else settings.timePrecision
                        val formattedTotalCumulative = TimeFormatter.formatTime(totalCumulativeMillis, totalPrecision, subsecondSeparator)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.label_total_all_runs),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                Text(
                                    text = formattedTotalCumulative,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    // Run Sessions Section (LazyColumn items)
                    if (effectiveRuns.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.report_laps_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        items(effectiveRuns) { run ->
                            val runStartString = if (run.startTimeMillis > 0L) {
                                timeFormatter.format(Date(run.startTimeMillis))
                            } else {
                                "-"
                            }
                            val runEndString = if (run.isFinished) {
                                if (run.startTimeMillis > 0L) {
                                    timeFormatter.format(Date(run.startTimeMillis + run.accumulatedMillis))
                                } else {
                                    "-"
                                }
                            } else if (watch.isRunning && run.runIndex == effectiveRuns.size) {
                                stringResource(R.string.status_running)
                            } else {
                                "-"
                            }
                            val runDurationMillis = if (run.isRunning && watch.isRunning && run.runIndex == effectiveRuns.size) {
                                watch.getElapsedMillis(nowNanos)
                            } else {
                                run.accumulatedMillis
                            }
                            
                            val isRunActive = watch.isRunning && run.isRunning
                            val runPrecision = if (isRunActive) settings.runningDisplayPrecision else settings.timePrecision
                            val formattedRunDuration = TimeFormatter.formatTime(runDurationMillis, runPrecision, subsecondSeparator)

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Card or section header: "Run X" with Run Duration and Start/End timestamp
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.label_run, run.runIndex),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formattedRunDuration,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${stringResource(R.string.label_start_time)}: $runStartString",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${stringResource(R.string.label_end_time)}: $runEndString",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )

                                    // Vertical list of Laps for this run: Lap number, Lap split duration, and total duration
                                    if (run.laps.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.report_laps_none),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Lap table header row: [Lap #]   [Lap Time]   [Total Time]   [Delta]
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.header_lap_num),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(0.15f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = stringResource(R.string.header_lap_time),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(0.30f),
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = stringResource(R.string.header_total_time),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(0.30f),
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = stringResource(R.string.header_pace_delta),
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.weight(0.25f),
                                                    textAlign = TextAlign.End,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            run.laps.forEachIndexed { index, lap ->
                                                val deltaMillis = lap.lapDeltaMillis ?: if (index > 0) lap.lapTimeMillis - run.laps[index - 1].lapTimeMillis else null
                                                val deltaText = if (deltaMillis != null) {
                                                    TimeFormatter.formatDeltaTime(deltaMillis, settings.timePrecision, subsecondSeparator)
                                                } else {
                                                    "-"
                                                }
                                                val deltaColor = when {
                                                    deltaMillis != null && deltaMillis < 0 -> Color(0xFF2E7D32)
                                                    deltaMillis != null && deltaMillis > 0 -> Color(0xFFD32F2F)
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = lap.lapNumber.toString(),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontWeight = FontWeight.Medium,
                                                            fontFamily = FontFamily.Monospace
                                                        ),
                                                        modifier = Modifier.weight(0.15f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = TimeFormatter.formatTime(lap.lapTimeMillis, settings.timePrecision, subsecondSeparator),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = FontFamily.Monospace
                                                        ),
                                                        modifier = Modifier.weight(0.30f),
                                                        textAlign = TextAlign.End,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = TimeFormatter.formatTime(lap.totalTimeMillis, settings.timePrecision, subsecondSeparator),
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = FontFamily.Monospace
                                                        ),
                                                        modifier = Modifier.weight(0.30f),
                                                        textAlign = TextAlign.End,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = deltaText,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = if (deltaMillis != null && deltaMillis != 0L) FontWeight.Bold else FontWeight.Normal,
                                                            color = deltaColor
                                                        ),
                                                        modifier = Modifier.weight(0.25f),
                                                        textAlign = TextAlign.End,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fixed Footer (Bottom): "Close", "Delete Runs", and "Share" buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val hasMultipleButtons = hasRunData || canDeleteRuns

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = if (hasMultipleButtons) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_close),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (canDeleteRuns) {
                        FilledTonalButton(
                            onClick = { onDeleteRuns(watch.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_runs),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_delete_runs),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (hasRunData) {
                        Button(
                            onClick = { showShareFormatDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_share),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareFormatDialog) {
        ShareFormatDialog(
            onSelectText = {
                showShareFormatDialog = false
                ShareHelper.shareSingleStopwatchResults(context, watch, settings.timePrecision, nowNanos)
            },
            onSelectCsv = {
                showShareFormatDialog = false
                ShareHelper.shareSingleStopwatchResultsCsv(context, watch, settings.timePrecision, nowNanos)
            },
            onDismiss = { showShareFormatDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StopwatchDetailModalPreview() {
    WatchesTheme {
        StopwatchDetailModal(
            watch = StopwatchItem(
                name = "Runner Alpha",
                pastelColorHex = 0xFF90CAF9L,
                runs = listOf(
                    RunSession(
                        runIndex = 1,
                        startTimeNanos = 1_000_000_000L,
                        endTimeNanos = 16_000_000_000L,
                        accumulatedNanos = 15_000_000_000L,
                        startTimeMillis = 1700000000000L,
                        laps = listOf(LapRecord(1, 15_000_000_000L, 15_000_000_000L))
                    ),
                    RunSession(
                        runIndex = 2,
                        startTimeNanos = 16_000_000_000L,
                        endTimeNanos = 46_200_000_000L,
                        accumulatedNanos = 30_200_000_000L,
                        startTimeMillis = 1700000015000L,
                        laps = listOf(LapRecord(1, 30_200_000_000L, 30_200_000_000L))
                    )
                )
            ),
            settings = AppSettings(
                runningDisplayPrecision = AppPrecision.TENTHS,
                timePrecision = AppPrecision.HUNDREDTHS
            ),
            tickerFlow = kotlinx.coroutines.flow.MutableStateFlow(1700000045200L),
            onDismiss = {}
        )
    }
}
