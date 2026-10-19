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
    /**
     * Retrieves the current or latest active [RunSession], or `null` if no runs exist.
     */
    val activeRun: RunSession?
        get() = runs.lastOrNull()

    /**
     * Returns the list of [LapRecord]s recorded in the active run session.
     */
    val currentRunLaps: List<LapRecord>
        get() = activeRun?.laps ?: emptyList()

    /**
     * Returns all [LapRecord]s recorded across all run sessions in chronological order.
     */
    val allLaps: List<LapRecord>
        get() = runs.flatMap { it.laps }

    /**
     * Returns the wall-clock start timestamp in milliseconds of the first run session, or 0 if no runs exist.
     */
    val initialStartTimeMillis: Long
        get() = runs.firstOrNull()?.startTimeMillis ?: 0L

    /**
     * Returns the total cumulative duration in milliseconds across all completed and active runs.
     */
    val totalCumulativeMillis: Long
        get() = getTotalCumulativeMillis(0L)

    /**
     * Calculates the total cumulative duration in nanoseconds across all completed runs and the currently active run session.
     *
     * @param nowNanos The current System.nanoTime() baseline used to compute active run elapsed duration.
     * @return Total duration in nanoseconds across all run sessions.
     */
    fun getTotalCumulativeNanos(nowNanos: Long = 0L): Long {
        val completedSum = runs.filter { it.isFinished }.sumOf { it.accumulatedNanos }
        val active = activeRun
        val activeElapsed = if (isRunning && active != null && active.isRunning) {
            (nowNanos - active.startTimeNanos).coerceAtLeast(0L)
        } else {
            0L
        }
        return completedSum + activeElapsed
    }

    /**
     * Calculates the total cumulative duration in milliseconds across all completed runs and the currently active run session.
     *
     * @param nowNanos The current System.nanoTime() baseline used to compute active run elapsed duration.
     * @return Total duration in milliseconds across all run sessions.
     */
    fun getTotalCumulativeMillis(nowNanos: Long = 0L): Long =
        getTotalCumulativeNanos(nowNanos) / 1_000_000L

    /**
     * Calculates the elapsed duration in nanoseconds for the active run session.
     *
     * @param nowNanos The current System.nanoTime() baseline used if the active run is currently running.
     * @return Elapsed duration in nanoseconds for the active run.
     */
    fun getElapsedNanos(nowNanos: Long = 0L): Long {
        val currentRun = activeRun ?: return 0L
        return if (isRunning) {
            if (currentRun.isRunning) {
                (nowNanos - currentRun.startTimeNanos).coerceAtLeast(0L)
            } else {
                currentRun.accumulatedNanos
            }
        } else {
            if (currentRun.isFinished) {
                currentRun.accumulatedNanos
            } else {
                0L
            }
        }
    }

    /**
     * Calculates the elapsed duration in milliseconds for the active run session.
     *
     * @param nowNanos The current System.nanoTime() baseline used if the active run is currently running.
     * @return Elapsed duration in milliseconds for the active run.
     */
    fun getElapsedMillis(nowNanos: Long = 0L): Long =
        getElapsedNanos(nowNanos) / 1_000_000L

    /**
     * Calculates the predictive timestamp (in nanoseconds) for the next lap completion based on average lap duration.
     *
     * The algorithm computes the average duration of existing laps in the current run and adds it to the last lap's
     * total elapsed time. Returns 0 if no laps have been recorded in the current run.
     *
     * @return Expected total run duration in nanoseconds when the next lap is expected to complete.
     */
    fun calculateExpectedNextLapTime(): Long {
        if (currentRunLaps.isEmpty()) return 0L
        val lastTotalTime = currentRunLaps.last().totalTimeNanos
        val lapCount = currentRunLaps.size
        return lastTotalTime + (lastTotalTime / lapCount)
    }

    /**
     * Performs an immutable state transition when starting or resuming timing.
     *
     * Creates a new [RunSession] if none exists or if the last run was finished, or updates the start timestamp
     * of the last run if resuming. Sets [isRunning] to `true`.
     *
     * @param nowNanos Current system monotonic timestamp in nanoseconds (System.nanoTime()).
     * @param nowMillis Current wall-clock timestamp in milliseconds (System.currentTimeMillis()).
     * @return A new [StopwatchItem] instance with updated running state and run sessions.
     */
    fun start(nowNanos: Long, nowMillis: Long): StopwatchItem {
        if (isRunning) return this
        val newRuns = if (runs.isEmpty()) {
            listOf(
                RunSession(
                    runIndex = 1,
                    startTimeNanos = nowNanos,
                    startTimeMillis = nowMillis
                )
            )
        } else {
            val lastRun = runs.last()
            if (lastRun.isFinished) {
                val runIndex = runs.size + 1
                runs + RunSession(
                    runIndex = runIndex,
                    startTimeNanos = nowNanos,
                    startTimeMillis = nowMillis
                )
            } else {
                val updatedLastRun = lastRun.copy(
                    startTimeNanos = nowNanos,
                    startTimeMillis = if (lastRun.startTimeMillis == 0L) nowMillis else lastRun.startTimeMillis
                )
                runs.dropLast(1) + updatedLastRun
            }
        }
        return copy(
            isRunning = true,
            runs = newRuns
        )
    }

    /**
     * Performs an immutable state transition when pausing or stopping timing.
     *
     * Finalizes the current active run session by calculating its accumulated duration and setting its end time.
     * Sets [isRunning] to `false`.
     *
     * @param nowNanos Current system monotonic timestamp in nanoseconds (System.nanoTime()).
     * @return A new [StopwatchItem] instance with paused state and completed run session.
     */
    fun pause(nowNanos: Long): StopwatchItem {
        if (!isRunning) return this
        val newRuns = if (runs.isNotEmpty()) {
            val activeRun = runs.last()
            val runDuration = (nowNanos - activeRun.startTimeNanos).coerceAtLeast(0L)
            val completedRun = activeRun.copy(
                endTimeNanos = nowNanos,
                accumulatedNanos = runDuration
            )
            runs.dropLast(1) + completedRun
        } else {
            emptyList()
        }
        return copy(
            isRunning = false,
            runs = newRuns
        )
    }

    /**
     * Performs an immutable state transition when recording a lap split with delta comparison against previous laps.
     *
     * Calculates the current lap time, cumulative total time, and delta comparison against the previous lap,
     * adding the resulting [LapRecord] to the current active run.
     *
     * @param nowNanos Current system monotonic timestamp in nanoseconds (System.nanoTime()).
     * @return A new [StopwatchItem] instance containing the newly recorded lap.
     */
    fun lap(nowNanos: Long): StopwatchItem {
        if (!isRunning) return this
        val currentRun = activeRun ?: RunSession(
            runIndex = 1,
            startTimeNanos = nowNanos,
            startTimeMillis = nowNanos / 1_000_000L
        )
        val currentRunTime = (nowNanos - currentRun.startTimeNanos).coerceAtLeast(0L)
        val lastLap = currentRun.laps.lastOrNull()
        val lastTotalTime = lastLap?.totalTimeNanos ?: 0L
        val lapTime = currentRunTime - lastTotalTime
        val lapDeltaNanos = lastLap?.let { lapTime - it.lapTimeNanos }
        val newLap = LapRecord(
            lapNumber = currentRun.laps.size + 1,
            lapTimeNanos = lapTime,
            totalTimeNanos = currentRunTime,
            lapDeltaNanos = lapDeltaNanos
        )
        val updatedLaps = currentRun.laps + newLap
        val updatedRun = currentRun.copy(laps = updatedLaps)
        val newRuns = if (runs.isNotEmpty()) {
            runs.dropLast(1) + updatedRun
        } else {
            listOf(updatedRun)
        }
        return copy(runs = newRuns)
    }

    /**
     * Performs short reset behavior by resetting current active time to 0 and preparing a new run session while preserving history.
     *
     * Finalizes any currently running session, resets current active timing to zero, and prepares a new
     * unstarted [RunSession] while preserving historical run sessions and laps. Sets [isRunning] to `false`.
     *
     * @param nowNanos Current system monotonic timestamp in nanoseconds (System.nanoTime()).
     * @return A new [StopwatchItem] instance with current active time reset and history preserved.
     */
    fun reset(nowNanos: Long): StopwatchItem {
        val finalizedRuns = if (runs.isNotEmpty()) {
            val lastRun = runs.last()
            if (lastRun.isRunning) {
                val duration = (nowNanos - lastRun.startTimeNanos).coerceAtLeast(0L)
                runs.dropLast(1) + lastRun.copy(
                    endTimeNanos = nowNanos,
                    accumulatedNanos = duration
                )
            } else {
                runs
            }
        } else {
            emptyList()
        }

        val preparedRuns = if (finalizedRuns.isNotEmpty() &&
            finalizedRuns.last().isUnstarted &&
            !finalizedRuns.last().isFinished
        ) {
            finalizedRuns
        } else {
            finalizedRuns + RunSession(
                runIndex = finalizedRuns.size + 1,
                startTimeNanos = 0L,
                endTimeNanos = 0L,
                accumulatedNanos = 0L,
                startTimeMillis = 0L,
                laps = emptyList()
            )
        }

        return copy(
            isRunning = false,
            runs = preparedRuns
        )
    }

    /**
     * Performs full reset behavior by clearing all run sessions and laps completely.
     *
     * @return A new [StopwatchItem] instance with all history and state cleared.
     */
    fun resetFull(): StopwatchItem {
        return copy(
            isRunning = false,
            runs = emptyList()
        )
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

        /**
         * Calculates focus selection based on running status, lap history, and predictive next-lap completion algorithms.
         *
         * Prioritizes currently running stopwatches, favoring those with zero recorded laps.
         * For watches with laps recorded, selects the watch with the lowest expected next-lap completion time.
         *
         * @param stopwatches List of stopwatches to evaluate.
         * @param currentFocusedId Currently focused stopwatch ID, if any.
         * @return The ID of the stopwatch that should gain focus, or `null` if the list is empty.
         */
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

        /**
         * Auto-sorts a list of stopwatches based on predictive next-lap completion algorithms.
         *
         * Stopwatches with zero recorded laps in their current run are placed first (expected time 0), followed by
         * stopwatches sorted in ascending order of their predicted next-lap completion times.
         *
         * @param stopwatches List of stopwatches to sort.
         * @return A sorted list of [StopwatchItem] instances.
         */
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
