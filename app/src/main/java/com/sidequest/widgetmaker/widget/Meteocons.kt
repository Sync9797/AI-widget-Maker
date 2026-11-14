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
 * can't play SVG or Lottie, so an animated one is a ViewFlipper stepping through the frames: a layout
 * of its own per icon (res/layout/mc_anim_<name>.xml), so the widget sends the launcher one layout id
 * rather than a view per frame. (Sending the frames as views made the update too big for Android to
 * deliver once a widget had a few sizes, and the app crashed.)
 */
object Meteocons {
    /** Milliseconds per frame (the frames were rendered at 10 a second). */
    const val FRAME_MS = 100

    private val cache = ConcurrentHashMap<String, IntArray>()

    /**
     * "{weather.icon}", "{weather.h2.icon}"... Both braces escaped: Android's regex engine (ICU) rejects
     * a bare "}", though the JVM's, which the unit tests run on, accepts it.
     */
    private val WEATHER_ICON = Regex("""\{weather(\.[hd]\d)?\.icon\}""")

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
            live != null && WEATHER_ICON.matches(live.trim()) -> live.trim().removeSurrounding("{", "}").removeSuffix(".icon") + ".mc"
            live == null && node.name == IconName.weather -> "weather.mc"
            else -> return null
        }
        return tokens.feeds[key]?.takeIf { it.isNotBlank() }
    }

    /** Whether this element shows the current weather (the one that animates, to spare the battery). */
    fun isNow(node: IconNode) = node.live?.trim()?.let { it == "{weather.icon}" } ?: (node.name == IconName.weather)

    /** Now and then: plays this long ([CALM_STILLS] rests of the same length follow; see res/layout/mc_calm_*.xml). */
    const val CALM_PLAY_MS = 6000

    /** How many [CALM_PLAY_MS] spells it rests between plays. */
    const val CALM_STILLS = 4

    /**
     * The Meteocon [name] stepping through its frames by itself, as a widget can animate; null if it has no
     * animation. [calm]: only now and then, playing for 6 seconds then resting for 24 (and it starts playing
     * each time the widget redraws). A widget can't tell when the home screen comes into view, but Android
     * already stops the animation while the screen is off or the widget is out of sight.
     */
    fun flipper(context: Context, name: String, calm: Boolean = false): RemoteViews? {
        val kind = if (calm) "mc_calm_" else "mc_anim_"
        val layout = context.resources.getIdentifier(kind + name, "layout", context.packageName)
        if (layout == 0) return null
        return RemoteViews(context.packageName, layout).apply {
            // Start on the animation (the last child), so it plays first and then rests.
            if (calm) setDisplayedChild(R.id.mc_calm, CALM_STILLS)
        }
    }
}
