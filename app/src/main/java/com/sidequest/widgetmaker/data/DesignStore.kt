package com.sidequest.widgetmaker.data

import android.content.Context
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.SavedDesign
import com.sidequest.widgetmaker.spec.Backdrop
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.TapKind
import com.sidequest.widgetmaker.spec.WidgetShape
import com.sidequest.widgetmaker.spec.WidgetTheme
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Saved designs (a JSON file), which design each placed widget shows, and the
 * API key. Everything stays on the phone.
 */
class DesignStore(context: Context) {
    private val file = File(context.filesDir, "designs.json")
    private val prefs = context.getSharedPreferences("widget_maker", Context.MODE_PRIVATE)
    private val serializer = ListSerializer(SavedDesign.serializer())

    /** Newest first. */
    fun all(): List<SavedDesign> = synchronized(LOCK) {
        if (!file.exists()) return emptyList()
        runCatching { DesignParser.json.decodeFromString(serializer, file.readText()) }
            .getOrDefault(emptyList())
            .sortedByDescending { it.createdAt }
    }

    fun get(id: String): SavedDesign? = all().firstOrNull { it.id == id }

    fun save(design: SavedDesign) = synchronized(LOCK) {
        val list = all().filterNot { it.id == design.id } + design
        writeAtomically(DesignParser.json.encodeToString(serializer, list))
    }

    fun delete(id: String) = synchronized(LOCK) {
        writeAtomically(DesignParser.json.encodeToString(serializer, all().filterNot { it.id == id }))
    }

    /** The design a placed widget shows: the one picked for it, else the newest. */
    fun designFor(appWidgetId: Int): SavedDesign? {
        val picked = prefs.getString(widgetKey(appWidgetId), null)?.let { get(it) }
        return picked ?: all().firstOrNull()
    }

    fun assign(appWidgetId: Int, designId: String) {
        prefs.edit().putString(widgetKey(appWidgetId), designId).apply()
    }

    /**
     * Which designs are on the home screen, and on how many widgets, given the
     * widgets that are there now ([liveIds]). Settings kept for widgets that
     * have since been removed are cleared on the way (in case the launcher
     * didn't say it removed them).
     */
    fun placed(liveIds: IntArray): Map<String, Int> {
        val live = liveIds.toSet()
        prefs.all.keys.mapNotNull { key -> WIDGET_KEY.matchEntire(key)?.groupValues?.get(1)?.toIntOrNull() }
            .filter { it !in live }.distinct().forEach(::forget)
        return live.mapNotNull { designFor(it)?.id }.groupingBy { it }.eachCount()
    }

    fun forget(appWidgetId: Int) {
        prefs.edit().remove(widgetKey(appWidgetId)).remove("widget_theme_$appWidgetId")
            .remove("widget_shape_$appWidgetId").remove("widget_bounds_$appWidgetId").apply()
    }

    /** How every widget is colored (Settings). */
    var theme: WidgetTheme
        get() = prefs.getString("widget_theme", null)
            ?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() } ?: WidgetTheme.glass
        set(value) = prefs.edit().putString("widget_theme", value.name).apply()

    /** A placed widget's own theme, if one was picked in its settings; otherwise the app's. */
    fun themeFor(appWidgetId: Int): WidgetTheme =
        prefs.getString("widget_theme_$appWidgetId", null)
            ?.let { runCatching { WidgetTheme.valueOf(it) }.getOrNull() } ?: theme

    /** Whether a widget has its own theme (rather than following the app's). */
    fun hasOwnTheme(appWidgetId: Int) = prefs.contains("widget_theme_$appWidgetId")

    /** Sets a widget's own theme; null makes it follow the app's again. */
    fun setThemeFor(appWidgetId: Int, theme: WidgetTheme?) {
        prefs.edit().apply {
            if (theme == null) remove("widget_theme_$appWidgetId") else putString("widget_theme_$appWidgetId", theme.name)
        }.apply()
    }

    /** The app picked for a tap kind (a package name, or "web"); null means ask (or use the system default). */
    fun tapApp(kind: TapKind): String? = prefs.getString("tap_app_${kind.name}", null)

    fun setTapApp(kind: TapKind, choice: String?) {
        prefs.edit().apply {
            if (choice == null) remove("tap_app_${kind.name}") else putString("tap_app_${kind.name}", choice)
        }.apply()
    }

    /** How all widgets look (Settings → Widget look). */
    var look: Look
        get() = prefs.getString("look", null)
            ?.let { runCatching { DesignParser.json.decodeFromString(Look.serializer(), it) }.getOrNull() } ?: Look()
        set(value) = prefs.edit().putString("look", DesignParser.json.encodeToString(Look.serializer(), value)).apply()

    /** A widget's own shape (its settings), or null for its design's. */
    fun shapeFor(appWidgetId: Int): WidgetShape? =
        prefs.getString("widget_shape_$appWidgetId", null)?.let { runCatching { WidgetShape.valueOf(it) }.getOrNull() }

    fun setShapeFor(appWidgetId: Int, shape: WidgetShape?) {
        prefs.edit().apply {
            if (shape == null) remove("widget_shape_$appWidgetId") else putString("widget_shape_$appWidgetId", shape.name)
        }.apply()
    }

    /** Where a widget's panel is on the home screen, in pixels (learned when it's tapped), for the One UI blur. */
    fun boundsFor(appWidgetId: Int): Backdrop.Box? =
        prefs.getString("widget_bounds_$appWidgetId", null)?.split(',')?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 4 }?.let { Backdrop.Box(it[0], it[1], it[2], it[3]) }

    /** Saves a widget's place; true if it changed. */
    fun setBoundsFor(appWidgetId: Int, box: Backdrop.Box): Boolean {
        if (boundsFor(appWidgetId) == box) return false
        prefs.edit().putString("widget_bounds_$appWidgetId", "${box.left},${box.top},${box.right},${box.bottom}").apply()
        return true
    }

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(value) = prefs.edit().putString("api_key", value.trim()).apply()

    private fun writeAtomically(text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }

    private fun widgetKey(appWidgetId: Int) = "widget_$appWidgetId"

    private companion object {
        val LOCK = Any()
        val WIDGET_KEY = Regex("widget_(?:theme_|shape_|bounds_)?(-?\\d+)")
    }
}
