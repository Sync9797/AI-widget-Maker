package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.FeedParsers
import com.sidequest.widgetmaker.feeds.HealthMath
import com.sidequest.widgetmaker.music.MusicMath
import com.sidequest.widgetmaker.music.NowPlaying
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.Level
import com.sidequest.widgetmaker.spec.Show
import com.sidequest.widgetmaker.spec.SleepStages
import com.sidequest.widgetmaker.spec.Smart
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import com.sidequest.widgetmaker.spec.levelColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class LevelsAndRulesTest {
    private fun tokens(feeds: Map<String, String> = emptyMap(), battery: Int? = 80) =
        Tokens(LocalDateTime.of(2026, 11, 12, 8, 0), battery, false, feeds = feeds)

    private val charge = listOf(Level(0f, "#FFFF0000"), Level(34f, "#FFFFFF00"), Level(67f, "#FF00FF00"))

    @Test
    fun levelsPickTheStepReached() {
        assertEquals("#FFFF0000", levelColor("{health.charge}", charge, tokens(mapOf("health.charge" to "20"))))
        assertEquals("#FFFFFF00", levelColor("{health.charge}", charge, tokens(mapOf("health.charge" to "34"))))
        assertEquals("#FF00FF00", levelColor("{health.charge}", charge, tokens(mapOf("health.charge" to "91"))))
        assertEquals("#FF00FF00", levelColor("{weather.h1.rain}", charge, tokens(mapOf("weather.h1.rain" to "80%"))))
        // No number yet: the node's own color.
        assertNull(levelColor("{health.charge}", charge, tokens()))
        assertEquals("#FFFF0000", levelColor("{battery}", charge, tokens(battery = 12)))
    }

    @Test
    fun levelsSurviveParsing() {
        val json = """{"name":"x","root":{"type":"text","text":"{battery}","levelOf":"{battery}","levels":[{"from":0,"color":"#FFFF0000"},{"from":20,"color":"nope"}]}}"""
        val node = DesignParser.parse(json).root as TextNode
        assertEquals(listOf(Level(0f, "#FFFF0000")), node.levels)
    }

    @Test
    fun ifEmptyShowsOnlyWhenAllAreEmpty() {
        val rule = Show(ifEmpty = "{calendar.soon}{music.playing}")
        assertTrue(rule.has(tokens(mapOf("calendar.soon" to "", "music.playing" to ""))))
        assertTrue(rule.has(tokens()))
        assertFalse(rule.has(tokens(mapOf("calendar.soon" to "", "music.playing" to "yes"))))
        assertFalse(Show(ifSet = "{music.playing}", ifEmpty = "{calendar.soon}").has(tokens(mapOf("music.playing" to "yes", "calendar.soon" to "yes"))))
    }
}

class SmartTest {
    @Test
    fun timeOfDay() {
        val v = Smart.timeOfDay(LocalDateTime.of(2026, 11, 12, 8, 30))
        assertEquals("yes", v["time.morning"])
        assertEquals("", v["time.evening"])
        assertEquals("yes", Smart.timeOfDay(LocalDateTime.of(2026, 11, 12, 23, 0))["time.night"])
    }

    @Test
    fun eventSoon() {
        val start = 1_000_000_000L
        val feeds = mapOf("calendar.1.startMs" to start.toString(), "calendar.1.endMs" to (start + 3_600_000).toString())
        assertEquals("", Smart.calendarSoon(feeds, start - 2 * 3_600_000)["calendar.soon"])
        assertEquals("in 25 min", Smart.calendarSoon(feeds, start - 25 * 60_000)["calendar.soon.in"])
        assertEquals("Now", Smart.calendarSoon(feeds, start + 60_000)["calendar.soon.in"])
        assertEquals("", Smart.calendarSoon(feeds, start + 2 * 3_600_000)["calendar.soon"])
    }

    @Test
    fun countdowns() {
        val today = LocalDate.of(2026, 11, 12)
        val v = Smart.countdowns(
            listOf(
                Smart.Countdown("Christmas", LocalDate.of(2026, 12, 25)),
                Smart.Countdown("Tomorrow thing", today.plusDays(1)),
                Smart.Countdown("Gone", today.minusDays(1)),
            ),
            today,
        )
        assertEquals("Tomorrow thing", v["countdown.1"])
        assertEquals("Tomorrow", v["countdown.1.text"])
        assertEquals("day", v["countdown.1.unit"])
        assertEquals("43", v["countdown.2.days"])
        assertEquals("Dec 25", v["countdown.2.date"])
        assertNull(v["countdown.3"])
    }
}

class SleepAndStepsTest {
    @Test
    fun stagesRoundTripAndMerge() {
        val parts = listOf(SleepStages.Part('L', 30), SleepStages.Part('L', 15), SleepStages.Part('D', 40), SleepStages.Part('R', 20))
        assertEquals("L45,D40,R20", SleepStages.encode(parts))
        assertEquals(3, SleepStages.decode("L45,D40,R20,X9,bad").size)
    }

    @Test
    fun sleepAndStepValues() {
        val v = HealthMath.values(
            steps = 3508, stepGoal = 8000, activeKcal = null, totalKcal = null, exerciseMinutes = null, heart = emptyList(),
            resting = null, workout = null, now = LocalDateTime.of(2026, 11, 12, 8, 0),
            sleep = HealthMath.Sleep(450, 80, 95, listOf(SleepStages.Part('L', 250), SleepStages.Part('A', 25)), averageMinutes = 420),
        )
        assertEquals("4,492", v["health.stepsLeft"])
        assertEquals("4,492 steps to go", v["health.stepsLeft.text"])
        assertEquals("L250,A25", v["health.sleep.stages"])
        assertEquals("7h 0m", v["health.sleep.avg"])
        assertEquals("30m more than usual", v["health.sleep.vsAvg"])
        assertEquals("25m", v["health.sleep.awake"])
    }
}

class MusicProgressTest {
    @Test
    fun progressMovesWhilePlaying() {
        val song = NowPlaying("Song", "Artist", "", "app", "App", playing = true, positionMs = 60_000, updatedAt = 1_000, durationMs = 240_000)
        val v = MusicMath.values(song, hasAccess = true, elapsedNow = 61_000)
        assertEquals("2:00", v["music.elapsed"])
        assertEquals("4:00", v["music.length"])
        assertEquals("50", v["music.progress"])
        // Paused: it stays put.
        assertEquals("1:00", MusicMath.values(song.copy(playing = false), true, 500_000)["music.elapsed"])
    }
}

class RssSafetyTest {
    @Test
    fun aFeedWithADoctypeStillParsesWithoutEntities() {
        val body = """<?xml version="1.0"?>
            <!DOCTYPE rss [<!ENTITY boom "BOOM">]>
            <rss><channel><item><title>Park opens - KXAN</title><source>KXAN</source><link>https://example.com/a</link></item></channel></rss>"""
        val items = FeedParsers.headlines(body)
        assertEquals(1, items.size)
        assertEquals("Park opens", items[0].title)
    }
}

class CategoriesTest {
    @Test
    fun everyStarterHasASection() {
        val groups = Starters.all.groupBy { it.category }
        assertTrue(groups.keys.size >= 7)
        assertTrue(groups[Starters.Category.game].orEmpty().all { it.title.startsWith("Side Quest") })
        assertEquals(Starters.Category.everyday, Starters.all.first { it.title == "Countdown" }.category)
        assertEquals(Starters.Category.health, Starters.all.first { it.title == "Sleep" }.category)
    }
}

class PhoneTest {
    private val hour = 3_600_000L

    @Test
    fun batteryLifeFromDrain() {
        val samples = listOf(
            com.sidequest.widgetmaker.feeds.PhoneMath.Sample(0, 80),
            com.sidequest.widgetmaker.feeds.PhoneMath.Sample(2 * hour, 72),
        )
        assertEquals(4.0, com.sidequest.widgetmaker.feeds.PhoneMath.drainPerHour(samples, 2 * hour)!!, 0.01)
        val v = com.sidequest.widgetmaker.feeds.PhoneMath.values(
            level = 72, charging = false, samples = samples, nowMs = 2 * hour, chargeLeftMs = null, tempTenthsC = 310,
            fahrenheit = false, ramUsed = 5L * 1_073_741_824, ramTotal = 8L * 1_073_741_824, storageUsed = 100L * 1_073_741_824, storageTotal = 256L * 1_073_741_824,
        )
        assertEquals("About 18 h left", v["battery.left"])
        assertEquals("4.0%/h", v["battery.drain"])
        assertEquals("31°C", v["battery.temp"])
        assertEquals("5.0 GB of 8.0 GB", v["phone.ram"])
        assertEquals("62", v["phone.ramPct"])
        assertEquals("100 GB of 256 GB", v["phone.storage"])
    }

    @Test
    fun noGuessUntilItHasDrained() {
        val v = com.sidequest.widgetmaker.feeds.PhoneMath.values(80, false, listOf(com.sidequest.widgetmaker.feeds.PhoneMath.Sample(0, 80)), 60_000, null, null, true, null, null, null, null)
        assertEquals("Working out battery life…", v["battery.left"])
        val charging = com.sidequest.widgetmaker.feeds.PhoneMath.values(50, true, emptyList(), 0, 40 * 60_000L, null, true, null, null, null, null)
        assertEquals("Full in 40 min", charging["battery.left"])
    }
}

class MyDesignsTest {
    private val design = Starters.all.first().design

    @Test
    fun yoursVersusReadyMade() {
        assertTrue(com.sidequest.widgetmaker.spec.SavedDesign("1", "a cozy rainy-day clock", design, 0).madeByYou)
        assertFalse(com.sidequest.widgetmaker.spec.SavedDesign("2", Starters.all.first().title, design, 0).madeByYou)
        assertTrue(com.sidequest.widgetmaker.spec.SavedDesign("3", Starters.all.first().title, design, 0, custom = true).madeByYou)
    }

    @Test
    fun olderSavesStillLoad() {
        val list = kotlinx.serialization.builtins.ListSerializer(com.sidequest.widgetmaker.spec.SavedDesign.serializer())
        val old = """[{"id":"x","prompt":"neon clock","design":${DesignParser.encode(design)},"createdAt":5}]"""
        val loaded = DesignParser.json.decodeFromString(list, old)
        assertNull(loaded[0].custom)
        assertTrue(loaded[0].madeByYou)
    }
}

class FillTest {
    private fun tokens(battery: Int?) = Tokens(LocalDateTime.of(2026, 11, 12, 8, 0), battery, false)
    private val fill = com.sidequest.widgetmaker.spec.Fill(
        "{battery}", "#FF9FD8CF", listOf(Level(0f, "#FFFF5C5C"), Level(16f, "#FFFFB74D")), levelsUpTo = 30f,
    )

    @Test
    fun fillsToTheLevelInTheAccentUntilLow() {
        val (full, color) = com.sidequest.widgetmaker.spec.fillLevel(fill, tokens(80))
        assertEquals(0.8f, full, 0.001f)
        assertEquals(com.sidequest.widgetmaker.spec.Role.Accent to "#FF9FD8CF", color)
        assertEquals(com.sidequest.widgetmaker.spec.Role.Level to "#FFFFB74D", com.sidequest.widgetmaker.spec.fillLevel(fill, tokens(25)).second)
        assertEquals(com.sidequest.widgetmaker.spec.Role.Level to "#FFFF5C5C", com.sidequest.widgetmaker.spec.fillLevel(fill, tokens(9)).second)
    }

    @Test
    fun theBatteryStarterFills() {
        val design = Starters.all.first { it.title == "Battery fill" }.design
        assertEquals("{battery}", design.fill?.value)
        assertEquals(design, DesignParser.parse(DesignParser.encode(design)))
    }
}

class WidgetSizeTest {
    @Test
    fun pillsStartSlim() {
        val design = Starters.all.first { it.title == "Battery fill" }.design
        assertEquals(com.sidequest.widgetmaker.widget.WidgetSize.square, com.sidequest.widgetmaker.widget.WidgetSize.suggested(design))
        assertEquals(
            com.sidequest.widgetmaker.widget.WidgetSize.slim,
            com.sidequest.widgetmaker.widget.WidgetSize.suggested(design.copy(shape = com.sidequest.widgetmaker.spec.WidgetShape.pill)),
        )
    }
}

class AutoScaleTest {
    private val design = Starters.all.first { it.title == "Daily briefing" }.design
    private fun panel(w: Int, h: Int, look: com.sidequest.widgetmaker.spec.Look) = com.sidequest.widgetmaker.spec.Shapes.panel(w, h, design, look)

    @Test
    fun growsWithTheShorterSide() {
        val more = com.sidequest.widgetmaker.spec.Look()
        // A cover-screen 2×2 stays as it is.
        assertEquals(1f, com.sidequest.widgetmaker.spec.Shapes.scale(panel(165, 170, more), more), 0.001f)
        // An open-screen 2×2 (wider, a bit taller) grows a little.
        assertEquals(1.2f, com.sidequest.widgetmaker.spec.Shapes.scale(panel(340, 198, more), more), 0.01f)
        // A 4×4 grows up to 1.25× when it also shows more, up to 2× when it only scales.
        assertEquals(1.25f, com.sidequest.widgetmaker.spec.Shapes.scale(panel(340, 340, more), more), 0.001f)
        val scale = more.copy(autoScale = com.sidequest.widgetmaker.spec.AutoScale.scale)
        assertEquals(2f, com.sidequest.widgetmaker.spec.Shapes.scale(panel(340, 340, scale), scale), 0.01f)
        val off = more.copy(autoScale = com.sidequest.widgetmaker.spec.AutoScale.off)
        assertEquals(1f, com.sidequest.widgetmaker.spec.Shapes.scale(panel(340, 340, off), off), 0.001f)
    }

    @Test
    fun oneRowWidgetsNeverScale() {
        val look = com.sidequest.widgetmaker.spec.Look(autoScale = com.sidequest.widgetmaker.spec.AutoScale.scale)
        assertEquals(1f, com.sidequest.widgetmaker.spec.Shapes.scale(panel(400, 110, look), look), 0.001f)
    }
}

class ObituaryTest {
    private fun h(title: String, source: String = "WSET", link: String = "https://wset.com/a") =
        FeedParsers.Headline(title, source, null, link)

    @Test
    fun deathNoticesAreLeftOut() {
        assertTrue(FeedParsers.isObituary(h("John Smith Obituary (1941 - 2026) - Lynchburg, VA")))
        assertTrue(FeedParsers.isObituary(h("Mary Jones passed away peacefully at home")))
        assertTrue(FeedParsers.isObituary(h("Funeral services set for longtime coach")))
        assertTrue(FeedParsers.isObituary(h("Robert Lee", source = "Legacy.com")))
        assertTrue(FeedParsers.isObituary(h("Robert Lee", link = "https://www.newsadvance.com/obituaries/robert-lee")))
        assertFalse(FeedParsers.isObituary(h("City council approves new downtown park")))
    }

    @Test
    fun newsSkipsThemAndMutedWords() {
        val body = """<rss><channel>
            <item><title>Jane Doe Obituary - News &amp; Advance</title><source>News &amp; Advance</source><link>https://x/1</link></item>
            <item><title>New park opens downtown - WSET</title><source>WSET</source><link>https://x/2</link></item>
            <item><title>Election results are in - WDBJ</title><source>WDBJ</source><link>https://x/3</link></item>
            <item><title>Food truck festival returns - WSET</title><source>WSET</source><link>https://x/4</link></item>
            </channel></rss>"""
        val v = FeedParsers.news(body, muted = listOf("Election"))
        assertEquals("New park opens downtown", v["news.1"])
        assertEquals("Food truck festival returns", v["news.2"])
        assertNull(v["news.3"])
        // Turned off: obituaries show again.
        assertEquals("Jane Doe Obituary", FeedParsers.news(body, hideObituaries = false)["news.1"])
    }
}
