package com.ear.watches.ui.dialogs

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.ear.watches.R
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.AppSettings
import com.ear.watches.data.model.LapRecord
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import com.ear.watches.data.model.RunSession
import com.ear.watches.data.model.StopwatchItem
import com.ear.watches.data.util.TimeFormatter
import com.ear.watches.ui.theme.WatchesTheme
import com.ear.watches.util.ShareHelper
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val timeFormatter = remember(locale) { SimpleDateFormat("HH:mm:ss", locale) }
    val subsecondSeparator = stringResource(R.string.subsecond_separator)

    val currentTick by produceState(initialValue = System.currentTimeMillis(), watch.isRunning, tickerFlow) {
        if (watch.isRunning) {
            tickerFlow.collect { value = it }
        }
    }
    val nowMillis = if (watch.isRunning) currentTick else System.currentTimeMillis()

    val effectiveRuns = watch.runs.filter { it.startTimeMillis > 0L || it.endTimeMillis > 0L || it.accumulatedMillis > 0L || it.laps.isNotEmpty() }
    val hasRunData = watch.runs.any { it.laps.isNotEmpty() || it.accumulatedMillis > 0L || it.endTimeMillis > 0L }

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
                        val totalCumulativeMillis = watch.getTotalCumulativeMillis(nowMillis)
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
                            val runEndString = if (run.endTimeMillis > 0L) {
                                timeFormatter.format(Date(run.endTimeMillis))
                            } else if (watch.isRunning && run.runIndex == effectiveRuns.size) {
                                stringResource(R.string.status_running)
                            } else {
                                "-"
                            }
                            val runDurationMillis = if (run.endTimeMillis == 0L && watch.isRunning && run.runIndex == effectiveRuns.size) {
                                (nowMillis - run.startTimeMillis).coerceAtLeast(0L)
                            } else {
                                run.accumulatedMillis
                            }
                            
                            val isRunActive = watch.isRunning && run.endTimeMillis == 0L
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

                // Fixed Footer (Bottom): "Close" and "Share" buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = if (hasRunData) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_close),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (hasRunData) {
                        Button(
                            onClick = {
                                ShareHelper.shareSingleStopwatchResults(context, watch, settings.timePrecision, nowMillis)
                            },
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
                        startTimeMillis = 1700000000000L,
                        endTimeMillis = 1700000015000L,
                        accumulatedMillis = 15000L,
                        laps = listOf(LapRecord(1, 15000L, 15000L))
                    ),
                    RunSession(
                        runIndex = 2,
                        startTimeMillis = 1700000015000L,
                        endTimeMillis = 1700000045200L,
                        accumulatedMillis = 30200L,
                        laps = listOf(LapRecord(1, 30200L, 30200L))
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
