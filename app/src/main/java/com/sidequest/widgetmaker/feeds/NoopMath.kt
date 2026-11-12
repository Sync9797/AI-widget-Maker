package com.sidequest.widgetmaker.feeds

import java.time.ZoneId

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
        val points = s.series.split(',').mapNotNull { p ->
            val i = p.indexOf(':')
            if (i <= 0) return@mapNotNull null
            val ts = p.substring(0, i).toLongOrNull() ?: return@mapNotNull null
            val bpm = p.substring(i + 1).toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            HealthMath.Pulse(ts * 1000, bpm)
        }
        out.putAll(HealthMath.heartChart(points, zone))
        if (s.heart > 0) out["health.heart.last"] = s.heart.toString()
        if (s.battery >= 0) out["noop.battery"] = "${s.battery}%"
        out["noop.connected"] = if (s.connected) "Connected" else "Not connected"
        out["health.source"] = "NOOP"
        return out
    }

    /**
     * Combines the fork's values with Health Connect's: the fork's scores win (they are NOOP's own,
     * not estimates), while a full day of heart rate from Health Connect beats the fork's 3 hours.
     */
    fun merge(healthConnect: Map<String, String>, noop: Map<String, String>): Map<String, String> {
        val out = healthConnect.toMutableMap()
        val hcHasChart = healthConnect.containsKey("health.heart.series")
        noop.forEach { (k, v) ->
            val chartKey = k.startsWith("health.heart.") && k != "health.heart.last" || k == "health.heartMin" || k == "health.heartMid" || k == "health.heartMax"
            if (!(hcHasChart && chartKey)) out[k] = v
        }
        return out
    }
}
