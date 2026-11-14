package com.sidequest.widgetmaker.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas

/** Installed apps' own icons, as pictures a widget can show (for shortcut widgets). */
object AppIcons {
    /** [packageName]'s icon, [px] pixels square (at most 192), or null if it isn't installed. */
    fun bitmap(context: Context, packageName: String, px: Int): Bitmap? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val side = px.coerceIn(24, 192)
        Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, side, side)
            drawable.draw(Canvas(it))
        }
    }.getOrNull()
}
