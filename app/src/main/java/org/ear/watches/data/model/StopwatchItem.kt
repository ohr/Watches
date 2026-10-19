package org.ear.watches.data.model

import androidx.compose.runtime.Stable
import java.util.UUID

/**
 * Represents a single stopwatch instance containing its core configuration, current state,
 * and a history of all [RunSession]s. Provides utility properties and methods for calculating
 * elapsed time and laps across runs.
 */
@Stable
data class StopwatchItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pastelColorHex: Long,
    val isRunning: Boolean = false,
    val runs: List<RunSession> = emptyList(),
    val isFocused: Boolean = false
) {
    val activeRun: RunSession?
        get() = runs.lastOrNull()

    val currentRunLaps: List<LapRecord>
        get() = activeRun?.laps ?: emptyList()

    val allLaps: List<LapRecord>
        get() = runs.flatMap { it.laps }

    val initialStartTimeMillis: Long
        get() = runs.firstOrNull()?.startTimeMillis ?: 0L

    val totalCumulativeMillis: Long
        get() = getTotalCumulativeMillis(0L)

    fun getTotalCumulativeNanos(nowNanos: Long = 0L): Long {
        val completedSum = runs.filter { it.endTimeNanos > 0L }.sumOf { it.accumulatedNanos }
        val active = activeRun
        val activeElapsed = if (isRunning && active != null && active.endTimeNanos == 0L && active.startTimeNanos > 0L) {
            (nowNanos - active.startTimeNanos).coerceAtLeast(0L)
        } else {
            0L
        }
        return completedSum + activeElapsed
    }

    fun getTotalCumulativeMillis(nowNanos: Long = 0L): Long =
        getTotalCumulativeNanos(nowNanos) / 1_000_000L

    fun getElapsedNanos(nowNanos: Long = 0L): Long {
        val currentRun = activeRun ?: return 0L
        return if (isRunning) {
            if (currentRun.startTimeNanos > 0L && currentRun.endTimeNanos == 0L) {
                (nowNanos - currentRun.startTimeNanos).coerceAtLeast(0L)
            } else {
                currentRun.accumulatedNanos
            }
        } else {
            if (currentRun.endTimeNanos > 0L) {
                currentRun.accumulatedNanos
            } else {
                0L
            }
        }
    }

    fun getElapsedMillis(nowNanos: Long = 0L): Long =
        getElapsedNanos(nowNanos) / 1_000_000L

    fun calculateExpectedNextLapTime(): Long {
        if (currentRunLaps.isEmpty()) return 0L
        val lastTotalTime = currentRunLaps.last().totalTimeNanos
        val lapCount = currentRunLaps.size
        return lastTotalTime + (lastTotalTime / lapCount)
    }

    companion object {
        val DEFAULT_PASTEL_COLORS = listOf(
            0xFFEF9A9AL, // Pastel Red
            0xFF90CAF9L, // Pastel Blue
            0xFFA5D6A7L, // Pastel Green
            0xFFFFE082L, // Pastel Amber
            0xFFCE93D8L, // Pastel Purple
            0xFF80CBC4L, // Pastel Teal
            0xFFFFAB91L, // Pastel Orange
            0xFFF48F11L, // Pastel Coral
            0xFFB0BEC5L, // Pastel Blue Grey
            0xFFB39DDBL  // Pastel Deep Purple
        )

        fun calculateNextFocusedWatchId(
            stopwatches: List<StopwatchItem>,
            currentFocusedId: String? = null
        ): String? {
            if (stopwatches.isEmpty()) return null

            val runningWatches = stopwatches.filter { it.isRunning }
            val candidates = runningWatches.ifEmpty { stopwatches }

            val zeroLapCandidates = candidates.filter { it.currentRunLaps.isEmpty() }
            if (zeroLapCandidates.isNotEmpty()) {
                return if (currentFocusedId != null && zeroLapCandidates.any { it.id == currentFocusedId }) {
                    currentFocusedId
                } else {
                    zeroLapCandidates.first().id
                }
            }

            val bestWatch = candidates.minByOrNull { watch ->
                watch.calculateExpectedNextLapTime()
            }

            return bestWatch?.id ?: currentFocusedId ?: stopwatches.firstOrNull()?.id
        }

        fun sortStopwatches(
            stopwatches: List<StopwatchItem>
        ): List<StopwatchItem> {
            return stopwatches.sortedWith(
                compareBy<StopwatchItem> { watch ->
                    if (watch.currentRunLaps.isEmpty()) 0L
                    else watch.calculateExpectedNextLapTime()
                }
            )
        }
    }
}
