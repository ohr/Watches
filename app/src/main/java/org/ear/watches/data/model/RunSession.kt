package org.ear.watches.data.model

import androidx.compose.runtime.Stable

/**
 * Represents a continuous recording period (run) of a stopwatch, starting when the stopwatch is resumed/started
 * and ending when it is paused or stopped. Holds all [LapRecord]s recorded during this period.
 *
 * ### Dual-Clock Architecture
 * [RunSession] uses two distinct clock sources to fulfill different requirements:
 * - **Monotonic Clock ([startTimeNanos], [endTimeNanos])**: Sourced from high-precision monotonic timers
 *   (e.g., `SystemClock.elapsedRealtimeNanos()`). Used for all duration calculations, lap timing, and
 *   active ticking logic. Monotonic clocks guarantee continuous upward time progression unaffected by system
 *   clock changes, NTP sync, daylight saving adjustments, or timezone shifts.
 * - **Calendar Clock ([startTimeMillis])**: Sourced from wall-clock time (e.g., `System.currentTimeMillis()`).
 *   Recorded when a run session begins, used purely for user-facing time-of-day displays and report headers
 *   (e.g., displaying "Start Time: 14:30:15").
 *
 * @property runIndex 1-based sequential identifier for this run within the owning stopwatch.
 * @property startTimeNanos Monotonic start timestamp in nanoseconds, or 0L if unstarted.
 * @property endTimeNanos Monotonic end timestamp in nanoseconds, or 0L if currently active or unstarted.
 * @property accumulatedNanos Total duration accumulated in nanoseconds during this run.
 * @property startTimeMillis Wall-clock start timestamp in milliseconds for time-of-day display.
 * @property laps Recorded lap splits belonging to this run.
 */
@Stable
data class RunSession(
    val runIndex: Int,
    val startTimeNanos: Long = 0L,
    val endTimeNanos: Long = 0L,
    val accumulatedNanos: Long = 0L,
    val startTimeMillis: Long = 0L,
    val laps: List<LapRecord> = emptyList(),
) {
    val accumulatedMillis: Long get() = accumulatedNanos / 1_000_000L

    /**
     * `true` if this run session has not been started yet ([startTimeNanos] is 0L).
     */
    val isUnstarted: Boolean get() = startTimeNanos == 0L

    /**
     * `true` if this run session has been started ([startTimeNanos] > 0L).
     */
    val isStarted: Boolean get() = startTimeNanos > 0L

    /**
     * `true` if this run session is actively running ([startTimeNanos] > 0L and [endTimeNanos] is 0L).
     */
    val isRunning: Boolean get() = (startTimeNanos > 0L) && (endTimeNanos == 0L)

    /**
     * `true` if this run session has completed ([endTimeNanos] > 0L).
     */
    val isFinished: Boolean get() = endTimeNanos > 0L
}
