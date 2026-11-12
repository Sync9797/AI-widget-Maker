package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.FeedParsers
import com.sidequest.widgetmaker.feeds.HealthMath
import com.sidequest.widgetmaker.feeds.Place
import com.sidequest.widgetmaker.music.MusicMath
import com.sidequest.widgetmaker.music.NowPlaying
import com.sidequest.widgetmaker.spec.ArtNode
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.TapAction
import com.sidequest.widgetmaker.spec.TapKind
import com.sidequest.widgetmaker.spec.Taps
import com.sidequest.widgetmaker.spec.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class NoopTest {
    private val now = LocalDateTime.of(2026, 11, 12, 8, 0)

    @Test
    fun baselineNeedsFiveDaysAndIgnoresOddOnes() {
        assertNull(HealthMath.baseline(listOf(50.0, 60.0, 55.0, 58.0)))
        assertEquals(58.0, HealthMath.baseline(listOf(50.0, 60.0, 55.0, 58.0, 200.0))!!, 0.01)
    }

    @Test
    fun chargeFollowsHrvAndRestingHeartRate() {
        val usual = HealthMath.charge(60.0, 60.0, 55, 55.0, 450)
        val great = HealthMath.charge(75.0, 60.0, 50, 55.0, 480)
        val poor = HealthMath.charge(42.0, 60.0, 62, 55.0, 300)
        assertTrue("$poor < $usual < $great", poor < usual && usual < great)
        assertEquals("Charged", HealthMath.chargeLabel(great))
        assertEquals("Drained", HealthMath.chargeLabel(poor))
        assertTrue(HealthMath.charge(500.0, 60.0, 40, 55.0, 600) <= 99)
        assertTrue(HealthMath.charge(5.0, 60.0, 90, 55.0, 60) >= 1)
    }

    @Test
    fun noopValuesBecomeTokens() {
        val v = HealthMath.values(
            steps = null, stepGoal = 8000, activeKcal = null, totalKcal = null, exerciseMinutes = null,
            heart = emptyList(), resting = 52, workout = null, sleep = null, now = now,
            extras = HealthMath.Extras(hrvToday = 64.4, hrvBaseline = 60.0, restingBaseline = 55.0, spo2 = 97.2, respiratoryRate = 14.83, vo2max = 47.6, source = "NOOP"),
        )
        assertEquals("64 ms", v["health.hrv"])
        assertEquals("97%", v["health.spo2"])
        assertEquals("14.8", v["health.respRate"])
        assertEquals("48", v["health.vo2max"])
        assertEquals("NOOP", v["health.source"])
        assertTrue(v["health.charge"]!!.toInt() > 55)
    }

    @Test
    fun noChargeWithoutABaseline() {
        val v = HealthMath.values(null, 8000, null, null, null, emptyList(), 52, null, null, now, extras = HealthMath.Extras(hrvToday = 60.0))
        assertNull(v["health.charge"])
        assertEquals("60 ms", v["health.hrv"])
    }

    @Test
    fun saysWhenHrvIsNotAllowed() {
        val v = HealthMath.values(null, 8000, null, null, null, emptyList(), 52, null, null, now, extras = HealthMath.Extras(hrvAllowed = false))
        assertEquals("Open the app to allow HRV", v["health.charge.label"])
        assertNull(v["health.charge"])
    }

    @Test
    fun noopStarterOpensHealth() {
        val design = Starters.all.first { it.title == "NOOP recovery" }.design
        assertEquals(TapKind.health, Taps.mainKind(design))
    }
}

class MusicTest {
    private val tokens = Tokens(LocalDateTime.of(2026, 11, 12, 8, 0), 80, false)

    @Test
    fun nowPlayingValues() {
        val v = MusicMath.values(NowPlaying("Clair de Lune", "Debussy", "Suite", "com.spotify.music", "Spotify", playing = true), hasAccess = true)
        assertEquals("Clair de Lune", v["music.title"])
        assertEquals("Clair de Lune · Debussy", v["music.line"])
        assertEquals("pause", v["music.playIcon"])
        assertEquals("Playing", v["music.state"])
        val paused = MusicMath.values(NowPlaying("", "", "", "com.podcast", "Pocket Casts", playing = false), hasAccess = true)
        assertEquals("Pocket Casts", paused["music.title"])
        assertEquals("play", paused["music.playIcon"])
    }

    @Test
    fun nothingPlayingOrNoAccess() {
        assertEquals("Nothing playing", MusicMath.values(null, hasAccess = true)["music.title"])
        assertEquals("Allow music access", MusicMath.values(null, hasAccess = false)["music.title"])
        // Not shared yet: a placeholder, not a raw {token}.
        assertEquals("--", tokens.fill("{music.artist}"))
    }

    @Test
    fun buttonsResolveToMediaCommands() {
        assertEquals(TapAction.Media("toggle"), Taps.resolve("music:toggle", tokens))
        assertEquals(TapAction.Media("next"), Taps.resolve("music:next", tokens))
        assertNull(Taps.resolve("music:rewind", tokens))
    }

    @Test
    fun artNodeParsesAndIsClamped() {
        val design = DesignParser.parse("""{"name":"x","root":{"type":"art","size":900,"cornerRadius":12}}""")
        assertEquals(200, (design.root as ArtNode).size)
    }

    @Test
    fun musicStartersOpenTheMusicApp() {
        listOf("Now playing", "Music mini").forEach { title ->
            assertEquals(title, TapKind.music, Taps.mainKind(Starters.all.first { it.title == title }.design))
        }
    }
}

class WeatherOutlookTest {
    private val place = Place("Lynchburg, VA", 37.4, -79.1)

    private fun body(rain: List<Int>, codes: List<Int> = rain.map { if (it > 50) 61 else 2 }): String {
        val times = (0 until rain.size).joinToString(",") { "\"2026-11-12T%02d:00\"".format(20 + it - if (20 + it >= 24) 24 else 0).let { t -> if (20 + it >= 24) t.replace("2026-11-12", "2026-11-13") else t } }
        return """{"current":{"temperature_2m":60,"apparent_temperature":58,"weather_code":2,"is_day":0,"wind_speed_10m":4},
            "daily":{"time":["2026-11-12"],"temperature_2m_max":[65],"temperature_2m_min":[48],"precipitation_probability_max":[30],"weather_code":[2]},
            "hourly":{"time":[$times],"temperature_2m":[${rain.joinToString(",") { "55" }}],"weather_code":[${codes.joinToString(",")}],
            "is_day":[${rain.joinToString(",") { "0" }}],"precipitation_probability":[${rain.joinToString(",")}]}}"""
    }

    @Test
    fun saysWhenRainIsPossible() {
        val v = FeedParsers.weather(body(listOf(0, 1, 2, 30, 2, 2)), place, fahrenheit = true)
        assertEquals("Rain possible tonight", v["weather.outlook"])
        assertEquals("21:00", v["weather.h1.time24"])
        assertEquals("9 PM", v["weather.h1.time"])
    }

    @Test
    fun saysLikelyAndDry() {
        assertEquals("Rain likely tonight", FeedParsers.weather(body(listOf(0, 80, 10)), place, true)["weather.outlook"])
        assertEquals("Dry for the next 12 hours", FeedParsers.weather(body(listOf(0, 5, 10)), place, true)["weather.outlook"])
    }

    @Test
    fun periods() {
        val now = LocalDateTime.of(2026, 11, 12, 9, 0)
        assertEquals("this afternoon", FeedParsers.period(now, now.withHour(14)))
        assertEquals("this evening", FeedParsers.period(now, now.withHour(19)))
        assertEquals("tomorrow morning", FeedParsers.period(now, now.plusDays(1).withHour(8)))
        assertEquals("tonight", FeedParsers.period(now, now.plusDays(1).withHour(2)))
    }
}

class SquareCellTest {
    private val design = Starters.all.first { it.title == "Weather forecast" }.design
    private val look = com.sidequest.widgetmaker.spec.Look()

    @Test
    fun aTall2x2CellGetsASquarePanel() {
        val p = com.sidequest.widgetmaker.spec.Shapes.panel(170, 260, design, look)
        assertEquals(170, p.width)
        assertEquals(170, p.height)
    }

    @Test
    fun wideAndTallWidgetsKeepTheirShape() {
        assertEquals(160, com.sidequest.widgetmaker.spec.Shapes.panel(320, 160, design, look).height)
        assertEquals(340, com.sidequest.widgetmaker.spec.Shapes.panel(170, 340, design, look).height)
        assertEquals(260, com.sidequest.widgetmaker.spec.Shapes.panel(170, 260, design, look.copy(squareCells = false)).height)
    }
}
