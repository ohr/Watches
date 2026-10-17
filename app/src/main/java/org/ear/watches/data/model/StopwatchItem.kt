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
        get() = getTotalCumulativeMillis()

    fun getTotalCumulativeMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val completedSum = runs.filter { it.endTimeMillis > 0L }.sumOf { it.accumulatedMillis }
        val activeElapsed = if (isRunning && activeRun != null && activeRun?.endTimeMillis == 0L && activeRun!!.startTimeMillis > 0L) {
            (nowMillis - activeRun!!.startTimeMillis).coerceAtLeast(0L)
        } else {
            0L
        }
        return completedSum + activeElapsed
    }

    fun getElapsedMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val currentRun = activeRun ?: return 0L
        return if (isRunning) {
            if (currentRun.startTimeMillis > 0L && currentRun.endTimeMillis == 0L) {
                (nowMillis - currentRun.startTimeMillis).coerceAtLeast(0L)
            } else {
                currentRun.accumulatedMillis
            }
        } else {
            if (currentRun.endTimeMillis > 0L) {
                currentRun.accumulatedMillis
            } else {
                0L
            }
        }
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
    }
}
