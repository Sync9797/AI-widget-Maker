package com.sidequest.widgetmaker.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.sidequest.widgetmaker.spec.Panel
import kotlin.math.roundToInt

/**
 * The part of a panel that's "full" (a battery's charge): the panel's own
 * shape up to [fraction] of its width, half see-through so the glass, frost or
 * color under it still shows. A silhouette, tinted with the theme's accent when drawn.
 */
object FillArt {
    /** How strong the fill is: enough to read as the level, light enough to keep text on it readable. */
    private const val ALPHA = 0x99

    fun build(context: Context, panel: Panel, fraction: Float): Bitmap? {
        if (fraction <= 0f) return null
        val density = context.resources.displayMetrics.density
        val pxW = (panel.width * density).roundToInt()
        val pxH = (panel.height * density).roundToInt()
        val (outW, outH) = ShapePainter.outSize(pxW, pxH)
        val radius = panel.radius * density * outW.toFloat() / pxW
        val out = ShapePainter.mask(panel.shape, outW, outH, radius)
        val canvas = Canvas(out)
        // Clear what's beyond the level, then make the rest see-through.
        canvas.drawRect(outW * fraction.coerceIn(0f, 1f), 0f, outW.toFloat(), outH.toFloat(), Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })
        canvas.drawColor(ALPHA shl 24, PorterDuff.Mode.DST_IN)
        return out
    }
}
