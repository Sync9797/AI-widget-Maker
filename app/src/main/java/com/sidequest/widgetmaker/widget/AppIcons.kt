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

    /**
     * [packageName]'s icon as a white silhouette, [px] square, to tint like text (white by day and dark by night,
     * or whatever suits the panel): the icon's own monochrome layer (Android 13's themed icons), else its
     * foreground layer's outline, or null if it isn't installed or has neither.
     */
    fun mono(context: Context, packageName: String, px: Int): Bitmap? = runCatching {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O || drawable !is android.graphics.drawable.AdaptiveIconDrawable) return null
        val layer = (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) drawable.monochrome else null)
            ?: drawable.foreground ?: return null
        val side = px.coerceIn(24, 192)
        Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888).also {
            // An adaptive layer is drawn half again as big as what shows: only its middle two thirds are the icon.
            val pad = side / 4
            layer.setBounds(-pad, -pad, side + pad, side + pad)
            val canvas = Canvas(it)
            layer.draw(canvas)
            canvas.drawColor(0xFFFFFFFF.toInt(), android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }.getOrNull()
}
