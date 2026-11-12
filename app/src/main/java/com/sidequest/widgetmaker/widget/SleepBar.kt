package com.sidequest.widgetmaker.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.sidequest.widgetmaker.spec.SleepStages

/** Draws last night's stages as a rounded bar, each stage as wide as it lasted. */
object SleepBar {
    fun draw(value: String?, widthPx: Int = 600, heightPx: Int = 40): Bitmap {
        val out = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val parts = SleepStages.decode(value)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = heightPx / 2f
        val whole = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        if (parts.isEmpty()) {
            paint.color = 0x33FFFFFF
            canvas.drawRoundRect(whole, r, r, paint)
            return out
        }
        // Clip to the rounded bar, then lay the stages side by side.
        val path = android.graphics.Path().apply { addRoundRect(whole, r, r, android.graphics.Path.Direction.CW) }
        canvas.clipPath(path)
        val total = parts.sumOf { it.minutes }.toFloat()
        var x = 0f
        parts.forEach { p ->
            val w = widthPx * p.minutes / total
            paint.color = SleepStages.color(p.stage)
            canvas.drawRect(x, 0f, x + w + 0.5f, heightPx.toFloat(), paint)
            x += w
        }
        return out
    }
}
