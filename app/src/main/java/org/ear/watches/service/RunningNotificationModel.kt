package org.ear.watches.service

import org.ear.watches.data.model.StopwatchItem
import org.ear.watches.data.model.StopwatchUiState

/**
 * Platform-independent content of the ongoing "stopwatches running" notification.
 *
 * @property targetWatchId Watch the Lap/Stop actions apply to (same target as the volume keys).
 * @property targetName Display name of [targetWatchId].
 * @property runningCount Number of running watches.
 * @property totalCount Number of watches overall.
 * @property chronometerBaseMillis Wall-clock time at which the target's current run would have
 *   started, so a notification chronometer counts up the target's elapsed time on its own.
 * @property lastLapNumber Number of the target's last lap in the current run, if any.
 * @property lastLapMillis Duration of that last lap, if any.
 */
data class RunningNotificationModel(
    val targetWatchId: String,
    val targetName: String,
    val runningCount: Int,
    val totalCount: Int,
    val chronometerBaseMillis: Long,
    val lastLapNumber: Int?,
    val lastLapMillis: Long?
) {
    companion object {
        /**
         * Builds the notification content, or returns `null` when no watch is running or the
         * notification is disabled in the settings (the service then stops itself).
         *
         * @param nowNanos Current monotonic time (same clock as run start timestamps).
         * @param nowMillis Current wall-clock time.
         */
        fun from(state: StopwatchUiState, nowNanos: Long, nowMillis: Long): RunningNotificationModel? {
            if (!state.settings.showRunningNotification) return null
            val targetId = StopwatchItem.resolveTargetWatchId(state.stopwatches, state.activeFocusedWatchId)
                ?: return null
            val target = state.stopwatches.first { it.id == targetId }
            val lastLap = target.currentRunLaps.lastOrNull()
            return RunningNotificationModel(
                targetWatchId = target.id,
                targetName = target.name,
                runningCount = state.stopwatches.count { it.isRunning },
                totalCount = state.stopwatches.size,
                chronometerBaseMillis = nowMillis - target.getElapsedMillis(nowNanos),
                lastLapNumber = lastLap?.lapNumber,
                lastLapMillis = lastLap?.lapTimeMillis
            )
        }
    }
}
