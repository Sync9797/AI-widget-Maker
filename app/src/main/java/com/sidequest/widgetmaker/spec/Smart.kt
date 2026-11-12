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
     * calendar.soon is "yes" from an hour before the next timed event until it
     * ends; calendar.soon.in is "in 25 min" or "Now".
     */
    fun calendarSoon(feeds: Map<String, String>, nowMs: Long, withinMinutes: Int = 60): Map<String, String> {
        val start = feeds["calendar.1.startMs"]?.toLongOrNull() ?: return mapOf("calendar.soon" to "")
        val end = feeds["calendar.1.endMs"]?.toLongOrNull() ?: start
        val minutes = (start - nowMs) / 60_000
        return when {
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
