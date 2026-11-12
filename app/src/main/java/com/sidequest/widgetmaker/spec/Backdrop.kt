package com.sidequest.widgetmaker.spec

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The math behind the One UI theme's blur, free of Android so it can be
 * tested: which part of the wallpaper sits behind a widget, a fast blur,
 * and how bright the result is (to pick dark or light text).
 */
object Backdrop {
    /** A rectangle in pixels. */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left
        val height get() = bottom - top
    }

    /**
     * Where the screen rectangle [onScreen] falls on a wallpaper of
     * [wallW]×[wallH], drawn the way launchers draw it: scaled to cover the
     * [screenW]×[screenH] screen and centered.
     */
    fun wallpaperArea(wallW: Int, wallH: Int, screenW: Int, screenH: Int, onScreen: Box): Box {
        val scale = max(screenW.toFloat() / wallW, screenH.toFloat() / wallH)
        val offsetX = (wallW * scale - screenW) / 2f
        val offsetY = (wallH * scale - screenH) / 2f
        fun x(v: Int) = ((v + offsetX) / scale).roundToInt().coerceIn(0, wallW)
        fun y(v: Int) = ((v + offsetY) / scale).roundToInt().coerceIn(0, wallH)
        val box = Box(x(onScreen.left), y(onScreen.top), x(onScreen.right), y(onScreen.bottom))
        return if (box.width < 2 || box.height < 2) Box(0, 0, wallW, wallH) else box
    }

    /** Where a widget of [w]×[h] probably sits when its position isn't known yet: the middle of the screen. */
    fun guess(screenW: Int, screenH: Int, w: Int, h: Int): Box {
        val left = (screenW - w) / 2
        val top = (screenH - h) / 2
        return Box(left, top, left + w, top + h)
    }

    /** A box blur of [radius], run [passes] times (three passes look like a Gaussian). In place. */
    fun blur(pixels: IntArray, w: Int, h: Int, radius: Int, passes: Int = 3) {
        if (radius < 1 || w < 2 || h < 2) return
        val tmp = IntArray(pixels.size)
        repeat(passes) {
            pass(pixels, tmp, w, h, radius, horizontal = true)
            pass(tmp, pixels, w, h, radius, horizontal = false)
        }
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        for (line in 0 until lines) {
            fun at(i: Int): Int {
                val c = i.coerceIn(0, len - 1)
                return if (horizontal) line * w + c else c * w + line
            }
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val p = src[at(i)]
                a += p ushr 24; rr += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
            }
            val n = 2 * r + 1
            for (i in 0 until len) {
                dst[at(i).let { if (horizontal) line * w + i else i * w + line }] =
                    ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
                val out = src[at(i - r)]
                val inn = src[at(i + r + 1)]
                a += (inn ushr 24) - (out ushr 24)
                rr += ((inn shr 16) and 0xFF) - ((out shr 16) and 0xFF)
                g += ((inn shr 8) and 0xFF) - ((out shr 8) and 0xFF)
                b += (inn and 0xFF) - (out and 0xFF)
            }
        }
    }

    /** Average perceived brightness, 0 (black) to 1 (white). */
    fun luminance(pixels: IntArray): Double {
        if (pixels.isEmpty()) return 0.0
        var sum = 0.0
        val step = max(1, pixels.size / 4096)
        var n = 0
        var i = 0
        while (i < pixels.size) {
            val p = pixels[i]
            sum += 0.2126 * ((p shr 16) and 0xFF) + 0.7152 * ((p shr 8) and 0xFF) + 0.0722 * (p and 0xFF)
            n++
            i += step
        }
        return sum / n / 255.0
    }

    /**
     * Above this, the frosted panel is light and gets dark text (like One UI
     * over a bright wallpaper); below it, white text.
     */
    const val LIGHT_ABOVE = 0.58

    /** Boosts (factor > 1) or mutes color, like the vivid look of One UI's blur. In place. */
    fun saturate(pixels: IntArray, factor: Double) {
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
            val gray = 0.299 * r + 0.587 * g + 0.114 * b
            fun ch(v: Int) = (gray + (v - gray) * factor).toInt().coerceIn(0, 255)
            pixels[i] = (p and 0xFF000000.toInt()) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
        }
    }

    /** Mixes [color] over every pixel at [alpha] (0-255): the frosted tint. */
    fun tint(pixels: IntArray, color: Int, alpha: Int) {
        val a = min(255, max(0, alpha))
        val cr = (color shr 16) and 0xFF; val cg = (color shr 8) and 0xFF; val cb = color and 0xFF
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p shr 16) and 0xFF) * (255 - a) / 255 + cr * a / 255
            val g = ((p shr 8) and 0xFF) * (255 - a) / 255 + cg * a / 255
            val b = (p and 0xFF) * (255 - a) / 255 + cb * a / 255
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}
