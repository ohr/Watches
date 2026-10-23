package org.ear.watches.data.util

import org.ear.watches.data.model.AppPrecision
import kotlin.math.abs

/**
 * Utility object responsible for formatting millisecond durations into human-readable strings.
 * Handles formatting logic for different precisions (e.g., tenths, hundredths, seconds),
 * and supports both absolute time formatting and delta time formatting.
 *
 * Optimized for high-frequency execution (60+ FPS) without String.format overhead to minimize GC pressure.
 */
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

        val sb = StringBuilder(16)
        if (prefix.isNotEmpty()) {
            sb.append(prefix)
        }

        if (isDelta) {
            if (hours > 0) {
                appendDigits(sb, hours)
                sb.append(':')
                appendTwoDigits(sb, minutes)
                sb.append(':')
                appendTwoDigits(sb, seconds)
            } else if (minutes > 0) {
                appendDigits(sb, minutes)
                sb.append(':')
                appendTwoDigits(sb, seconds)
            } else {
                appendDigits(sb, seconds)
            }
        } else {
            if (hours > 0) {
                appendTwoDigits(sb, hours)
                sb.append(':')
                appendTwoDigits(sb, minutes)
                sb.append(':')
                appendTwoDigits(sb, seconds)
            } else {
                appendTwoDigits(sb, minutes)
                sb.append(':')
                appendTwoDigits(sb, seconds)
            }
        }

        when (precision) {
            AppPrecision.TENTHS -> {
                val tenths = remainderMillis / 100
                sb.append(subsecondSeparator)
                sb.append(tenths)
            }
            AppPrecision.HUNDREDTHS -> {
                val hundredths = remainderMillis / 10
                sb.append(subsecondSeparator)
                appendTwoDigits(sb, hundredths)
            }
            AppPrecision.SECONDS -> {}
        }

        if (isDelta) {
            sb.append('s')
        }

        return sb.toString()
    }

    private fun appendTwoDigits(sb: StringBuilder, number: Long) {
        val n = (number % 100).toInt()
        sb.append((n / 10 + '0'.code).toChar())
        sb.append((n % 10 + '0'.code).toChar())
    }

    private fun appendDigits(sb: StringBuilder, number: Long) {
        if (number < 10) {
            sb.append((number.toInt() + '0'.code).toChar())
        } else {
            sb.append(number.toString())
        }
    }
}
