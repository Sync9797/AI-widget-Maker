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
    /** The installed NOOP build that shares its snapshot, and the snapshot; null when none does. */
    fun read(context: Context): NoopMath.Snapshot? {
        for (pkg in Health.NOOP_PACKAGES) {
            val snap = runCatching {
                context.contentResolver.query(Uri.parse("content://$pkg.share/snapshot"), null, null, null, null)?.use { c ->
                    if (!c.moveToFirst()) return@use null
                    fun int(name: String) = c.getColumnIndex(name).takeIf { it >= 0 }?.let { c.getInt(it) } ?: -1
                    NoopMath.Snapshot(
                        charge = int("recovery"),
                        rest = int("rest"),
                        effort = int("effort"),
                        heart = int("hr"),
                        heartStale = int("hr_stale") == 1,
                        battery = int("battery"),
                        connected = int("connected") == 1,
                        updatedAtMs = c.getColumnIndex("updated_at").takeIf { it >= 0 }?.let { c.getLong(it) } ?: 0L,
                        series = c.getColumnIndex("hr_series").takeIf { it >= 0 }?.let { c.getString(it) }.orEmpty(),
                    )
                }
            }.getOrNull()
            if (snap != null) return snap
        }
        return null
    }

    /** The widget values from the fork's snapshot (they replace the Health Connect estimates). */
    fun values(context: Context): Map<String, String> =
        read(context)?.let { NoopMath.values(it, ZoneId.systemDefault()) }.orEmpty()
}

/** The fork says its snapshot changed: redraw the widgets that show health values. */
class NoopChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
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
