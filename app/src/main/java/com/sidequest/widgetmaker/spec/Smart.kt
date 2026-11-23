package com.sidequest.widgetmaker.spec

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Values worked out each time a widget draws (not when data is fetched), for
 * widgets that change with the time: the time of day, an event about to
 * start, and countdowns. Free of Android, so it can be tested.
 */
object Smart {
    /** "morning" (5-11), "afternoon" (12-16), "evening" (17-21) or "night": the one that applies is "yes", the rest empty. */
    fun timeOfDay(now: LocalDateTime): Map<String, String> {
        val part = when (now.hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
        return listOf("morning", "afternoon", "evening", "night").associate { "time.$it" to if (it == part) "yes" else "" }
    }

    /**
     * How long until an event that starts at [startMs] and ends at [endMs], short enough for a small tile: "45M"
     * under an hour, "3H" (to the nearest hour) after that, "Now" while it's on, and empty once it's over.
     */
    fun until(startMs: Long, endMs: Long, nowMs: Long): String {
        val minutes = (startMs - nowMs) / 60_000
        return when {
            nowMs >= endMs -> ""
            nowMs >= startMs -> "Now"
            minutes < 60 -> "${maxOf(1, minutes)}M"
            else -> "${(minutes + 30) / 60}H"
        }
    }

    /**
     * calendar.later is "yes" while the next event is still today (on now, or later on), so a briefing shows it;
     * once today's are done, it's empty and the briefing turns to tomorrow's first event.
     */
    fun calendarLater(feeds: Map<String, String>, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Map<String, String> {
        val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().toString()
        val end = feeds["calendar.1.endMs"]?.toLongOrNull()
        val later = feeds["calendar.1"].isNullOrBlank().not() && feeds["calendar.1.dayIso"] == today && (end == null || nowMs < end)
        return mapOf("calendar.later" to if (later) "yes" else "")
    }

    /** calendar.1.in and calendar.tomorrow.in: how long until the next timed event, and tomorrow's first (see [until]). */
    fun calendarUntil(feeds: Map<String, String>, nowMs: Long): Map<String, String> = listOf("calendar.1", "calendar.tomorrow").associate { key ->
        val start = feeds["$key.startMs"]?.toLongOrNull()
        val end = feeds["$key.endMs"]?.toLongOrNull() ?: start
        "$key.in" to if (start == null || end == null) "" else until(start, end, nowMs)
    }

    /**
     * calendar.soon is "yes" from an hour before the next timed event until it
     * ends; calendar.soon.in is "in 25 min" or "Now".
     */
    fun calendarSoon(feeds: Map<String, String>, nowMs: Long, withinMinutes: Int = 60): Map<String, String> = calendarUntil(feeds, nowMs) + calendarLater(feeds, nowMs) + run {
        val start = feeds["calendar.1.startMs"]?.toLongOrNull() ?: return@run mapOf("calendar.soon" to "")
        val end = feeds["calendar.1.endMs"]?.toLongOrNull() ?: start
        val minutes = (start - nowMs) / 60_000
        when {
            nowMs >= end -> mapOf("calendar.soon" to "")
            nowMs >= start -> mapOf("calendar.soon" to "yes", "calendar.soon.in" to "Now")
            minutes <= withinMinutes -> mapOf("calendar.soon" to "yes", "calendar.soon.in" to "in ${maxOf(1, minutes)} min")
            else -> mapOf("calendar.soon" to "")
        }
    }

    data class Countdown(val name: String, val date: LocalDate)

    /**
     * countdown.N (name), countdown.N.days ("12", for big numbers),
     * countdown.N.text ("12 days", "Tomorrow", "Today!") and countdown.N.date
     * ("Dec 25"), soonest first; days already past are left out.
     */
    fun countdowns(list: List<Countdown>, today: LocalDate): Map<String, String> {
        val out = mutableMapOf<String, String>()
        list.filter { !it.date.isBefore(today) }.sortedBy { it.date }.take(5).forEachIndexed { i, c ->
            val days = ChronoUnit.DAYS.between(today, c.date)
            val n = i + 1
            out["countdown.$n"] = c.name
            out["countdown.$n.days"] = days.toString()
            out["countdown.$n.unit"] = if (days == 1L) "day" else "days"
            out["countdown.$n.text"] = when (days) {
                0L -> "Today!"
                1L -> "Tomorrow"
                else -> "$days days"
            }
            out["countdown.$n.date"] = c.date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US))
        }
        return out
    }
}
