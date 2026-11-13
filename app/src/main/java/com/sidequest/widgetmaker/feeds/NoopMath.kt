package com.sidequest.widgetmaker.feeds

import java.time.ZoneId
import kotlin.math.roundToInt

/** Turns the not-NOOP fork's shared snapshot into widget values. Free of Android, so it can be tested. */
object NoopMath {
    /** The fork's snapshot; -1 marks a value it has not got yet. [series] is "epochSec:bpm,...". */
    data class Snapshot(
        val charge: Int,
        val rest: Int,
        val effort: Int,
        val heart: Int,
        val heartStale: Boolean,
        val battery: Int,
        val connected: Boolean,
        val updatedAtMs: Long,
        val series: String,
        /** Today's hourly stress, "epochSec:level:moving,...", "-" for an unscored hour. */
        val stress: String = "",
        /** The Coach brief (Markdown, as NOOP writes it) and when it was written; "" and 0 when none. */
        val brief: String = "",
        val briefAtMs: Long = 0L,
        /** The last 24 hours of heart rate from NOOP's history, 5-minute averages, "epochSec:bpm,...". */
        val day: String = "",
    )

    /**
     * NOOP's own scores and heart rate as the same values the widgets use (health.charge,
     * health.effort, health.rest, health.heart.last...), plus noop.* extras. Effort is NOOP's 0-100.
     */
    fun values(s: Snapshot, zone: ZoneId): Map<String, String> {
        val out = mutableMapOf<String, String>()
        if (s.charge >= 0) {
            out["health.charge"] = s.charge.toString()
            out["health.charge.label"] = HealthMath.chargeLabel(s.charge.coerceIn(1, 99))
        }
        if (s.rest >= 0) out["health.rest"] = s.rest.toString()
        if (s.effort >= 0) {
            out["health.effort"] = s.effort.toString()
            out["health.effort.pct"] = s.effort.toString()
        }
        if (s.heart > 0) {
            out["health.heart.last"] = s.heart.toString()
            if (!s.heartStale) out["health.heart"] = s.heart.toString()
        }
        // The day's history when the fork shares it (it holds everything offloaded from the strap);
        // else the 3-hour trace, which only grows while live heart rate streams.
        val day = pulses(s.day)
        if (day.size >= 2) out["noop.heart.day"] = "1"
        out.putAll(HealthMath.heartChart(if (day.size >= 2) day else pulses(s.series), zone))
        if (s.heart > 0) out["health.heart.last"] = s.heart.toString()
        out.putAll(stress(s.stress, zone))
        out.putAll(coach(s.brief, s.briefAtMs, zone))
        if (s.battery >= 0) out["noop.battery"] = "${s.battery}%"
        out["noop.connected"] = if (s.connected) "Connected" else "Not connected"
        out["health.source"] = "NOOP"
        return out
    }

    /** "epochSec:bpm,..." as pulses; malformed pairs and zero bpm are skipped. */
    private fun pulses(encoded: String): List<HealthMath.Pulse> = encoded.split(',').mapNotNull { p ->
        val i = p.indexOf(':')
        if (i <= 0) return@mapNotNull null
        val ts = p.substring(0, i).toLongOrNull() ?: return@mapNotNull null
        val bpm = p.substring(i + 1).toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        HealthMath.Pulse(ts * 1000, bpm)
    }

    /**
     * Today's stress (NOOP's 0-3 scale) as widget values: noop.stress (the latest scored hour),
     * .label, .pct (of 3), .avg, .peak and .peakAt, and for a chart .series (levels x100, so a chart
     * from 0 to 300 matches NOOP's) with .start, .mid and .end times. Empty before an hour is scored.
     */
    fun stress(encoded: String, zone: ZoneId): Map<String, String> {
        val points = encoded.split(',').mapNotNull { part ->
            val bits = part.split(':')
            if (bits.size != 3) return@mapNotNull null
            val ts = bits[0].toLongOrNull() ?: return@mapNotNull null
            val level = bits[1].toDoubleOrNull() ?: return@mapNotNull null
            ts to level.coerceIn(0.0, 3.0)
        }.sortedBy { it.first }
        if (points.isEmpty()) return emptyMap()
        fun one(v: Double) = String.format(java.util.Locale.US, "%.1f", v)
        val clock = java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)
        fun time(sec: Long) = java.time.Instant.ofEpochSecond(sec).atZone(zone).format(clock)
        val latest = points.last().second
        val peak = points.maxBy { it.second }
        val first = points.first().first
        val last = points.last().first
        return mapOf(
            "noop.stress" to one(latest),
            "noop.stress.label" to when {
                latest < 1.0 -> "Low"
                latest < 2.0 -> "Moderate"
                else -> "High"
            },
            "noop.stress.pct" to (latest / 3 * 100).roundToInt().toString(),
            "noop.stress.avg" to one(points.map { it.second }.average()),
            "noop.stress.peak" to one(peak.second),
            "noop.stress.peakAt" to time(peak.first),
            "noop.stress.series" to points.joinToString(",") { (it.second * 100).roundToInt().toString() },
            "noop.stress.start" to time(first),
            "noop.stress.mid" to time(first + (last - first) / 2),
            "noop.stress.end" to time(last),
        )
    }

    /**
     * The Coach brief as widget values, its Markdown made plain: noop.coach (all of it), .title (its
     * heading, else "Coach brief"), .body (without the heading), .summary (the first point) and .time
     * (when it was written: the time today, else the day). Empty when there is no brief.
     */
    fun coach(brief: String, atMs: Long, zone: ZoneId, today: java.time.LocalDate = java.time.LocalDate.now(zone)): Map<String, String> {
        val lines = brief.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyMap()
        fun plain(line: String) = line
            .replace(Regex("^#+\\s*"), "")
            .replace(Regex("^[*-]\\s+"), "• ")
            .replace("**", "")
            .replace("__", "")
            .trim()
        val heading = lines.first().takeIf { it.startsWith("#") }?.let(::plain)
        val body = (if (heading != null) lines.drop(1) else lines).map(::plain)
        val out = mutableMapOf(
            "noop.coach" to (listOfNotNull(heading) + body).joinToString("\n"),
            "noop.coach.title" to (heading ?: "Coach brief"),
            "noop.coach.body" to body.joinToString("\n"),
            "noop.coach.summary" to (body.firstOrNull()?.removePrefix("• ") ?: heading.orEmpty()),
        )
        if (atMs > 0) {
            val at = java.time.Instant.ofEpochMilli(atMs).atZone(zone)
            out["noop.coach.time"] = if (at.toLocalDate() == today) at.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US))
            else at.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM d", java.util.Locale.US))
        }
        return out
    }

    /**
     * One line saying what the fork shares right now, for the Check screen: whether its heart rate is
     * live, how long its 3-hour trace is, its scores and how old the snapshot is. [nowMs] is passed in.
     */
    fun describe(s: Snapshot, nowMs: Long): String {
        val points = s.series.split(',').count { it.indexOf(':') > 0 }
        val heart = when {
            s.heart <= 0 -> "no heart rate yet"
            s.heartStale -> "heart rate ${s.heart} (not live)"
            else -> "heart rate ${s.heart} live"
        }
        val dayPoints = s.day.split(',').count { it.indexOf(':') > 0 }
        val parts = mutableListOf(heart, when {
            dayPoints > 0 -> "${(dayPoints * 5 + 59) / 60}h of history"
            points > 0 -> "$points-minute trace"
            else -> "no trace yet"
        })
        if (s.charge >= 0) parts += "Charge ${s.charge}%"
        parts += if (s.stress.any { it.isDigit() }) "stress shared" else "no stress yet"
        parts += if (s.brief.isNotBlank()) "coach brief shared" else "no coach brief"
        parts += if (s.connected) "strap connected" else "strap not connected"
        val age = if (s.updatedAtMs <= 0L) "never updated" else {
            val min = ((nowMs - s.updatedAtMs) / 60_000L).coerceAtLeast(0L)
            when {
                min < 1 -> "updated just now"
                min < 60 -> "updated $min min ago"
                min < 48 * 60 -> "updated ${min / 60} h ago"
                else -> "updated ${min / (24 * 60)} days ago"
            }
        }
        return parts.joinToString(", ") + ", " + age
    }

    /**
     * Combines the fork's values with Health Connect's: the fork's scores win (they are NOOP's own,
     * not estimates), while a full day of heart rate from Health Connect beats the fork's 3 hours.
     */
    fun merge(healthConnect: Map<String, String>, noop: Map<String, String>): Map<String, String> {
        val out = healthConnect.toMutableMap()
        // Health Connect's full day beats the fork's 3-hour trace, but not the fork's own day history.
        val hcHasChart = healthConnect.containsKey("health.heart.series") && !noop.containsKey("noop.heart.day")
        noop.forEach { (k, v) ->
            val chartKey = k.startsWith("health.heart.") && k != "health.heart.last" || k == "health.heartMin" || k == "health.heartMid" || k == "health.heartMax"
            if (!(hcHasChart && chartKey)) out[k] = v
        }
        return out
    }
}
