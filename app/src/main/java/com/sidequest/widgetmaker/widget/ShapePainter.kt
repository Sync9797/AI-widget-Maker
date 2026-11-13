package com.sidequest.widgetmaker.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import com.sidequest.widgetmaker.spec.WidgetShape
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Draws widget panels in any shape. Widgets can only show rectangles (and,
 * on Android 12+, rounded corners), so a shaped panel is an image: the
 * shape's fill, plus a soft edge on top, with the content drawn over both.
 */
object ShapePainter {
    /** Panels are drawn at most this many pixels across (they're scaled up smoothly). */
    const val MAX_SIDE = 480

    /** The bitmap size for a panel of [w]×[h] pixels. */
    fun outSize(w: Int, h: Int): Pair<Int, Int> {
        val scale = min(1f, MAX_SIDE.toFloat() / max(w, h))
        return max(8, (w * scale).roundToInt()) to max(8, (h * scale).roundToInt())
    }

    /** The outline of [shape] filling [w]×[h] pixels; [radius] for the rounded ones. */
    fun path(shape: WidgetShape, w: Float, h: Float, radius: Float, inset: Float = 0f): Path {
        val r = RectF(inset, inset, w - inset, h - inset)
        val rad = max(0f, radius - inset)
        return Path().apply {
            when (shape) {
                WidgetShape.rounded, WidgetShape.pill -> addRoundRect(r, rad, rad, Path.Direction.CW)
                WidgetShape.stadium -> min(r.width(), r.height()).div(2).let { addRoundRect(r, it, it, Path.Direction.CW) }
                WidgetShape.clover -> polar(this, r, square = true) { t -> 0.8 + 0.2 * cos(4 * t) }
                WidgetShape.flower -> polar(this, r, square = true) { t -> 0.88 + 0.12 * cos(6 * t) }
                WidgetShape.pebble -> polar(this, r, square = false) { t -> 1 + 0.06 * cos(2 * t + 0.6) + 0.04 * cos(3 * t + 1.9) }
                WidgetShape.ticket -> ticket(this, r, rad)
                WidgetShape.circle -> addOval(r, Path.Direction.CW)
                WidgetShape.arch -> {
                    val bottom = min(rad * 0.35f, r.width() / 6)
                    addRoundRect(r, floatArrayOf(r.width() / 2, r.width() / 2, r.width() / 2, r.width() / 2, bottom, bottom, bottom, bottom), Path.Direction.CW)
                }
                WidgetShape.squircle -> squircle(this, r)
                WidgetShape.cookie -> cookie(this, r, lobes = 9)
            }
        }
    }

    /** A superellipse: flat sides blending into soft corners. */
    private fun squircle(path: Path, r: RectF, n: Double = 4.5) {
        val cx = r.centerX(); val cy = r.centerY(); val a = r.width() / 2; val b = r.height() / 2
        val steps = 120
        for (i in 0..steps) {
            val t = 2 * PI * i / steps
            val c = cos(t); val s = sin(t)
            val x = cx + a * sign(c) * abs(c).pow(2 / n)
            val y = cy + b * sign(s) * abs(s).pow(2 / n)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
    }

    /** Material 3 Expressive's "cookie": a circle with soft scalloped bumps. */
    private fun cookie(path: Path, r: RectF, lobes: Int) {
        val cx = r.centerX(); val cy = r.centerY(); val rad = min(r.width(), r.height()) / 2
        val depth = rad * 0.075
        val steps = 240
        for (i in 0..steps) {
            val t = 2 * PI * i / steps
            val rr = rad - depth + depth * cos(lobes * t)
            val x = cx + rr * cos(t); val y = cy + rr * sin(t)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
    }

    /**
     * A closed outline whose distance from the center follows [radius] (at angle t, in radians),
     * scaled so its widest point touches the box: a circle's box when [square], else [r] itself.
     */
    private fun polar(path: Path, r: RectF, square: Boolean, radius: (Double) -> Double) {
        val steps = 360
        val ts = (0..steps).map { 2 * PI * it / steps }
        val maxX = ts.maxOf { abs(radius(it) * cos(it)) }
        val maxY = ts.maxOf { abs(radius(it) * sin(it)) }
        val cx = r.centerX(); val cy = r.centerY()
        val side = min(r.width(), r.height()) / 2
        val a = (if (square) side else r.width() / 2) / maxX
        val b = (if (square) side else r.height() / 2) / maxY
        ts.forEachIndexed { i, t ->
            val rr = radius(t)
            val x = cx + a * rr * cos(t); val y = cy + b * rr * sin(t)
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat()) else path.lineTo(x.toFloat(), y.toFloat())
        }
        path.close()
    }

    /** A rounded card with a half-circle notch cut into the middle of each side. */
    private fun ticket(path: Path, r: RectF, rad: Float) {
        val notch = com.sidequest.widgetmaker.spec.Shapes.ticketNotch(r.width(), r.height())
        val card = Path().apply { addRoundRect(r, rad, rad, Path.Direction.CW) }
        val holes = Path().apply {
            addCircle(r.left, r.centerY(), notch, Path.Direction.CW)
            addCircle(r.right, r.centerY(), notch, Path.Direction.CW)
        }
        card.op(holes, Path.Op.DIFFERENCE)
        path.addPath(card)
    }

    /** A white silhouette of the shape, to be tinted with the panel's color (Material 3 colors stay live this way). */
    fun mask(shape: WidgetShape, w: Int, h: Int, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawPath(path(shape, w.toFloat(), h.toFloat(), radius), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        return out
    }

    /** [image] cut to the shape. */
    fun clip(image: Bitmap, shape: WidgetShape, radius: Float): Bitmap {
        val out = mask(shape, image.width, image.height, radius)
        Canvas(out).drawBitmap(image, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        })
        return out
    }

    /** The Glass theme's panel: a smoky see-through tint with a soft highlight at the top. */
    fun glass(shape: WidgetShape, w: Int, h: Int, radius: Float): Bitmap {
        val fill = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(fill)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0x7A1C2333, 0x590B0F1A, Shader.TileMode.CLAMP)
        })
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h * 0.6f, 0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        return clip(fill, shape, radius)
    }

    /** A soft bright edge along the shape, [thickness] pixels wide; null for none. */
    fun edge(shape: WidgetShape, w: Int, h: Int, radius: Float, thickness: Float, color: Int): Bitmap? {
        if (thickness <= 0.2f) return null
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawPath(
            path(shape, w.toFloat(), h.toFloat(), radius, inset = thickness / 2),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = thickness
                this.color = color
            },
        )
        return out
    }
}
