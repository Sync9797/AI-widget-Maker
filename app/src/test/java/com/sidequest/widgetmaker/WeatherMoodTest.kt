package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.FeedParsers
import com.sidequest.widgetmaker.feeds.Place
import com.sidequest.widgetmaker.spec.WeatherMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherMoodTest {
    private val sunrise = 1_763_000_000_000L
    private val sunset = sunrise + 10 * 3_600_000L
    private fun feeds(code: Int) = mapOf("weather.code" to "$code", "weather.sunrise" to "$sunrise", "weather.sunset" to "$sunset")
    private val noon = sunrise + 5 * 3_600_000L

    @Test
    fun moodFollowsTheWeatherAndTheTime() {
        assertEquals(WeatherMood.day, WeatherMood.of(feeds(0), noon))
        assertEquals(WeatherMood.day, WeatherMood.of(feeds(2), noon))
        assertEquals(WeatherMood.cloudy, WeatherMood.of(feeds(3), noon))
        assertEquals(WeatherMood.cloudy, WeatherMood.of(feeds(45), noon))
        assertEquals(WeatherMood.rain, WeatherMood.of(feeds(61), noon))
        assertEquals(WeatherMood.rain, WeatherMood.of(feeds(95), noon))
        assertEquals(WeatherMood.snow, WeatherMood.of(feeds(73), noon))
        // Golden around sunrise and sunset (clear skies), on either side of it.
        assertEquals(WeatherMood.golden, WeatherMood.of(feeds(0), sunset - 20 * 60_000L))
        assertEquals(WeatherMood.golden, WeatherMood.of(feeds(1), sunset + 40 * 60_000L))
        assertEquals(WeatherMood.golden, WeatherMood.of(feeds(0), sunrise - 30 * 60_000L))
        // Rain wins over golden hour.
        assertEquals(WeatherMood.rain, WeatherMood.of(feeds(63), sunset))
        // Night keeps the widget's own look.
        assertNull(WeatherMood.of(feeds(0), sunset + 2 * 3_600_000L))
        assertNull(WeatherMood.of(feeds(61), sunrise - 3 * 3_600_000L))
        // Without sunrise and sunset, is_day decides.
        assertNull(WeatherMood.of(mapOf("weather.code" to "0", "weather.isDay" to "0"), noon))
        assertEquals(WeatherMood.day, WeatherMood.of(mapOf("weather.code" to "0", "weather.isDay" to "1"), noon))
        assertNull(WeatherMood.of(emptyMap(), noon))
    }

    @Test
    fun sunriseAndSunsetBecomeInstants() {
        val body = """{"utc_offset_seconds":-18000,"current":{"temperature_2m":38,"weather_code":0,"is_day":1},
            "daily":{"temperature_2m_max":[52],"temperature_2m_min":[31],"sunrise":["2026-11-14T06:58"],"sunset":["2026-11-14T17:04"]},
            "hourly":{"time":[]}}"""
        val v = FeedParsers.weather(body, Place("Lynchburg, Virginia", 37.4, -79.1), true)
        // 06:58 at UTC-5 is 11:58 UTC.
        assertEquals(java.time.Instant.parse("2026-11-14T11:58:00Z").toEpochMilli().toString(), v["weather.sunrise"])
        assertEquals("0", v["weather.code"])
        assertEquals("1", v["weather.isDay"])
    }
}
