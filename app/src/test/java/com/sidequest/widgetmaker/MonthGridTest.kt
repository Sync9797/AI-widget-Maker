package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.FeedParsers
import com.sidequest.widgetmaker.spec.ColumnNode
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.MonthGrid
import com.sidequest.widgetmaker.spec.MonthNode
import com.sidequest.widgetmaker.spec.Node
import com.sidequest.widgetmaker.spec.RowNode
import com.sidequest.widgetmaker.spec.TapAction
import com.sidequest.widgetmaker.spec.Taps
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

class MonthGridTest {
    private fun texts(node: Node): List<TextNode> = when (node) {
        is ColumnNode -> node.children.flatMap(::texts)
        is RowNode -> node.children.flatMap(::texts)
        is TextNode -> listOf(node)
        else -> emptyList()
    }

    @Test
    fun weeksMatchOneUi() {
        // November 2026 starts on a Sunday: five weeks, numbered 45 to 49 like One UI's.
        val weeks = MonthGrid.weeks(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(5, weeks.size)
        assertEquals(45, weeks.first().number)
        assertEquals(LocalDate.of(2026, 11, 1), weeks.first().days[0])
        assertEquals(listOf(29, 30), weeks.last().days.filterNotNull().map { it.dayOfMonth })
        // December starts on a Tuesday: two blanks first.
        val dec = MonthGrid.weeks(YearMonth.of(2026, 12), DayOfWeek.SUNDAY)
        assertEquals(listOf(null, null), dec.first().days.take(2))
        // Starting on Monday moves Sunday the 1st to the end of the first week.
        assertEquals(LocalDate.of(2026, 11, 1), MonthGrid.weeks(YearMonth.of(2026, 11), DayOfWeek.MONDAY).first().days[6])
    }

    @Test
    fun todayIsMarkedAndEventDaysTakeTheAccent() {
        val feeds = mapOf("calendar.days" to "2026-11-15,2026-11-14,nonsense")
        val grid = MonthGrid.expand(MonthNode(months = 2), LocalDate.of(2026, 11, 14), feeds, Locale.US)
        val all = texts(grid)
        val today = all.single { it.highlight }
        assertEquals("14", today.text)
        assertEquals(null, today.levelOf) // Today's own mark wins over its event color.
        val fifteenth = all.first { it.text == "15" }
        assertEquals("1", fifteenth.levelOf)
        assertTrue(all.any { it.text == "NOV" } && all.any { it.text == "DEC" })
        assertEquals("calendar", (grid as RowNode).tap)
    }

    @Test
    fun scrollingAgendaListsTodayAndTheNextThreeDays() {
        val now = LocalDateTime.of(2026, 11, 22, 9, 0)
        fun event(title: String, start: LocalDateTime) = FeedParsers.CalendarEvent(title, start, start.plusHours(1), false, "")
        val out = FeedParsers.calendar(
            listOf(
                event("Meeting", LocalDateTime.of(2026, 11, 22, 19, 0)),
                event("Work out", LocalDateTime.of(2026, 11, 23, 12, 0)),
                event("Dentist", LocalDateTime.of(2026, 11, 25, 15, 0)),
                event("Too far", LocalDateTime.of(2026, 11, 26, 9, 0)),
            ),
            now,
        )
        assertEquals("22", out["calendar.d0.num"])
        assertEquals("Today", out["calendar.d0.label"])
        assertEquals("Meeting", out["calendar.d0.1"])
        assertEquals("7:00 PM", out["calendar.d0.1.time"])
        assertEquals("23", out["calendar.d1.num"])
        assertEquals("Tomorrow", out["calendar.d1.label"])
        assertEquals("Work out", out["calendar.d1.1"])
        // A day without events says so; the list stops three days out.
        assertEquals("No events", out["calendar.d2.none"])
        assertEquals("Wed", out["calendar.d3.label"])
        assertEquals("Dentist", out["calendar.d3.1"])
        assertEquals(null, out["calendar.d4.num"])
        assertEquals(null, out.filterKeys { it.startsWith("calendar.d") }.values.firstOrNull { it == "Too far" })
    }

    @Test
    fun agendaGroupsByDay() {
        val now = LocalDateTime.of(2026, 11, 14, 8, 0)
        fun event(title: String, start: LocalDateTime, allDay: Boolean = false) =
            FeedParsers.CalendarEvent(title, start, start.plusHours(1), allDay, "")
        val out = FeedParsers.calendar(
            listOf(
                event("Car bill", LocalDateTime.of(2026, 11, 14, 0, 0), allDay = true).copy(end = LocalDateTime.of(2026, 11, 15, 0, 0)),
                event("CVFP", LocalDateTime.of(2026, 11, 15, 7, 0)),
                event("Church", LocalDateTime.of(2026, 11, 15, 10, 0)),
            ),
            now,
            busyDays = setOf(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 14)),
        )
        assertEquals("Today, Sat, Nov 14", out["calendar.1.heading"])
        assertEquals("All day", out["calendar.1.time"])
        assertEquals("NOV 15", out["calendar.2.day"])
        assertEquals("Tomorrow, Sun, Nov 15", out["calendar.2.heading"])
        assertEquals("7:00 AM", out["calendar.2.time"])
        assertEquals("", out["calendar.3.day"])
        assertEquals("", out["calendar.3.heading"])
        assertEquals("2026-11-14,2026-11-15", out["calendar.days"])
    }

    @Test
    fun plusStartsANewEventAndMonthsParse() {
        val tokens = Tokens(LocalDateTime.of(2026, 11, 14, 9, 0), null, false)
        assertEquals(TapAction.NewEvent, Taps.resolve("calendar-new", tokens))
        val design = DesignParser.parse("""{"name":"M","root":{"type":"month","months":5,"size":99,"firstDay":"Monday"}}""")
        val month = design.root as MonthNode
        assertEquals(2, month.months)
        assertEquals(28, month.size)
        assertEquals("monday", month.firstDay)
    }
}
