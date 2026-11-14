package com.sidequest.widgetmaker.feeds

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Web map tile arithmetic (the slippy-map scheme TomTom's tiles use). Free of Android, so it's tested on its own. */
object MapMath {
    const val TILE = 256

    /** The world pixel of [lat],[lon] at [zoom]: x and y, tiles of [TILE] pixels from the top left. */
    fun pixel(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
        val n = (1 shl zoom).toDouble()
        val x = (lon + 180.0) / 360.0 * n
        val rad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
        val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * n
        return x * TILE to y * TILE
    }

    /** A tile to fetch ([x],[y]) and where it goes in the picture ([left],[top], in pixels). */
    data class Placed(val x: Int, val y: Int, val left: Int, val top: Int)

    /** The tiles covering a [width]×[height] picture centered on world pixel [cx],[cy] at [zoom]. */
    fun tiles(cx: Double, cy: Double, width: Int, height: Int, zoom: Int): List<Placed> {
        val n = 1 shl zoom
        val left = cx - width / 2.0
        val top = cy - height / 2.0
        val firstX = floor(left / TILE).toInt(); val lastX = floor((left + width - 1) / TILE).toInt()
        val firstY = floor(top / TILE).toInt(); val lastY = floor((top + height - 1) / TILE).toInt()
        return (firstY..lastY).flatMap { ty ->
            (firstX..lastX).mapNotNull { tx ->
                if (ty !in 0 until n) return@mapNotNull null
                Placed(((tx % n) + n) % n, ty, (tx * TILE - left).toInt(), (ty * TILE - top).toInt())
            }
        }
    }
}
