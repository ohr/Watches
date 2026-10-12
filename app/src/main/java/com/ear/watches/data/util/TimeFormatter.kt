package com.ear.watches.data.util

import com.ear.watches.data.model.AppPrecision
import java.util.Locale

object TimeFormatter {

    fun formatTime(millis: Long, precision: AppPrecision, subsecondSeparator: String = "."): String {
        val safeMillis = if (millis < 0) 0L else millis

        val hours = safeMillis / 3_600_000
        val minutes = (safeMillis % 3_600_000) / 60_000
        val seconds = (safeMillis % 60_000) / 1_000
        val remainderMillis = safeMillis % 1_000

        val timeString = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

        return when (precision) {
            AppPrecision.TENTHS -> {
                val tenths = remainderMillis / 100
                "$timeString$subsecondSeparator$tenths"
            }
            AppPrecision.HUNDREDTHS -> {
                val hundredths = remainderMillis / 10
                String.format(Locale.US, "%s%s%02d", timeString, subsecondSeparator, hundredths)
            }
            AppPrecision.SECONDS -> {
                timeString
            }
        }
    }
}
