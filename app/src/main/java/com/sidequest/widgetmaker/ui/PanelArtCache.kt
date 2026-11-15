package com.sidequest.widgetmaker.ui

import android.content.Context
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.Panel
import com.sidequest.widgetmaker.spec.Sky
import com.sidequest.widgetmaker.spec.WeatherMood
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.widget.PanelArt

/**
 * Panel backgrounds for the in-app previews, kept so each is built once: most designs at the same size and
 * shape share one (a blur, a shape, an edge), and the widget list shows dozens. Built off the main thread by
 * the previews, so the list opens at once.
 */
object PanelArtCache {
    data class Key(
        val theme: WidgetTheme,
        val panel: Panel,
        val look: Look,
        val mood: WeatherMood?,
        val sky: Sky?,
        val dark: Boolean,
    )

    private const val MAX = 48
    private val cache = object : LinkedHashMap<Key, PanelArt>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, PanelArt>?) = size > MAX
    }

    @Synchronized
    fun get(key: Key): PanelArt? = cache[key]

    fun getOrBuild(context: Context, key: Key, feeds: Map<String, String>): PanelArt =
        get(key) ?: PanelArt.build(context, key.theme, key.panel, key.look, null, key.mood, feeds).also { put(key, it) }

    @Synchronized
    private fun put(key: Key, art: PanelArt) {
        cache[key] = art
    }

    /** Forgets every background (the wallpaper changed, so the frosted ones are out of date). */
    @Synchronized
    fun clear() = cache.clear()
}
