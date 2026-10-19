package org.ear.watches.data

import org.ear.watches.data.model.AppPrecision
import org.ear.watches.data.util.TimeFormatter.formatDeltaTime
import org.ear.watches.data.util.TimeFormatter.formatTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class TimeFormatterTest {

    @Test
    fun formatTime_defaultSeparator_usesDot() {
        assertEquals("00:00.0", formatTime(0L, AppPrecision.TENTHS))
        assertEquals("00:05.4", formatTime(5432L, AppPrecision.TENTHS))
        assertEquals("01:05.4", formatTime(65432L, AppPrecision.TENTHS))
        assertEquals("01:00:00.0", formatTime(3600000L, AppPrecision.TENTHS))

        assertEquals("00:00.00", formatTime(0L, AppPrecision.HUNDREDTHS))
        assertEquals("00:05.43", formatTime(5432L, AppPrecision.HUNDREDTHS))
        assertEquals("01:05.43", formatTime(65432L, AppPrecision.HUNDREDTHS))
        assertEquals("01:02:03.04", formatTime(3723040L, AppPrecision.HUNDREDTHS))

        assertEquals("00:00", formatTime(0L, AppPrecision.SECONDS))
        assertEquals("00:05", formatTime(5432L, AppPrecision.SECONDS))
        assertEquals("01:05", formatTime(65432L, AppPrecision.SECONDS))
        assertEquals("01:00:00", formatTime(3600000L, AppPrecision.SECONDS))

        assertEquals("00:00.00", formatTime(-1000L, AppPrecision.HUNDREDTHS))
    }

    @Test
    fun formatTime_customSubsecondSeparator_germanLocaleComma() {
        assertEquals("00:00,0", formatTime(0L, AppPrecision.TENTHS, ","))
        assertEquals("00:05,4", formatTime(5432L, AppPrecision.TENTHS, ","))
        assertEquals("01:05,4", formatTime(65432L, AppPrecision.TENTHS, ","))
        assertEquals("01:00:00,0", formatTime(3600000L, AppPrecision.TENTHS, ","))

        assertEquals("00:00,00", formatTime(0L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("00:05,43", formatTime(5432L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("01:05,43", formatTime(65432L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("01:02:03,04", formatTime(3723040L, AppPrecision.HUNDREDTHS, ","))

        assertEquals("00:00", formatTime(0L, AppPrecision.SECONDS, ","))
        assertEquals("00:05", formatTime(5432L, AppPrecision.SECONDS, ","))

        assertEquals("00:00,00", formatTime(-1000L, AppPrecision.HUNDREDTHS, ","))
    }

    @Test
    fun formatDeltaTime_fasterSlowerEqual_tenths() {
        assertEquals("-1,2s", formatDeltaTime(-1200L, ","))
        assertEquals("+0,8s", formatDeltaTime(800L, ","))
        assertEquals("0,0s", formatDeltaTime(0L, ","))

        assertEquals("-1.2s", formatDeltaTime(-1200L, "."))
        assertEquals("+0.8s", formatDeltaTime(800L, "."))
        assertEquals("0.0s", formatDeltaTime(0L, "."))
    }

    @Test
    fun formatDeltaTime_precisionVariations() {
        assertEquals("-1,20s", formatDeltaTime(-1200L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("+0,80s", formatDeltaTime(800L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("0,00s", formatDeltaTime(0L, AppPrecision.HUNDREDTHS, ","))

        assertEquals("-1s", formatDeltaTime(-1200L, AppPrecision.SECONDS, ","))
        assertEquals("+0s", formatDeltaTime(800L, AppPrecision.SECONDS, ","))
        assertEquals("0s", formatDeltaTime(0L, AppPrecision.SECONDS, ","))
    }
}
