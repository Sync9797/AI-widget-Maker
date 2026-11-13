package com.sidequest.widgetmaker.feeds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.widget.AiWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Reads straight from the not-NOOP fork, which shares its widget snapshot with this app alone
 * (content://<its id>.share): NOOP's own Charge, Rest and Effort, live heart rate, strap battery and
 * the last 3 hours of heart rate. No Health Connect in between, and no estimates.
 */
object NoopBridge {
    /**
     * The snapshot of the NOOP build that shares and updated last (two builds can sit side by side,
     * and only the one paired with the strap stays fresh); null when none shares.
     */
    fun read(context: Context): NoopMath.Snapshot? {
        val now = android.os.SystemClock.elapsedRealtime()
        cached?.let { (at, snap) -> if (now - at < CACHE_MS) return snap }
        val pm = context.packageManager
        // Only builds that are installed: asking for an absent one still costs a round trip.
        val snap = Health.NOOP_PACKAGES
            .filter { runCatching { pm.getPackageInfo(it, 0) }.isSuccess }
            .mapNotNull { readFrom(context, it) }
            .maxByOrNull { it.updatedAtMs }
        cached = now to snap
        return snap
    }

    /**
     * A read is reused for this long. Several widgets redrawing together, or a screen of previews,
     * would otherwise each ask not-NOOP, and each ask can start it and read its database.
     */
    private const val CACHE_MS = 15_000L
    @Volatile private var cached: Pair<Long, NoopMath.Snapshot?>? = null

    /** Forgets the last read, so the next one asks not-NOOP again (when it says its data changed). */
    fun invalidate() { cached = null }

    /**
     * What the Check screen says about not-NOOP: each installed NOOP build, and whether it shares.
     * A build that answers nothing predates sharing (or isn't the fork), so it needs the newer build.
     */
    fun diagnose(context: Context): List<String> {
        val pm = context.packageManager
        val installed = Health.NOOP_PACKAGES.filter { pm.getLaunchIntentForPackage(it) != null }
        if (installed.isEmpty()) return listOf("not-NOOP: not installed, so health widgets use Health Connect only")
        val now = System.currentTimeMillis()
        invalidate()
        return installed.map { pkg ->
            val name = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
            val snap = readFrom(context, pkg)
            if (snap == null) "$name ($pkg): installed but not sharing; it needs the not-NOOP build with sharing"
            else "$name ($pkg): sharing: " + NoopMath.describe(snap, now)
        }
    }

    private fun readFrom(context: Context, pkg: String): NoopMath.Snapshot? =
        runCatching {
            context.contentResolver.query(Uri.parse("content://$pkg.share/snapshot"), null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use null
                fun int(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getInt(it) } ?: -1
                fun str(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getString(it) }.orEmpty()
                NoopMath.Snapshot(
                    charge = int("recovery"),
                    rest = int("rest"),
                    effort = int("effort"),
                    heart = int("hr"),
                    heartStale = int("hr_stale") == 1,
                    battery = int("battery"),
                    connected = int("connected") == 1,
                    updatedAtMs = c.getColumnIndex("updated_at").takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L,
                    series = str("hr_series"),
                // Newer not-NOOP builds also share today's stress and the Coach brief.
                stress = str("stress_series"),
                brief = str("coach_brief"),
                briefAtMs = c.getColumnIndex("coach_brief_at").takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L,
                day = str("hr_day"),
                )
            }
        }.getOrNull()

    /** The widget values from the fork's snapshot (they replace the Health Connect estimates). */
    fun values(context: Context): Map<String, String> =
        read(context)?.let { NoopMath.values(it, ZoneId.systemDefault()) }.orEmpty()
}

/** The fork says its snapshot changed: redraw the widgets that show health values. */
class NoopChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        NoopBridge.invalidate()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val store = DesignStore(app)
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(app)
                manager.getGlanceIds(AiWidget::class.java).forEach { id ->
                    val design = store.designFor(manager.getAppWidgetId(id))?.design ?: return@forEach
                    if (DesignParser.encode(design).contains("{health.")) AiWidget.refresh(app, id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
