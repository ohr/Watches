package org.ear.watches.data.model

import kotlinx.serialization.Serializable

/**
 * Snapshot of the timing state (all stopwatches plus the focused watch) persisted across
 * process death and app restarts.
 *
 * Run timestamps ([RunSession.startTimeNanos]) come from `SystemClock.elapsedRealtimeNanos()`,
 * which restarts at zero after a reboot. [bootCount] records which boot those timestamps belong
 * to so that [reconcile] can detect a reboot and fall back to wall-clock time.
 *
 * @property version Schema version of the persisted format.
 * @property bootCount `Settings.Global.BOOT_COUNT` at save time, or `null` if unknown.
 * @property savedAtMillis Wall-clock timestamp of the save.
 * @property focusedWatchId ID of the focused stopwatch, if any.
 * @property stopwatches All stopwatches including their full run history.
 */
@Serializable
data class PersistedStopwatchState(
    val version: Int = CURRENT_VERSION,
    val bootCount: Int? = null,
    val savedAtMillis: Long = 0L,
    val focusedWatchId: String? = null,
    val stopwatches: List<StopwatchItem> = emptyList()
) {
    /**
     * Returns the state adjusted for the current boot session.
     *
     * If the device has rebooted since the save, monotonic timestamps of running watches are no
     * longer meaningful. Those watches are stopped, and the duration of their active run is
     * estimated from wall-clock time ([RunSession.startTimeMillis]), never shorter than the last
     * recorded lap.
     *
     * @param currentBootCount The current `Settings.Global.BOOT_COUNT`, or `null` if unknown.
     * @param nowMillis Current wall-clock time in milliseconds.
     */
    fun reconcile(currentBootCount: Int?, nowMillis: Long): PersistedStopwatchState {
        val rebooted = bootCount != null && currentBootCount != null && bootCount != currentBootCount
        if (!rebooted) return this
        return copy(
            bootCount = currentBootCount,
            stopwatches = stopwatches.map { it.stopAfterReboot(nowMillis) }
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private fun StopwatchItem.stopAfterReboot(nowMillis: Long): StopwatchItem {
            val run = activeRun
            if (!isRunning || run == null || !run.isRunning) {
                return copy(isRunning = false)
            }
            val wallClockNanos = if (run.startTimeMillis > 0L) {
                (nowMillis - run.startTimeMillis).coerceAtLeast(0L) * 1_000_000L
            } else {
                0L
            }
            val minimumNanos = run.laps.lastOrNull()?.totalTimeNanos ?: 0L
            val duration = maxOf(wallClockNanos, minimumNanos)
            val stoppedRun = run.copy(
                // Any positive end marks the run as finished; old-boot nanos are only kept for consistency.
                endTimeNanos = (run.startTimeNanos + duration).coerceAtLeast(1L),
                accumulatedNanos = duration
            )
            return copy(isRunning = false, runs = runs.dropLast(1) + stoppedRun)
        }
    }
}
