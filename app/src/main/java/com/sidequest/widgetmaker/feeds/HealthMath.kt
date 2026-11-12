package com.sidequest.widgetmaker.feeds

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns health readings into widget values. Free of Android and Health
 * Connect, so it can be tested.
 */
object HealthMath {
    data class Pulse(val timeMs: Long, val bpm: Int)
    data class Workout(val name: String, val start: LocalDateTime, val minutes: Long)
    data class Sleep(
        val asleepMinutes: Long,
        val deepMinutes: Long,
        val remMinutes: Long,
        /** The night's stages in order (see [com.sidequest.widgetmaker.spec.SleepStages]). */
        val stages: List<com.sidequest.widgetmaker.spec.SleepStages.Part> = emptyList(),
        /** Your average over the last week, in minutes asleep. */
        val averageMinutes: Long? = null,
        val bedtime: LocalDateTime? = null,
        val wake: LocalDateTime? = null,
        /** The last nights, oldest first, by the day you woke up: minutes asleep. */
        val week: List<Pair<java.time.LocalDate, Long>> = emptyList(),
    )

    /** Readings NOOP (and other WHOOP-style apps) share: HRV and resting heart rate against your own baseline, and more. */
    data class Extras(
        val hrvToday: Double? = null,
        val hrvBaseline: Double? = null,
        val restingBaseline: Double? = null,
        val spo2: Double? = null,
        val respiratoryRate: Double? = null,
        val vo2max: Double? = null,
        /** "NOOP" when the data comes from NOOP. */
        val source: String? = null,
        /** False when Health Connect doesn't let this app read HRV yet (so Charge can't be worked out). */
        val hrvAllowed: Boolean = true,
    )

    /** The middle of the last readings (needs at least 5 days), so one odd day doesn't move it. */
    fun baseline(values: List<Double>): Double? {
        if (values.size < 5) return null
        val sorted = values.sorted()
        return sorted[sorted.size / 2]
    }

    /**
     * A recovery score from 1 to 99, like WHOOP's (NOOP's "Charge"), which
     * NOOP keeps to itself, so this is an estimate: HRV above your baseline,
     * resting heart rate below it, and a full night's sleep each raise it.
     */
    fun charge(hrv: Double, hrvBaseline: Double, resting: Int?, restingBaseline: Double?, asleepMinutes: Long?): Int {
        var score = 55.0 + 110.0 * (hrv / hrvBaseline - 1.0)
        if (resting != null && restingBaseline != null) score += 2.5 * (restingBaseline - resting)
        if (asleepMinutes != null) score += 25.0 * ((asleepMinutes / 480.0).coerceAtMost(1.1) - 0.85)
        return score.roundToInt().coerceIn(1, 99)
    }

    fun chargeLabel(charge: Int) = when {
        charge >= 67 -> "Charged"
        charge >= 34 -> "Steady"
        else -> "Drained"
    }

    fun values(
        steps: Long?,
        stepGoal: Int,
        activeKcal: Double?,
        totalKcal: Double?,
        exerciseMinutes: Long?,
        heart: List<Pulse>,
        resting: Int?,
        workout: Workout?,
        sleep: Sleep?,
        now: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault(),
        extras: Extras = Extras(),
    ): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val count = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
        steps?.let {
            out["health.steps"] = count.format(it)
            out["health.stepGoal"] = count.format(stepGoal)
            out["health.stepsPct"] = (it * 100 / max(1, stepGoal)).coerceAtMost(100).toString()
            val left = (stepGoal - it).coerceAtLeast(0)
            out["health.stepsLeft"] = count.format(left)
            out["health.stepsLeft.text"] = if (left == 0L) "Step goal reached!" else "${count.format(left)} steps to go"
        }
        activeKcal?.let { out["health.kcal"] = count.format(it.roundToInt()) }
        totalKcal?.let { out["health.totalKcal"] = count.format(it.roundToInt()) }
        exerciseMinutes?.let { out["health.exercise"] = FeedParsers.duration((it * 60).toInt()) }
        // The latest reading, if it's from the last two hours.
        val nowMs = now.atZone(zone).toInstant().toEpochMilli()
        heart.maxByOrNull { it.timeMs }?.takeIf { nowMs - it.timeMs < 2 * 3600_000 }?.let { out["health.heart"] = it.bpm.toString() }
        if (heart.isNotEmpty()) out["health.heartMax"] = heart.maxOf { it.bpm }.toString()
        out.putAll(heartChart(heart, zone))
        resting?.let { out["health.restingHeart"] = it.toString() }
        if (heart.isNotEmpty()) {
            val e = effort(heart, resting ?: 60)
            out["health.effort"] = "%.1f".format(Locale.US, e)
            out["health.effort.pct"] = (e / 21 * 100).roundToInt().coerceIn(0, 100).toString()
        }
        workout?.let {
            out["health.workout"] = it.name
            out["health.workout.when"] = FeedParsers.whenText(it.start.toLocalDate(), it.start.toLocalTime(), now.toLocalDate())
            out["health.workout.duration"] = FeedParsers.duration((it.minutes * 60).toInt())
        }
        extras.hrvToday?.let { out["health.hrv"] = "${it.roundToInt()} ms" }
        extras.spo2?.let { out["health.spo2"] = "${it.roundToInt()}%" }
        extras.respiratoryRate?.let { out["health.respRate"] = "%.1f".format(Locale.US, it) }
        extras.vo2max?.let { out["health.vo2max"] = it.roundToInt().toString() }
        extras.source?.let { out["health.source"] = it }
        // Say why Charge is missing, right on the widget, when it's a permission.
        if (!extras.hrvAllowed) out["health.charge.label"] = "Open the app to allow HRV"
        if (extras.hrvToday != null && extras.hrvBaseline != null) {
            val c = charge(extras.hrvToday, extras.hrvBaseline, resting, extras.restingBaseline, sleep?.asleepMinutes)
            out["health.charge"] = c.toString()
            out["health.charge.label"] = chargeLabel(c)
        }
        sleep?.let {
            out["health.sleep"] = hoursMinutes(it.asleepMinutes)
            out["health.sleep.deep"] = hoursMinutes(it.deepMinutes)
            out["health.sleep.rem"] = hoursMinutes(it.remMinutes)
            if (it.stages.isNotEmpty()) {
                out["health.sleep.stages"] = com.sidequest.widgetmaker.spec.SleepStages.encode(it.stages)
                fun stage(c: Char) = it.stages.filter { p -> p.stage == c }.sumOf { p -> p.minutes }.toLong()
                out["health.sleep.light"] = hoursMinutes(stage('L'))
                out["health.sleep.awake"] = hoursMinutes(stage('A'))
            }
            // Rest, like NOOP's: last night's sleep against an 8-hour night.
            out["health.rest"] = (it.asleepMinutes * 100 / 480).coerceIn(0, 100).toString()
            val clock = java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            it.bedtime?.let { t -> out["health.sleep.bed"] = t.format(clock) }
            it.wake?.let { t -> out["health.sleep.wake"] = t.format(clock) }
            // The last 7 nights, n1 oldest to n7 last night; pct against 9 hours, for bars.
            it.week.takeLast(7).forEachIndexed { i, (day, minutes) ->
                val n = i + 1
                out["health.sleep.n$n.day"] = day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", Locale.US))
                out["health.sleep.n$n"] = hoursMinutes(minutes)
                out["health.sleep.n$n.pct"] = (minutes * 100 / 540).coerceIn(0, 100).toString()
            }
            it.averageMinutes?.let { avg ->
                out["health.sleep.avg"] = hoursMinutes(avg)
                val diff = it.asleepMinutes - avg
                out["health.sleep.vsAvg"] = when {
                    kotlin.math.abs(diff) < 5 -> "About your average"
                    diff > 0 -> "${hoursMinutes(diff)} more than usual"
                    else -> "${hoursMinutes(-diff)} less than usual"
                }
                out["health.sleep.score"] = (it.asleepMinutes * 100 / 480).coerceIn(0, 100).toString()
            }
        }
        return out
    }

    /**
     * Today's heart rate for a chart: health.heart.series (up to 96 points,
     * each the average of its stretch of the day), health.heart.last (latest
     * reading), health.heartMin/Mid, and the chart's start, middle and end
     * times (health.heart.start/.mid/.end, like "7:55 AM").
     */
    fun heartChart(heart: List<Pulse>, zone: ZoneId, points: Int = 96): Map<String, String> {
        if (heart.size < 2) return emptyMap()
        val sorted = heart.sortedBy { it.timeMs }
        val first = sorted.first().timeMs
        val last = sorted.last().timeMs
        val span = maxOf(1L, last - first)
        val buckets = Array(points) { mutableListOf<Int>() }
        sorted.forEach { p -> buckets[((p.timeMs - first) * (points - 1) / span).toInt()] += p.bpm }
        // Empty stretches (no readings) carry the last value, so the line doesn't drop to zero.
        var previous = sorted.first().bpm
        val series = buckets.map { b -> (if (b.isEmpty()) previous else b.average().roundToInt()).also { previous = it } }
        val clock = java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        fun time(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(zone).format(clock)
        val min = sorted.minOf { it.bpm }
        val max = sorted.maxOf { it.bpm }
        return mapOf(
            "health.heart.series" to series.joinToString(","),
            "health.heart.last" to sorted.last().bpm.toString(),
            "health.heartMin" to min.toString(),
            "health.heartMid" to ((min + max) / 2).toString(),
            "health.heart.start" to time(first),
            "health.heart.mid" to time(first + span / 2),
            "health.heart.end" to time(last),
        )
    }

    fun hoursMinutes(minutes: Long): String = if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"

    /**
     * An effort score for the day from 0 to 21, like WHOOP's strain (which
     * WHOOP keeps to itself, so this is an estimate): time spent at each heart
     * rate above resting, weighted the harder it is (Banister's TRIMP), on the
     * same 0-21 curve that flattens as it climbs.
     */
    fun effort(heart: List<Pulse>, resting: Int, maxHr: Int = 190): Double {
        if (heart.size < 2) return 0.0
        val sorted = heart.sortedBy { it.timeMs }
        var trimp = 0.0
        for (i in 1 until sorted.size) {
            // Gaps over 10 minutes are missing data, not effort.
            val minutes = ((sorted[i].timeMs - sorted[i - 1].timeMs) / 60_000.0).coerceAtMost(10.0)
            val reserve = ((sorted[i - 1].bpm - resting).toDouble() / (maxHr - resting)).coerceIn(0.0, 1.0)
            trimp += minutes * reserve * 0.64 * exp(1.92 * reserve)
        }
        return 21.0 * (1 - exp(-trimp / 120.0))
    }
}
