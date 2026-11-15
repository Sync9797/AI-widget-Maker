package com.sidequest.widgetmaker.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/**
 * Pictures of previews already drawn in the widget list, so it shows them at once when you come back to it,
 * instead of drawing every widget again. Keyed by what the picture shows (design, theme, look, dark mode, and for
 * live previews the data and the minute), so a change draws it afresh. Kept to a few dozen in memory.
 *
 * The category menu's examples are also kept on the phone ([load], [save]), so the menu opens with pictures
 * straight away the next time the app starts, without drawing a widget at all.
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

    /** The picture kept on the phone for [key] (also kept in memory), or null. Reads a file: call it off the main thread. */
    fun load(context: Context, key: String): ImageBitmap? {
        get(key)?.let { return it }
        val file = file(context, key)
        if (!file.exists()) return null
        val picture = runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull() ?: return null
        put(key, picture)
        return picture
    }

    /** Keeps [picture] for [key] in memory and on the phone. Writes a file: call it off the main thread. */
    fun save(context: Context, key: String, picture: ImageBitmap) {
        put(key, picture)
        runCatching {
            val file = file(context, key)
            file.parentFile?.mkdirs()
            file.outputStream().use { picture.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    /** A stable file name for [key] (FNV-1a, the same every run). */
    private fun file(context: Context, key: String): File {
        var h = 0xcbf29ce484222325uL
        for (c in key) h = (h xor c.code.toULong()) * 0x100000001b3uL
        return File(context.cacheDir, "previews/${h.toString(16)}.png")
    }
}
