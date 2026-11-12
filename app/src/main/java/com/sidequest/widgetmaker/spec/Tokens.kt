package com.sidequest.widgetmaker.spec

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Live values a design's text can show. The widget fills them in each time it
 * redraws (when you open the app, and at least every 30 minutes); for a clock
 * that ticks every minute, use a [ClockNode] instead.
 */
data class Tokens(
    val now: LocalDateTime,
    /** 0-100, or null if unknown. */
    val battery: Int?,
    val charging: Boolean,
    val locale: Locale = Locale.getDefault(),
    /** Latest values from the data sources (weather, crypto, traffic, news, events). */
    val feeds: Map<String, String> = emptyMap(),
) {
    fun values(): Map<String, String> = feeds + mapOf(
        "date" to now.format(DateTimeFormatter.ofPattern("EEE d MMM", locale)),
        "day" to now.format(DateTimeFormatter.ofPattern("EEEE", locale)),
        "month" to now.format(DateTimeFormatter.ofPattern("MMMM", locale)),
        "dayOfMonth" to now.dayOfMonth.toString(),
        "year" to now.year.toString(),
        "greeting" to greeting(now.hour),
        "battery" to (battery?.toString() ?: "--"),
        "charging" to if (charging) "Charging" else "",
    )

    /**
     * Replaces {token} with its value. A data-source value that isn't there
     * yet (not set up, or not fetched) shows as "--"; other unknown tokens
     * are left as they are.
     */
    fun fill(text: String): String {
        val values = values()
        return TOKEN.replace(text) { m ->
            val name = m.groupValues[1]
            values[name] ?: if (FEED_PREFIXES.any { name.startsWith(it) }) "--" else m.value
        }
    }

    /** Up (true), down (false) or unknown (null), from a value like "+2.4%" or a token holding one. */
    fun trend(value: String): Boolean? {
        // A sign followed by a digit: "--" (no data yet) is neither up nor down.
        val text = fill(value).trim()
        return when {
            UP.containsMatchIn(text) -> true
            DOWN.containsMatchIn(text) -> false
            else -> null
        }
    }

    /** A progress value: a number, or a token such as {battery}. */
    fun progress(value: String): Float {
        val number = fill(value).trim().removeSuffix("%").toFloatOrNull() ?: 0f
        return (number / 100f).coerceIn(0f, 1f)
    }

    companion object {
        private val TOKEN = Regex("\\{([\\w.]+)\\}")
        private val UP = Regex("^\\+\\s*[0-9]")
        private val DOWN = Regex("^-\\s*[0-9]")
        private val FEED_PREFIXES = listOf("weather.", "crypto.", "traffic.", "news.", "events.", "calendar.", "health.", "mq.", "music.")

        val NAMES = listOf("date", "day", "month", "dayOfMonth", "year", "greeting", "battery", "charging")

        fun greeting(hour: Int) = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            in 18..21 -> "Good evening"
            else -> "Good night"
        }
    }
}
