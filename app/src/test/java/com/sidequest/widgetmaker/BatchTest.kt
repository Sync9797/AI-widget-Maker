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
