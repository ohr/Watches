package com.ear.watches.data.util

import com.ear.watches.data.model.AppPrecision
import java.util.Locale
import kotlin.math.abs

object TimeFormatter {

    fun formatTime(millis: Long, precision: AppPrecision, subsecondSeparator: String = "."): String {
        val safeMillis = if (millis < 0) 0L else millis
        return formatTimeInternal(
            millis = abs(safeMillis),
            precision = precision,
            subsecondSeparator = subsecondSeparator,
            prefix = ""
        )
    }

    fun formatDeltaTime(
        deltaMillis: Long,
        precision: AppPrecision = AppPrecision.TENTHS,
        subsecondSeparator: String = "."
    ): String {
        return formatTimeInternal(
            millis = abs(deltaMillis),
            precision = precision,
            subsecondSeparator = subsecondSeparator,
            prefix = if (deltaMillis > 0) "+" else if (deltaMillis < 0) "-" else "",
            isDelta = true
        )
    }

    fun formatDeltaTime(
        deltaMillis: Long,
        subsecondSeparator: String
    ): String = formatDeltaTime(deltaMillis, AppPrecision.TENTHS, subsecondSeparator)

    internal fun formatTimeInternal(
        millis: Long,
        precision: AppPrecision,
        subsecondSeparator: String,
        prefix: String = "",
        isDelta: Boolean = false
    ): String {
        val safeMillis = abs(millis)

        val hours = safeMillis / 3_600_000
        val minutes = (safeMillis % 3_600_000) / 60_000
        val seconds = (safeMillis % 60_000) / 1_000
        val remainderMillis = safeMillis % 1_000

        val mainPart = if (isDelta) {
            if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else if (minutes > 0) {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            } else {
                seconds.toString()
            }
        } else {
            if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

        val subsecondPart = when (precision) {
            AppPrecision.TENTHS -> {
                val tenths = remainderMillis / 100
                "$subsecondSeparator$tenths"
            }
            AppPrecision.HUNDREDTHS -> {
                val hundredths = remainderMillis / 10
                String.format(Locale.US, "%s%02d", subsecondSeparator, hundredths)
            }
            AppPrecision.SECONDS -> ""
        }

        val suffix = if (isDelta) "s" else ""

        return "$prefix$mainPart$subsecondPart$suffix"
    }
}
