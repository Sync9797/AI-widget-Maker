package com.sidequest.widgetmaker.spec

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The outline of a widget's panel. */
enum class WidgetShape(val label: String) {
    rounded("Rounded"),
    /** Fully round ends, like a capsule. Made for 1-row widgets. */
    pill("Pill"),
    /** A circle (in the middle of a square-ish widget). */
    circle("Circle"),
    /** Softer, iOS/One UI-style rounded square. */
    squircle("Squircle"),
    /** Round top, square-ish bottom, like Material 3 Expressive photo widgets. */
    arch("Arch"),
    /** Material 3 Expressive's scalloped circle. */
    cookie("Cookie"),
    /** Material 3 Expressive's four-leaf clover: one bold value or icon. */
    clover("Clover"),
    /** A softer, rounder scallop than the cookie, with six petals. */
    flower("Flower"),
    /** A smooth, slightly lopsided river stone that fills its cell. */
    pebble("Pebble"),
    /** A rounded card with a half-circle notch in each side, like a ticket stub. */
    ticket("Ticket"),
    /** A capsule along the cell's longer side: tall in a 1×2 or 1×3, made for a single column. */
    stadium("Stadium"),
}

/** How all widgets look, set in Settings → Widget look. */
@Serializable
data class Look(
    /** Corner radius for rounded shapes, in dp; null keeps each design's own. */
    val cornerRadius: Int? = null,
    /** The bright edge around a panel, in dp (0 for none). */
    val edge: Float = 1f,
    /** Space between the widget's cell and its panel, in dp: bigger makes the panel smaller. */
    val inset: Int = 0,
    /**
     * For 1-row widgets: turn them into ovals no taller than this many dp,
     * centered in their cell (like One UI's "Adjust height for 2x1 & 4x1"). Null is off.
     */
    val pillMaxHeight: Int? = null,
    /**
     * One UI proportions: a 2×2 (or 4×4) cell is often much taller than it is
     * wide in portrait, so its panel keeps the shape of One UI's own 2×2
     * widgets (a touch taller than wide), centered, instead of stretching.
     */
    val squareCells: Boolean = true,
    /** Bigger widgets (like on a foldable's inner screen) can grow their text and icons too. */
    val autoScale: AutoScale = AutoScale.more,
)

/** How a widget grows when it's bigger than a standard 2×2 (an open foldable, a tablet, a 4×4). */
enum class AutoScale(val label: String, val about: String) {
    off("Off", "Text and icons stay the same size; bigger widgets show more."),
    scale("Scale with size", "Everything grows with the widget, like a bigger copy of the 2×2."),
    more("Scale and show more", "Grows a little and shows more, so big widgets aren't sparse."),
}

/** Where a widget's panel goes inside its cell, and how much room its content gets. */
data class Panel(val width: Int, val height: Int, val shape: WidgetShape, val radius: Int, val padH: Int, val padV: Int, val padTop: Int)

object Shapes {
    /** Widgets up to this tall count as one row (Samsung reports 2×1 cells up to about 125 dp tall). */
    const val ONE_ROW_MAX = 130

    /** The tallest a pill gets (unless set in Settings), and the shortest. */
    const val PILL_MAX = 72
    const val PILL_MIN = 40

    /** The shorter side of a standard 2×2 widget, in dp: bigger panels scale up from it. */
    const val BASE_SIDE = 165f

    /**
     * How much a panel's content grows: its shorter side against a standard
     * 2×2, up to 1.25× when it also shows more, up to 2× when it only scales.
     * One-row widgets and pills never scale.
     */
    fun scale(panel: Panel, look: Look): Float {
        if (panel.shape == WidgetShape.pill || panel.height <= ONE_ROW_MAX) return 1f
        val raw = minOf(panel.width, panel.height) / BASE_SIDE
        return when (look.autoScale) {
            AutoScale.off -> 1f
            AutoScale.scale -> raw.coerceIn(1f, 2f)
            AutoScale.more -> raw.coerceIn(1f, 1.25f)
        }
    }

    /** A pill's height for its width, like One UI's oval widgets. */
    const val PILL_RATIO = 0.32

    /** Height to width of One UI's own 2×2 widgets (about 380 by 360). */
    const val ONE_UI_RATIO = 1.055f

    private val SQUARE_SHAPES = setOf(WidgetShape.rounded, WidgetShape.squircle, WidgetShape.arch, WidgetShape.ticket)

    /** Shapes drawn as big as the shorter side allows, centered in their cell. */
    val ROUND_SHAPES = setOf(WidgetShape.circle, WidgetShape.cookie, WidgetShape.clover, WidgetShape.flower)

    /** The notch radius of a [WidgetShape.ticket] [w]×[h] across (the same unit in, same unit out). */
    fun ticketNotch(w: Float, h: Float): Float = min(h * 0.12f, w * 0.07f)

    /**
     * The panel for a widget of [width]×[height] dp: its size after the inset
     * and the 1-row oval setting, its final shape and corner radius, and the
     * padding its content needs to stay inside the shape.
     */
    fun panel(width: Int, height: Int, design: WidgetDesign, look: Look, shapeOverride: WidgetShape? = null): Panel {
        var w = max(24, width - 2 * look.inset)
        var h = max(24, height - 2 * look.inset)
        var shape = shapeOverride ?: design.shape
        val maxH = look.pillMaxHeight
        if (maxH != null && height <= ONE_ROW_MAX) shape = WidgetShape.pill
        // A squarish cell that's taller than it is wide (a 2×2 in portrait) gets One UI's proportions,
        // a touch taller than wide, like Samsung's own 2×2 widgets, not the whole tall cell.
        // Wider cells (an open foldable's 2×2) are filled, as One UI fills its own; 2×4 keeps its shape.
        if (look.squareCells && shape in SQUARE_SHAPES && height > ONE_ROW_MAX && w.toFloat() / h in 0.6f..1f) {
            h = min(h, (w * ONE_UI_RATIO).roundToInt())
        }
        // A pill is a slim oval, centered in its cell like One UI's: about a third as tall as it is wide,
        // or the height set in Settings → Widget look (which applies to every pill, whatever its cell).
        if (shape == WidgetShape.pill) h = min(h, maxH ?: (w * PILL_RATIO).roundToInt().coerceIn(PILL_MIN, PILL_MAX))
        // A circle or cookie is as big as the shorter side allows.
        if (shape in ROUND_SHAPES) {
            w = min(w, h)
            h = w
        }
        val radius = when (shape) {
            WidgetShape.rounded -> look.cornerRadius ?: design.cornerRadius
            WidgetShape.squircle -> (look.cornerRadius ?: design.cornerRadius) * 3 / 2
            WidgetShape.pill -> min(w, h) / 2
            WidgetShape.arch -> w / 2
            WidgetShape.circle, WidgetShape.cookie, WidgetShape.clover, WidgetShape.flower -> w / 2
            WidgetShape.pebble, WidgetShape.stadium -> min(w, h) / 2
            WidgetShape.ticket -> look.cornerRadius ?: design.cornerRadius
        }.coerceIn(0, min(w, h) / 2)
        val p = design.padding
        return when (shape) {
            WidgetShape.rounded, WidgetShape.squircle -> Panel(w, h, shape, radius, p, p, p)
            // Keep content off the round ends (on a squarish widget a pill is nearly a circle).
            WidgetShape.pill -> if (w >= h * 1.6) {
                Panel(w, h, shape, radius, max(p, (h * 0.42).roundToInt()), min(p, max(4, h / 6)), min(p, max(4, h / 6)))
            } else {
                (min(w, h) * 0.13).roundToInt().let { Panel(w, h, shape, radius, max(p / 2, it), max(p / 2, it), max(p / 2, it)) }
            }
            // The round top needs more room above the content.
            WidgetShape.arch -> Panel(w, h, shape, radius, max(p, w / 10), p, max(p, (w * 0.22).roundToInt()))
            // Content sits in the square inside the circle.
            WidgetShape.circle -> (w * 0.13).roundToInt().let { Panel(w, h, shape, radius, max(p / 2, it), max(p / 2, it), max(p / 2, it)) }
            WidgetShape.cookie -> (w * 0.13).roundToInt().let { Panel(w, h, shape, radius, max(p / 2, it), max(p / 2, it), max(p / 2, it)) }
            // The clover's leaves dip deeper between them, so content keeps further in.
            WidgetShape.clover -> (w * 0.17).roundToInt().let { Panel(w, h, shape, radius, max(p / 2, it), max(p / 2, it), max(p / 2, it)) }
            WidgetShape.flower -> (w * 0.15).roundToInt().let { Panel(w, h, shape, radius, max(p / 2, it), max(p / 2, it), max(p / 2, it)) }
            WidgetShape.pebble -> Panel(w, h, shape, radius, max(p / 2, (w * 0.13).roundToInt()), max(p / 2, (h * 0.13).roundToInt()), max(p / 2, (h * 0.13).roundToInt()))
            // Content clears the notches in the sides.
            WidgetShape.ticket -> Panel(w, h, shape, radius, max(p, ticketNotch(w.toFloat(), h.toFloat()).roundToInt() + 6), p, p)
            // A tall capsule keeps content off its round ends; a wide one is a pill.
            WidgetShape.stadium -> when {
                h >= w * 1.6 -> Panel(w, h, shape, radius, min(p, max(4, w / 6)), max(p, (w * 0.42).roundToInt()), max(p, (w * 0.42).roundToInt()))
                w >= h * 1.6 -> Panel(w, h, shape, radius, max(p, (h * 0.42).roundToInt()), min(p, max(4, h / 6)), min(p, max(4, h / 6)))
                else -> (min(w, h) * 0.13).roundToInt().let { Panel(w, h, shape, radius, max(p / 2, it), max(p / 2, it), max(p / 2, it)) }
            }
        }
    }
}
