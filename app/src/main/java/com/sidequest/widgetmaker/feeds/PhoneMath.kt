package com.sidequest.widgetmaker.feeds

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Phone values for battery and system widgets. Free of Android, so the
 * battery-life estimate can be tested.
 */
object PhoneMath {
    /** A battery reading: when, and the level (0-100). */
    data class Sample(val timeMs: Long, val level: Int)

    /**
     * How fast the battery drains, in % per hour, from readings taken while
     * unplugged: the last 6 hours of them, once it has dropped 2% over 20
     * minutes or more. Null until then.
     */
    fun drainPerHour(samples: List<Sample>, nowMs: Long): Double? {
        val recent = samples.filter { nowMs - it.timeMs <= 6 * 3_600_000L }.sortedBy { it.timeMs }
        if (recent.size < 2) return null
        val first = recent.first()
        val last = recent.last()
        val hours = (last.timeMs - first.timeMs) / 3_600_000.0
        val dropped = first.level - last.level
        if (hours < 1.0 / 3 || dropped < 2) return null
        return dropped / hours
    }

    fun hoursMinutes(hours: Double): String {
        val total = (hours * 60).roundToInt().coerceAtLeast(1)
        return when {
            total < 60 -> "$total min"
            total % 60 == 0 || total >= 600 -> "${(total + 30) / 60} h"
            else -> "${total / 60} h ${total % 60} min"
        }
    }

    fun gb(bytes: Long): String {
        val g = bytes / 1_073_741_824.0
        return if (g >= 10) "${g.roundToInt()} GB" else String.format(Locale.US, "%.1f GB", g)
    }

    /**
     * battery.left ("About 9 h left", "Full in 40 min"), battery.drain
     * ("3.1%/h"), battery.temp, phone.ram/.ramPct/.ramFree, phone.storage/
     * .storagePct/.storageFree.
     */
    fun values(
        level: Int?,
        charging: Boolean,
        samples: List<Sample>,
        nowMs: Long,
        chargeLeftMs: Long?,
        tempTenthsC: Int?,
        fahrenheit: Boolean,
        ramUsed: Long?,
        ramTotal: Long?,
        storageUsed: Long?,
        storageTotal: Long?,
    ): Map<String, String> {
        val out = mutableMapOf<String, String>()
        if (charging) {
            out["battery.left"] = when {
                level == 100 -> "Fully charged"
                chargeLeftMs != null && chargeLeftMs > 0 -> "Full in ${hoursMinutes(chargeLeftMs / 3_600_000.0)}"
                else -> "Charging"
            }
        } else if (level != null) {
            val rate = drainPerHour(samples, nowMs)
            out["battery.left"] = if (rate == null) "Working out battery life…" else "About ${hoursMinutes(level / rate)} left"
            rate?.let { out["battery.drain"] = String.format(Locale.US, "%.1f%%/h", it) }
        }
        tempTenthsC?.let {
            val c = it / 10.0
            out["battery.temp"] = if (fahrenheit) "${(c * 9 / 5 + 32).roundToInt()}°F" else "${c.roundToInt()}°C"
        }
        if (ramUsed != null && ramTotal != null && ramTotal > 0) {
            out["phone.ram"] = "${gb(ramUsed)} of ${gb(ramTotal)}"
            out["phone.ramPct"] = (ramUsed * 100 / ramTotal).toString()
            out["phone.ramFree"] = "${gb(ramTotal - ramUsed)} free"
        }
        if (storageUsed != null && storageTotal != null && storageTotal > 0) {
            out["phone.storage"] = "${gb(storageUsed)} of ${gb(storageTotal)}"
            out["phone.storagePct"] = (storageUsed * 100 / storageTotal).toString()
            out["phone.storageFree"] = "${gb(storageTotal - storageUsed)} free"
        }
        return out
    }
}
