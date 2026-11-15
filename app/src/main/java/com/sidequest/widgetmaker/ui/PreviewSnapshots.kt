package com.sidequest.widgetmaker.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Pictures of previews already drawn in the widget list, so it shows them at once when you come back to it,
 * instead of drawing every widget again. Keyed by what the picture shows (design, theme, look, dark mode, the
 * data and the minute), so a change draws it afresh. Kept to a few dozen.
 */
object PreviewSnapshots {
    private const val MAX = 32
    private val pictures = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > MAX
    }

    @Synchronized
    fun get(key: String): ImageBitmap? = pictures[key]

    @Synchronized
    fun put(key: String, picture: ImageBitmap) {
        pictures[key] = picture
    }
}
