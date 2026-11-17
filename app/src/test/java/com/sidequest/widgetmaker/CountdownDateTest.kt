package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.ui.usDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownDateTest {
    @Test
    fun countdownDatesReadMonthDayYear() {
        assertEquals("12-25-26", usDate("2026-12-25"))
        assertEquals("03-07-27", usDate("2027-03-07"))
        // Anything that isn't a date is shown as it is.
        assertEquals("soon", usDate("soon"))
    }
}
