package com.ear.watches.data

import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.util.TimeFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimeFormatterTest {

    @Test
    fun formatTime_defaultSeparator_usesDot() {
        assertEquals("00:00.0", TimeFormatter.formatTime(0L, AppPrecision.TENTHS))
        assertEquals("00:05.4", TimeFormatter.formatTime(5432L, AppPrecision.TENTHS))
        assertEquals("01:05.4", TimeFormatter.formatTime(65432L, AppPrecision.TENTHS))
        assertEquals("01:00:00.0", TimeFormatter.formatTime(3600000L, AppPrecision.TENTHS))

        assertEquals("00:00.00", TimeFormatter.formatTime(0L, AppPrecision.HUNDREDTHS))
        assertEquals("00:05.43", TimeFormatter.formatTime(5432L, AppPrecision.HUNDREDTHS))
        assertEquals("01:05.43", TimeFormatter.formatTime(65432L, AppPrecision.HUNDREDTHS))
        assertEquals("01:02:03.04", TimeFormatter.formatTime(3723040L, AppPrecision.HUNDREDTHS))

        assertEquals("00:00", TimeFormatter.formatTime(0L, AppPrecision.SECONDS))
        assertEquals("00:05", TimeFormatter.formatTime(5432L, AppPrecision.SECONDS))
        assertEquals("01:05", TimeFormatter.formatTime(65432L, AppPrecision.SECONDS))
        assertEquals("01:00:00", TimeFormatter.formatTime(3600000L, AppPrecision.SECONDS))

        assertEquals("00:00.00", TimeFormatter.formatTime(-1000L, AppPrecision.HUNDREDTHS))
    }

    @Test
    fun formatTime_customSubsecondSeparator_germanLocaleComma() {
        assertEquals("00:00,0", TimeFormatter.formatTime(0L, AppPrecision.TENTHS, ","))
        assertEquals("00:05,4", TimeFormatter.formatTime(5432L, AppPrecision.TENTHS, ","))
        assertEquals("01:05,4", TimeFormatter.formatTime(65432L, AppPrecision.TENTHS, ","))
        assertEquals("01:00:00,0", TimeFormatter.formatTime(3600000L, AppPrecision.TENTHS, ","))

        assertEquals("00:00,00", TimeFormatter.formatTime(0L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("00:05,43", TimeFormatter.formatTime(5432L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("01:05,43", TimeFormatter.formatTime(65432L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("01:02:03,04", TimeFormatter.formatTime(3723040L, AppPrecision.HUNDREDTHS, ","))

        assertEquals("00:00", TimeFormatter.formatTime(0L, AppPrecision.SECONDS, ","))
        assertEquals("00:05", TimeFormatter.formatTime(5432L, AppPrecision.SECONDS, ","))

        assertEquals("00:00,00", TimeFormatter.formatTime(-1000L, AppPrecision.HUNDREDTHS, ","))
    }

    @Test
    fun formatDeltaTime_fasterSlowerEqual_tenths() {
        assertEquals("-1,2s", TimeFormatter.formatDeltaTime(-1200L, ","))
        assertEquals("+0,8s", TimeFormatter.formatDeltaTime(800L, ","))
        assertEquals("0,0s", TimeFormatter.formatDeltaTime(0L, ","))

        assertEquals("-1.2s", TimeFormatter.formatDeltaTime(-1200L, "."))
        assertEquals("+0.8s", TimeFormatter.formatDeltaTime(800L, "."))
        assertEquals("0.0s", TimeFormatter.formatDeltaTime(0L, "."))
    }

    @Test
    fun formatDeltaTime_precisionVariations() {
        assertEquals("-1,20s", TimeFormatter.formatDeltaTime(-1200L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("+0,80s", TimeFormatter.formatDeltaTime(800L, AppPrecision.HUNDREDTHS, ","))
        assertEquals("0,00s", TimeFormatter.formatDeltaTime(0L, AppPrecision.HUNDREDTHS, ","))

        assertEquals("-1s", TimeFormatter.formatDeltaTime(-1200L, AppPrecision.SECONDS, ","))
        assertEquals("+0s", TimeFormatter.formatDeltaTime(800L, AppPrecision.SECONDS, ","))
        assertEquals("0s", TimeFormatter.formatDeltaTime(0L, AppPrecision.SECONDS, ","))
    }
}
