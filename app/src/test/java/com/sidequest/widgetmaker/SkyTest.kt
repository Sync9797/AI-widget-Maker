package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.spec.Sky
import com.sidequest.widgetmaker.spec.WidgetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class SkyTest {
    private val zone = ZoneId.of("America/Chicago")
    private fun ms(h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, 14, h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun followsTheClockWithoutWeather() {
        assertEquals(Sky.night, Sky.of(emptyMap(), ms(3), zone))
        assertEquals(Sky.dawn, Sky.of(emptyMap(), ms(7, 20), zone))
        assertEquals(Sky.day, Sky.of(emptyMap(), ms(13), zone))
        assertEquals(Sky.dusk, Sky.of(emptyMap(), ms(18), zone))
        assertEquals(Sky.night, Sky.of(emptyMap(), ms(22), zone))
    }

    @Test
    fun followsTodaysSunriseAndSunset() {
        val feeds = mapOf("weather.sunrise" to ms(6, 45).toString(), "weather.sunset" to ms(17, 25).toString())
        assertEquals(Sky.dawn, Sky.of(feeds, ms(6, 0), zone))
        assertEquals(Sky.dusk, Sky.of(feeds, ms(17, 0), zone))
        assertEquals(Sky.night, Sky.of(feeds, ms(19, 0), zone))
        // Yesterday's times are ignored: the clock's are used.
        val stale = mapOf("weather.sunrise" to (ms(6, 45) - 86_400_000).toString())
        assertEquals(Sky.dawn, Sky.of(stale, ms(7, 30), zone))
    }

    @Test
    fun themesThatTakeMoods() {
        assertTrue(WidgetTheme.takesMoods(WidgetTheme.timeOfDay))
        assertFalse(WidgetTheme.takesMoods(WidgetTheme.tonal))
        assertFalse(WidgetTheme.takesMoods(WidgetTheme.outline))
        assertTrue(WidgetTheme.liquid.frosted && WidgetTheme.outline.showsWallpaper)
    }
}
