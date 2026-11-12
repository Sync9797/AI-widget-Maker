package com.sidequest.widgetmaker.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader

/**
 * Charts and rings as white silhouettes (with see-through parts), tinted
 * with the theme's colors when drawn, so Material 3 colors stay live.
 */
object ChartArt {
    /** The numbers in a series value like "72,75,81". */
    fun parse(series: String?): List<Int> = series.orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }

    /** A line through [values] with a fading fill under it, scaled between their lowest and highest. */
    fun line(values: List<Int>, w: Int, h: Int, strokePx: Float): Bitmap {
        val out = Bitmap.createBitmap(w.coerceAtLeast(8), h.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        if (values.size < 2) return out
        val canvas = Canvas(out)
        val lo = values.min()
        val hi = maxOf(values.max(), lo + 1)
        val pad = strokePx
        fun x(i: Int) = pad + (out.width - 2 * pad) * i / (values.size - 1).toFloat()
        fun y(v: Int) = pad + (out.height - 2 * pad) * (1f - (v - lo).toFloat() / (hi - lo))
        val line = Path().apply {
            moveTo(x(0), y(values[0]))
            values.forEachIndexed { i, v -> if (i > 0) lineTo(x(i), y(v)) }
        }
        val fill = Path(line).apply {
            lineTo(x(values.lastIndex), out.height.toFloat())
            lineTo(x(0), out.height.toFloat())
            close()
        }
        canvas.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, out.height.toFloat(), 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        canvas.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        })
        return out
    }

    /** A ring [sizePx] across: the whole track when [fraction] is null, else an arc from the top, clockwise. */
    fun ring(sizePx: Int, thicknessPx: Float, fraction: Float?): Bitmap {
        val out = Bitmap.createBitmap(sizePx.coerceAtLeast(8), sizePx.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        val inset = thicknessPx / 2
        val box = RectF(inset, inset, out.width - inset, out.height - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = thicknessPx
            strokeCap = Paint.Cap.ROUND
        }
        val canvas = Canvas(out)
        if (fraction == null) canvas.drawOval(box, paint.apply { strokeCap = Paint.Cap.BUTT })
        else if (fraction > 0f) canvas.drawArc(box, -90f, 360f * fraction.coerceIn(0f, 1f), false, paint)
        return out
    }
}
