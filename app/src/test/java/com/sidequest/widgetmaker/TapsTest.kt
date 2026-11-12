package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.spec.ClockNode
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.TapAction
import com.sidequest.widgetmaker.spec.TapKind
import com.sidequest.widgetmaker.spec.Taps
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class TapsTest {
    private val tokens = Tokens(
        LocalDateTime.of(2026, 11, 11, 15, 0), battery = 80, charging = false,
        feeds = mapOf(
            "news.2.link" to "https://news.example.com/park",
            "calendar.1.link" to "calendar-event:42:1000:2000",
        ),
    )

    @Test
    fun headlinesAndEntriesOpenTheirOwnLink() {
        assertEquals("{news.2.link}", Taps.forNode(TextNode("{news.2}")))
        assertEquals("{events.1.link}", Taps.forNode(TextNode("{events.1} · {events.1.venue}")))
        assertEquals("{calendar.3.link}", Taps.forNode(TextNode("{calendar.3}")))
        assertNull(Taps.forNode(TextNode("{weather.temp}")))
        assertEquals(TapAction.Link("https://news.example.com/park"), Taps.resolve("{news.2.link}", tokens))
        assertEquals(TapAction.CalendarEvent(42, 1000, 2000), Taps.resolve("{calendar.1.link}", tokens))
        // No data yet: nothing to open.
        assertNull(Taps.resolve("{news.4.link}", tokens))
    }

    @Test
    fun timesOpenTheClockAndDatesTheCalendar() {
        assertEquals("clock", Taps.forNode(ClockNode("h:mm")))
        assertEquals("clock", Taps.forNode(ClockNode("HH:mm")))
        assertEquals("calendar", Taps.forNode(ClockNode("EEE d MMM")))
        assertEquals(TapAction.Open(TapKind.clock), Taps.resolve("clock", tokens))
    }

    @Test
    fun aDesignsOwnTapWins() {
        assertEquals("weather", Taps.forNode(TextNode("{news.1}", tap = "weather")))
        val design = DesignParser.parse("""{"root":{"type":"column","tap":"crypto","children":[{"type":"text","text":"{weather.temp}"}]}}""")
        assertEquals("crypto", Taps.forWidget(design))
    }

    @Test
    fun eachStarterOpensTheRightApp() {
        val expected = mapOf(
            "Big clock" to TapKind.clock,
            "Clock and weather" to TapKind.weather,
            "Weather forecast" to TapKind.weather,
            "Crypto ticker" to TapKind.crypto,
            "Commute" to TapKind.commute,
            "Local news" to TapKind.news,
            "Nearby events" to TapKind.events,
            "Up next" to TapKind.calendar,
            "Big date" to TapKind.calendar,
            "Day cookie" to TapKind.calendar,
            "Hourly weather" to TapKind.weather,
            "Battery fill" to TapKind.battery,
            "Agenda card" to TapKind.calendar,
            "Steps" to TapKind.health,
            "Activity" to TapKind.health,
            "NOOP recovery" to TapKind.health,
            "Now playing" to TapKind.music,
            "Music mini" to TapKind.music,
            "Sleep" to TapKind.health,
            "Daily briefing" to TapKind.weather,
            "Smart stack" to TapKind.weather,
            "Side Quest eggs" to TapKind.game,
            "Side Quest adventures" to TapKind.game,
            "Side Quest day" to TapKind.game,
            "Side Quest quests" to TapKind.game,
            "Side Quest progress" to TapKind.game,
            "Side Quest party" to TapKind.game,
            "Battery" to TapKind.battery,
        )
        Starters.all.forEach { s -> assertEquals(s.title, expected[s.title], Taps.mainKind(s.design)) }
    }

    @Test
    fun unknownTapsAreIgnored() {
        assertNull(Taps.resolve("teleport", tokens))
        assertNull(Taps.resolve("calendar-event:oops", tokens))
    }
}

class GameBridgeTest {
    private val nowMs = 1_800_000_000_000L
    private val body = """{"v":2,"hero":"Aaron","level":23,"steps":3508,"torches":4,"torchCap":15,"torchSteps":480,"stepsPerTorch":1000,
        "eggs":[{"id":"e1","name":"Ember Pup","progress":100,"left":"0 m","ready":true},{"id":"e2","name":"Tide Sprite","progress":40,"left":"1.2 km","ready":false}],
        "adventures":[{"id":"a1","title":"Clear the cellar","team":"Ember","startMs":${nowMs - 1000},"endMs":${nowMs + 3_600_000}},
                      {"id":"a2","title":"Scout","team":"Pip","startMs":${nowMs - 7_200_000},"endMs":${nowMs - 1}}],
        "quests":[{"id":"q1","title":"Write report","when":"Today 3 PM"}],"questsOpen":6,
        "party":[{"name":"Ember","level":24,"hp":52,"maxHp":60}],
        "bounty":[{"text":"Walk 40,000 steps","progress":12400,"goal":40000}],"bountyReady":false,
        "focus":{"startMs":${nowMs - 600_000},"endMs":${nowMs + 600_000},"minutes":20}}"""

    @Test
    fun valuesAndButtons() {
        val v = com.sidequest.widgetmaker.feeds.MonsterQuestMath.values(body, nowMs)
        assertEquals("mq-action:hatch:e1", v["mq.egg.1.hatch"])
        assertEquals("", v["mq.egg.2.hatch"])
        assertEquals("", v["mq.adv.1.claim"])
        assertEquals("mq-action:claim:a2", v["mq.adv.2.claim"])
        assertEquals("mq-action:complete:q1", v["mq.quest.1.done"])
        assertEquals("6", v["mq.quests"])
        assertEquals("52/60", v["mq.party.1.hp"])
        assertEquals("86", v["mq.party.1.hpPct"])
        assertEquals("12,400 / 40,000", v["mq.bounty.1.progress"])
        assertEquals("520 steps to the next torch", v["mq.torch.next"])
        assertEquals("Phone-free · 10 min left", v["mq.focus"])
        assertEquals("50", v["mq.focus.pct"])
    }

    @Test
    fun tapsOpenScreensAndAct() {
        val tokens = Tokens(java.time.LocalDateTime.now(), null, false, feeds = com.sidequest.widgetmaker.feeds.MonsterQuestMath.values(body, nowMs))
        assertEquals("{mq.egg.2.link}", Taps.forNode(TextNode("  {mq.egg.2}")))
        assertEquals(TapAction.GameScreen("eggs"), Taps.resolve("{mq.egg.2.link}", tokens))
        assertEquals(TapAction.GameAction("hatch", "e1"), Taps.resolve("{mq.egg.1.hatch}", tokens))
        assertEquals(TapAction.GameAction("complete", "q1"), Taps.resolve("{mq.quest.1.done}", tokens))
        // Not ready yet: nothing to tap.
        assertNull(Taps.resolve("{mq.egg.2.hatch}", tokens))
        assertNull(Taps.resolve("mq-action:explode:x", tokens))
        // Game widgets open the screen they're about.
        val eggs = com.sidequest.widgetmaker.spec.Starters.all.first { it.title == "Side Quest eggs" }.design
        assertEquals("mq-open:eggs", Taps.forWidget(eggs))
        val quests = com.sidequest.widgetmaker.spec.Starters.all.first { it.title == "Side Quest quests" }.design
        assertEquals("mq-open:quests", Taps.forWidget(quests))
    }
}
