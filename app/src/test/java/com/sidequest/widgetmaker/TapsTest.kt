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
            "Battery pill" to TapKind.battery,
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
