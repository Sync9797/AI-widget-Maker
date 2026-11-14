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

    /**
     * A line through [values] with a fading fill under it, scaled between their lowest and highest,
     * or between [min] and [max] when given.
     */
    fun line(values: List<Int>, w: Int, h: Int, strokePx: Float, min: Int? = null, max: Int? = null): Bitmap {
        val out = Bitmap.createBitmap(w.coerceAtLeast(8), h.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        if (values.size < 2) return out
        val canvas = Canvas(out)
        val lo = min ?: values.min()
        val hi = maxOf(max ?: values.max(), lo + 1)
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

    /**
     * Fine bars, one per value (spread out to about [bars] when there are fewer, by stepping between them),
     * rising from a baseline: a sound-meter style chart, white, to tint.
     */
    fun ticks(values: List<Int>, w: Int, h: Int, barPx: Float, bars: Int = 32, min: Int? = null, max: Int? = null): Bitmap {
        val out = Bitmap.createBitmap(w.coerceAtLeast(8), h.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        if (values.isEmpty()) return out
        val canvas = Canvas(out)
        val lo = (min ?: values.min()).toFloat()
        val hi = maxOf((max ?: values.max()).toFloat(), lo + 1)
        val n = maxOf(values.size, bars)
        // Each bar takes the value nearest its place in the series, rising from a third of the height at least.
        fun valueAt(i: Int): Float {
            val p = i * (values.size - 1).toFloat() / maxOf(1, n - 1)
            val a = values[p.toInt()]; val b = values[minOf(values.lastIndex, p.toInt() + 1)]
            return a + (b - a) * (p - p.toInt())
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); strokeWidth = barPx; strokeCap = Paint.Cap.ROUND }
        val step = (out.width - barPx) / maxOf(1, n - 1)
        for (i in 0 until n) {
            val x = barPx / 2 + step * i
            val f = 0.3f + 0.7f * (valueAt(i) - lo) / (hi - lo)
            canvas.drawLine(x, out.height - barPx / 2, x, out.height - barPx / 2 - (out.height - barPx) * f, paint)
        }
        return out
    }

    /**
     * A ruler [w]×[h]: tick marks (every fifth longer) along its length, bottom to top when [vertical], white to
     * tint. [marker]: only the tick at [fraction] (0 to 1), longer and bolder, to lay over the ruler in a color.
     */
    fun scale(w: Int, h: Int, ticks: Int, vertical: Boolean, density: Float, marker: Boolean, fraction: Float?): Bitmap {
        val out = Bitmap.createBitmap(w.coerceAtLeast(8), h.coerceAtLeast(8), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val n = ticks.coerceIn(3, 60)
        val length = (if (vertical) out.height else out.width).toFloat()
        val across = (if (vertical) out.width else out.height).toFloat()
        val pad = 2 * density
        fun at(f: Float) = if (vertical) length - pad - (length - 2 * pad) * f else pad + (length - 2 * pad) * f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); strokeCap = Paint.Cap.ROUND }
        fun tick(f: Float, size: Float, width: Float) {
            paint.strokeWidth = width
            val p = at(f)
            // Ticks hang from the far edge (the right of a vertical ruler, the bottom of a flat one).
            if (vertical) canvas.drawLine(across - size, p, across - pad, p, paint)
            else canvas.drawLine(p, across - size, p, across - pad, paint)
        }
        if (marker) {
            fraction?.let { tick(it, across - pad, 3 * density) }
        } else {
            for (i in 0 until n) tick(i / (n - 1f), if (i % 5 == 0) across * 0.55f else across * 0.3f, 1.2f * density)
        }
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
