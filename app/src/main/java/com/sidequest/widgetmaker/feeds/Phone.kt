package com.sidequest.widgetmaker.feeds

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs

/** Battery life, temperature, memory and storage, read when a widget draws. */
object Phone {
    private const val PREFS = "phone"

    fun values(context: Context): Map<String, String> = runCatching {
        val battery = context.getSystemService(BatteryManager::class.java)
        val level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = (sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
        val now = System.currentTimeMillis()
        val samples = remember(context, level, plugged, now)
        val memory = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().path)
        PhoneMath.values(
            level = level,
            charging = plugged,
            samples = samples,
            nowMs = now,
            chargeLeftMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) battery?.computeChargeTimeRemaining()?.takeIf { it > 0 } else null,
            tempTenthsC = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE },
            fahrenheit = Feeds(context).settings.fahrenheit,
            ramUsed = memory.totalMem.takeIf { it > 0 }?.let { it - memory.availMem },
            ramTotal = memory.totalMem.takeIf { it > 0 },
            storageUsed = stat.totalBytes - stat.availableBytes,
            storageTotal = stat.totalBytes,
        )
    }.getOrDefault(emptyMap())

    /**
     * Keeps a short history of battery readings while unplugged (every time
     * widgets draw, at most every 5 minutes), cleared when plugged in, for the
     * battery-life estimate.
     */
    private fun remember(context: Context, level: Int?, plugged: Boolean, now: Long): List<PhoneMath.Sample> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (plugged || level == null) {
            prefs.edit().remove("samples").apply()
            return emptyList()
        }
        var samples = prefs.getString("samples", "").orEmpty().split(';').mapNotNull { s ->
            val (t, l) = s.split(',').takeIf { it.size == 2 } ?: return@mapNotNull null
            PhoneMath.Sample(t.toLongOrNull() ?: return@mapNotNull null, l.toIntOrNull() ?: return@mapNotNull null)
        }.filter { now - it.timeMs <= 6 * 3_600_000L }
        // A higher level than before means it was charged in between: start again.
        if (samples.isNotEmpty() && level > samples.last().level) samples = emptyList()
        if (samples.isEmpty() || now - samples.last().timeMs >= 5 * 60_000L) samples = samples + PhoneMath.Sample(now, level)
        prefs.edit().putString("samples", samples.takeLast(80).joinToString(";") { "${it.timeMs},${it.level}" }).apply()
        return samples
    }
}
