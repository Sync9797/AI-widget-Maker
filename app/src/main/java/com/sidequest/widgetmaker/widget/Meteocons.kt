package com.sidequest.widgetmaker.widget

import android.content.Context
import android.widget.RemoteViews
import com.sidequest.widgetmaker.R
import com.sidequest.widgetmaker.spec.IconName
import com.sidequest.widgetmaker.spec.IconNode
import com.sidequest.widgetmaker.spec.Tokens
import java.util.concurrent.ConcurrentHashMap

/**
 * Meteocons (by Bas Milius, MIT licensed; see res/raw/meteocons_license.txt), pre-rendered from their
 * animated SVGs as loops of frames at 10 a second (res/drawable-nodpi/mc_<name>_<nn>.webp). Widgets
 * can't play SVG or Lottie, so an animated one is a ViewFlipper stepping through the frames.
 */
object Meteocons {
    /** Milliseconds per frame (the frames were rendered at 10 a second). */
    const val FRAME_MS = 100

    private val cache = ConcurrentHashMap<String, IntArray>()

    /** The frames of the Meteocon [name] (like "clear_day"), first to last; empty if there's none. */
    fun frames(context: Context, name: String): IntArray = cache.getOrPut(name) {
        val res = context.resources
        generateSequence(0) { it + 1 }
            .map { res.getIdentifier("mc_%s_%02d".format(name, it), "drawable", context.packageName) }
            .takeWhile { it != 0 }
            .toList().toIntArray()
    }

    /**
     * The Meteocon an icon element shows, or null when it isn't a weather icon (or the weather isn't
     * known yet). "{weather.h2.icon}" shows weather.h2.mc; the weather icon itself shows weather.mc.
     */
    fun nameFor(node: IconNode, tokens: Tokens): String? {
        val live = node.live
        val key = when {
            live != null && Regex("""\{weather(\.[hd]\d)?\.icon}""").matches(live.trim()) -> live.trim().removeSurrounding("{", "}").removeSuffix(".icon") + ".mc"
            live == null && node.name == IconName.weather -> "weather.mc"
            else -> return null
        }
        return tokens.feeds[key]?.takeIf { it.isNotBlank() }
    }

    /** Whether this element shows the current weather (the one that animates, to spare the battery). */
    fun isNow(node: IconNode) = node.live?.trim()?.let { it == "{weather.icon}" } ?: (node.name == IconName.weather)

    /** A ViewFlipper stepping through [frames] by itself, as a widget can animate. */
    fun flipper(context: Context, frames: IntArray): RemoteViews =
        RemoteViews(context.packageName, R.layout.mc_flipper).apply {
            removeAllViews(R.id.mc_flipper)
            frames.forEach { frame ->
                addView(R.id.mc_flipper, RemoteViews(context.packageName, R.layout.mc_frame).apply { setImageViewResource(R.id.mc_frame, frame) })
            }
        }
}
