package com.ear.watches.data

import com.ear.watches.data.model.AppPrecision
import com.ear.watches.data.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
