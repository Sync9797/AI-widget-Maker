package com.sidequest.widgetmaker.spec

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The Time of day theme's colors: the sky's, through the day. Deep blue at night, warm at dawn and
 * dusk, bright blue by day. [top] and [bottom] are the panel's gradient (ARGB).
 */
enum class Sky(val top: Int, val bottom: Int) {
    night(0xFF0E1638.toInt(), 0xFF26305E.toInt()),
    dawn(0xFF4B4F8F.toInt(), 0xFFE39678.toInt()),
    day(0xFF2F6CCC.toInt(), 0xFF6FA6E4.toInt()),
    dusk(0xFF47397A.toInt(), 0xFFDB7357.toInt());

    companion object {
        /** How long either side of sunrise and sunset counts as dawn or dusk. */
        const val TWILIGHT_MS = 60 * 60_000L

        /**
         * The sky at [nowMs]: by today's sunrise and sunset from the weather [feeds] when known,
         * otherwise by the clock (sunrise at 7, sunset at half past 6).
         */
        fun of(feeds: Map<String, String>, nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Sky {
            val today = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
            fun at(time: LocalTime) = today.atTime(time).atZone(zone).toInstant().toEpochMilli()
            val sunrise = feeds["weather.sunrise"]?.toLongOrNull()?.takeIf { sameDay(it, today, zone) } ?: at(LocalTime.of(7, 0))
            val sunset = feeds["weather.sunset"]?.toLongOrNull()?.takeIf { sameDay(it, today, zone) } ?: at(LocalTime.of(18, 30))
            return when {
                kotlin.math.abs(nowMs - sunrise) <= TWILIGHT_MS -> dawn
                kotlin.math.abs(nowMs - sunset) <= TWILIGHT_MS -> dusk
                nowMs in sunrise..sunset -> day
                else -> night
            }
        }

        private fun sameDay(ms: Long, day: LocalDate, zone: ZoneId) = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate() == day
    }
}
