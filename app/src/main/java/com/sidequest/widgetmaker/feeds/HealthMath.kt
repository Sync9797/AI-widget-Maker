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
    data class Sleep(val asleepMinutes: Long, val deepMinutes: Long, val remMinutes: Long)

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
    ): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val count = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
        steps?.let {
            out["health.steps"] = count.format(it)
            out["health.stepGoal"] = count.format(stepGoal)
            out["health.stepsPct"] = (it * 100 / max(1, stepGoal)).coerceAtMost(100).toString()
        }
        activeKcal?.let { out["health.kcal"] = count.format(it.roundToInt()) }
        totalKcal?.let { out["health.totalKcal"] = count.format(it.roundToInt()) }
        exerciseMinutes?.let { out["health.exercise"] = FeedParsers.duration((it * 60).toInt()) }
        // The latest reading, if it's from the last two hours.
        val nowMs = now.atZone(zone).toInstant().toEpochMilli()
        heart.maxByOrNull { it.timeMs }?.takeIf { nowMs - it.timeMs < 2 * 3600_000 }?.let { out["health.heart"] = it.bpm.toString() }
        if (heart.isNotEmpty()) out["health.heartMax"] = heart.maxOf { it.bpm }.toString()
        resting?.let { out["health.restingHeart"] = it.toString() }
        if (heart.isNotEmpty()) out["health.effort"] = "%.1f".format(Locale.US, effort(heart, resting ?: 60))
        workout?.let {
            out["health.workout"] = it.name
            out["health.workout.when"] = FeedParsers.whenText(it.start.toLocalDate(), it.start.toLocalTime(), now.toLocalDate())
            out["health.workout.duration"] = FeedParsers.duration((it.minutes * 60).toInt())
        }
        sleep?.let {
            out["health.sleep"] = hoursMinutes(it.asleepMinutes)
            out["health.sleep.deep"] = hoursMinutes(it.deepMinutes)
            out["health.sleep.rem"] = hoursMinutes(it.remMinutes)
        }
        return out
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
