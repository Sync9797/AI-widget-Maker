package com.sidequest.widgetmaker.spec

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Lays out a [MonthNode] as plain rows and columns of text, so the home screen widget and the in-app
 * preview draw the same calendar with the parts they already have.
 */
object MonthGrid {
    /** One week of a month: its week number and its seven days (null outside the month). */
    data class Week(val number: Int, val days: List<LocalDate?>)

    /** [month]'s weeks, starting on [firstDay]. */
    fun weeks(month: YearMonth, firstDay: DayOfWeek): List<Week> {
        val fields = WeekFields.of(firstDay, 1)
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - firstDay.value + 7) % 7
        var start = first.minusDays(lead.toLong())
        val out = mutableListOf<Week>()
        while (!start.isAfter(month.atEndOfMonth())) {
            val days = (0 until 7).map { start.plusDays(it.toLong()).takeIf { d -> YearMonth.from(d) == month } }
            out += Week(start.get(fields.weekOfWeekBasedYear()), days)
            start = start.plusWeeks(1)
        }
        return out
    }

    /** The days with events, from "calendar.days" (dates like 2026-11-14, comma separated). */
    fun eventDays(feeds: Map<String, String>): Set<LocalDate> =
        feeds["calendar.days"].orEmpty().split(',').mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }.toSet()

    fun firstDay(node: MonthNode) = if (node.firstDay.equals("monday", ignoreCase = true)) DayOfWeek.MONDAY else DayOfWeek.SUNDAY

    /** The calendar as rows and columns, for [today]. */
    fun expand(node: MonthNode, today: LocalDate, feeds: Map<String, String>, locale: Locale = Locale.getDefault()): Node {
        val first = firstDay(node)
        val busy = if (node.events) eventDays(feeds) else emptySet()
        val faded = fade(node.color, 0xB3)
        fun month(m: YearMonth, grow: Boolean): Node {
            val header = if (node.header) listOf(
                TextNode(m.month.getDisplayName(TextStyle.SHORT, locale).uppercase(locale).trimEnd('.'), size = node.size + 4, color = node.color, weight = Weight.bold, align = HAlign.center, maxLines = 1),
                SpacerNode(size = maxOf(2, node.size / 3)),
            ) else emptyList()
            val numberCell = if (node.weekNumbers) listOf(TextNode(" ", size = node.size - 2, color = faded, align = HAlign.center, maxLines = 1, grow = true)) else emptyList()
            val names = RowNode(
                children = numberCell + (0 until 7).map { i ->
                    val day = first.plus(i.toLong())
                    TextNode(day.getDisplayName(TextStyle.NARROW, locale), size = node.size, color = node.color, weight = Weight.medium, align = HAlign.center, maxLines = 1, grow = true)
                },
            )
            val weeks = weeks(m, first).map { week ->
                RowNode(
                    grow = true,
                    verticalAlign = VAlign.center,
                    children = (if (node.weekNumbers) listOf(TextNode(week.number.toString(), size = node.size - 2, color = fade(node.color, 0x66), align = HAlign.center, maxLines = 1, grow = true)) else emptyList()) +
                        week.days.map { day -> dayCell(node, day, today, busy) },
                )
            }
            return ColumnNode(children = header + names + weeks, align = HAlign.center, grow = grow)
        }
        val now = YearMonth.from(today)
        val count = node.months.coerceIn(1, 2)
        val root = if (count == 1) month(now, grow = true)
        else RowNode(children = listOf(month(now, grow = true), SpacerNode(size = 12), month(now.plusMonths(1), grow = true)), verticalAlign = VAlign.top)
        // Keeps the node's own place in its row or column, size rules and tap.
        return when (root) {
            is ColumnNode -> root.copy(grow = node.grow, show = node.show, tap = node.tap ?: TapKind.calendar.name)
            is RowNode -> root.copy(grow = node.grow, show = node.show, tap = node.tap ?: TapKind.calendar.name)
            else -> root
        }
    }

    private fun dayCell(node: MonthNode, day: LocalDate?, today: LocalDate, busy: Set<LocalDate>): Node {
        if (day == null) return TextNode(" ", size = node.size, color = node.color, align = HAlign.center, maxLines = 1, grow = true)
        val isToday = day == today
        val text = TextNode(
            day.dayOfMonth.toString(),
            size = node.size,
            color = node.color,
            // An event day keeps its accent in every theme (a fixed level: see levelColor).
            levelOf = if (day in busy && !isToday) "1" else null,
            levels = if (day in busy && !isToday) listOf(Level(0f, node.accent)) else null,
            weight = if (isToday || day in busy) Weight.bold else Weight.medium,
            align = HAlign.center,
            maxLines = 1,
            highlight = isToday,
            grow = !isToday,
        )
        // Today's block is only as wide as its number, centered in its cell.
        return if (isToday) RowNode(children = listOf(text), align = HAlign.center, grow = true) else text
    }

    /** [color] ("#AARRGGBB" or "#RRGGBB") at [alpha]. */
    private fun fade(color: String, alpha: Int): String {
        val rgb = Colors.argb(color, fallback = 0xFFFFFFFF.toInt()) and 0xFFFFFF
        return "#%02X%06X".format(alpha, rgb)
    }
}
