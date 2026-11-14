package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.FeedParsers
import com.sidequest.widgetmaker.spec.IconName
import com.sidequest.widgetmaker.spec.IconNode
import com.sidequest.widgetmaker.spec.Tokens
import com.sidequest.widgetmaker.widget.Meteocons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class MeteoconsTest {
    private val frames = File("src/main/res/drawable-nodpi")

    @Test
    fun everyWeatherCodeHasAMeteoconWithFrames() {
        val codes = listOf(0, 1, 2, 3, 45, 48, 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99, 42)
        for (code in codes) for (day in listOf(true, false)) {
            val name = FeedParsers.meteocon(code, day)
            assertTrue("$code/$day → $name", File(frames, "mc_${name}_00.webp").exists())
            // Every loop has its frames numbered without gaps (30 or 60 of them, at 10 a second).
            val count = frames.listFiles()!!.count { Regex("mc_${name}_\\d\\d\\.webp").matches(it.name) }
            assertTrue("$name has $count frames", count == 30 || count == 60)
        }
        // Each has its animation as one layout listing every frame (one layout id is all a widget sends).
        for (code in codes) for (day in listOf(true, false)) {
            val name = FeedParsers.meteocon(code, day)
            val layout = File("src/main/res/layout/mc_anim_$name.xml")
            assertTrue("layout for $name", layout.exists())
            val count = frames.listFiles()!!.count { Regex("mc_${name}_\\d\\d\\.webp").matches(it.name) }
            assertEquals("frames in mc_anim_$name", count, Regex("@drawable/mc_${name}_\\d\\d").findAll(layout.readText()).count())
        }
        assertEquals("clear_night", FeedParsers.meteocon(0, false))
        assertEquals("partly_cloudy_day_rain", FeedParsers.meteocon(80, true))
        assertEquals("thunderstorms_hail", FeedParsers.meteocon(99, false))
    }

    @Test
    fun weatherIconsPickTheirMeteocon() {
        val tokens = Tokens(LocalDateTime.of(2026, 11, 14, 9, 0), null, false, feeds = mapOf("weather.mc" to "rain", "weather.h2.mc" to "clear_day"))
        assertEquals("rain", Meteocons.nameFor(IconNode(IconName.weather), tokens))
        assertEquals("rain", Meteocons.nameFor(IconNode(IconName.sun, live = "{weather.icon}"), tokens))
        assertEquals("clear_day", Meteocons.nameFor(IconNode(IconName.sun, live = "{weather.h2.icon}"), tokens))
        assertNull(Meteocons.nameFor(IconNode(IconName.heart), tokens))
        assertNull(Meteocons.nameFor(IconNode(IconName.sun, live = "{weather.h4.icon}"), tokens))
        assertTrue(Meteocons.isNow(IconNode(IconName.weather)))
        assertFalse(Meteocons.isNow(IconNode(IconName.sun, live = "{weather.h2.icon}")))
    }
}
