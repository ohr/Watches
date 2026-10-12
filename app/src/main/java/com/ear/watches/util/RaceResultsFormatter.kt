package com.ear.watches.util

import android.content.Context
import com.ear.watches.R
import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.model.StopwatchItem
import com.ear.watches.data.util.TimeFormatter

object RaceResultsFormatter {

    private fun getString(context: Context?, resId: Int, vararg args: Any): String? {
        return context?.getString(resId, *args)
    }

    fun formatRaceResults(
        context: Context?,
        stopwatches: List<StopwatchItem>,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis(),
        subsecondSeparator: String? = null
    ): String {
        if (stopwatches.isEmpty()) {
            return getString(context, R.string.report_no_data)
                ?: "Multi-Stopwatch Coach Report\nNo stopwatch data available."
        }

        val effectiveSeparator = subsecondSeparator
            ?: context?.getString(R.string.subsecond_separator)
            ?: "."

        val sb = StringBuilder()
        sb.appendLine("========================================")
        val headerBanner = getString(context, R.string.report_header_banner) ?: "MULTI-STOPWATCH RESULTS"
        sb.appendLine("         $headerBanner        ")
        sb.appendLine("========================================")

        val totalAthletesText = getString(context, R.string.report_total_athletes, stopwatches.size)
            ?: "Total Athletes/Stopwatches: ${stopwatches.size}"
        sb.appendLine(totalAthletesText)
        sb.appendLine()

        stopwatches.forEachIndexed { index, watch ->
            val totalTime = watch.getTotalCumulativeMillis(nowMillis)
            val formattedTotalTime = TimeFormatter.formatTime(totalTime, precision, effectiveSeparator)

            val runnerText = getString(context, R.string.report_runner_format, index + 1, watch.name)
                ?: "${index + 1}. Runner: ${watch.name}"
            val statusRunning = getString(context, R.string.status_running) ?: "RUNNING"
            val statusStopped = getString(context, R.string.status_stopped) ?: "STOPPED"
            val statusLabel = getString(context, R.string.report_status) ?: "Status"
            val statusValue = if (watch.isRunning) statusRunning else statusStopped

            val totalAllRunsLabel = getString(context, R.string.label_total_all_runs) ?: "Total (All Runs)"

            sb.appendLine(runnerText)
            sb.appendLine("   $statusLabel: $statusValue")
            sb.appendLine("   $totalAllRunsLabel: $formattedTotalTime")

            val effectiveRuns = watch.runs.filter { it.startTimeMillis > 0L || it.endTimeMillis > 0L || it.accumulatedMillis > 0L || it.laps.isNotEmpty() }

            if (effectiveRuns.isNotEmpty()) {
                effectiveRuns.forEach { run ->
                    val runTitle = getString(context, R.string.label_run, run.runIndex) ?: "Run ${run.runIndex}"
                    val runStartMillis = run.startTimeMillis
                    val runStartTimeVal = if (runStartMillis > 0L) {
                        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(runStartMillis))
                    } else "-"
                    val startTimeLabel = getString(context, R.string.label_start_time) ?: "Start Time"

                    val runDurationMillis = if (run.endTimeMillis == 0L && watch.isRunning) {
                        (nowMillis - run.startTimeMillis).coerceAtLeast(0L)
                    } else {
                        run.accumulatedMillis
                    }
                    val formattedRunDuration = TimeFormatter.formatTime(runDurationMillis, precision, effectiveSeparator)
                    val runDurationLabel = getString(context, R.string.label_run_duration) ?: "Run Duration"

                    sb.appendLine("   --- $runTitle ---")
                    sb.appendLine("     $startTimeLabel: $runStartTimeVal")
                    sb.appendLine("     $runDurationLabel: $formattedRunDuration")

                    val laps = run.laps
                    if (laps.isNotEmpty()) {
                        val bestLap = laps.minByOrNull { it.lapTimeMillis }
                        if (bestLap != null) {
                            val formattedBestLap = TimeFormatter.formatTime(bestLap.lapTimeMillis, precision, effectiveSeparator)
                            val bestLapText = getString(context, R.string.report_best_lap_format, formattedBestLap, bestLap.lapNumber)
                                ?: "Best Lap: $formattedBestLap (Lap ${bestLap.lapNumber})"
                            sb.appendLine("     $bestLapText")
                        }

                        val lapBreakdownText = getString(context, R.string.report_lap_breakdown_format, laps.size)
                            ?: "Lap Breakdown (${laps.size} laps):"
                        sb.appendLine("     $lapBreakdownText")

                        laps.forEach { lap ->
                            val formattedLapTime = TimeFormatter.formatTime(lap.lapTimeMillis, precision, effectiveSeparator)
                            val formattedCumTime = TimeFormatter.formatTime(lap.totalTimeMillis, precision, effectiveSeparator)
                            val lapEntryText = getString(context, R.string.report_lap_entry_format, lap.lapNumber, formattedLapTime, formattedCumTime)
                                ?: "- Lap ${lap.lapNumber}: $formattedLapTime | Cumulative: $formattedCumTime"
                            sb.appendLine("       $lapEntryText")
                        }
                    } else {
                        val bestLapNaText = getString(context, R.string.report_best_lap_na) ?: "Best Lap: N/A"
                        val lapsNoneText = getString(context, R.string.report_laps_none) ?: "Laps: None recorded"
                        sb.appendLine("     $bestLapNaText")
                        sb.appendLine("     $lapsNoneText")
                    }
                }
            } else {
                val bestLapNaText = getString(context, R.string.report_best_lap_na) ?: "Best Lap: N/A"
                val lapsNoneText = getString(context, R.string.report_laps_none) ?: "Laps: None recorded"
                sb.appendLine("   $bestLapNaText")
                sb.appendLine("   $lapsNoneText")
            }
            sb.appendLine("----------------------------------------")
        }

        return sb.toString().trimEnd()
    }

    fun formatRaceResults(
        stopwatches: List<StopwatchItem>,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis(),
        subsecondSeparator: String = "."
    ): String = formatRaceResults(null, stopwatches, precision, nowMillis, subsecondSeparator)

    fun formatSingleStopwatchResults(
        context: Context?,
        watch: StopwatchItem,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis(),
        subsecondSeparator: String? = null
    ): String = formatRaceResults(context, listOf(watch), precision, nowMillis, subsecondSeparator)

    fun formatSingleStopwatchResults(
        watch: StopwatchItem,
        precision: AppPrecision = AppPrecision.HUNDREDTHS,
        nowMillis: Long = System.currentTimeMillis(),
        subsecondSeparator: String = "."
    ): String = formatSingleStopwatchResults(null, watch, precision, nowMillis, subsecondSeparator)
}
