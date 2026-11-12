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
     * Square widgets stay square: a 2×2 (or 4×4) cell is often taller than it
     * is wide in portrait, so its panel becomes a square of the shorter side,
     * centered, like One UI's own 2×2 widgets.
     */
    val squareCells: Boolean = true,
)

/** Where a widget's panel goes inside its cell, and how much room its content gets. */
data class Panel(val width: Int, val height: Int, val shape: WidgetShape, val radius: Int, val padH: Int, val padV: Int, val padTop: Int)

object Shapes {
    /** Widgets up to this tall count as one row. */
    const val ONE_ROW_MAX = 110

    /** The tallest a pill gets. */
    const val PILL_MAX = 96

    private val SQUARE_SHAPES = setOf(WidgetShape.rounded, WidgetShape.squircle, WidgetShape.arch)

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
        if (maxH != null && height <= ONE_ROW_MAX) {
            h = min(h, maxH)
            shape = WidgetShape.pill
        }
        // A squarish cell (2×2, 4×4) gets a square panel; 4×2 and 2×4 keep their shape.
        if (look.squareCells && shape in SQUARE_SHAPES && height > ONE_ROW_MAX && w.toFloat() / h in 0.6f..1.67f) {
            w = min(w, h)
            h = w
        }
        // A pill stays pill-height on taller widgets, centered (like One UI's oval widgets).
        if (shape == WidgetShape.pill) h = min(h, max(56, min(PILL_MAX, w / 2)))
        // A circle or cookie is as big as the shorter side allows.
        if (shape == WidgetShape.circle || shape == WidgetShape.cookie) {
            w = min(w, h)
            h = w
        }
        val radius = when (shape) {
            WidgetShape.rounded -> look.cornerRadius ?: design.cornerRadius
            WidgetShape.squircle -> (look.cornerRadius ?: design.cornerRadius) * 3 / 2
            WidgetShape.pill -> min(w, h) / 2
            WidgetShape.arch -> w / 2
            WidgetShape.circle, WidgetShape.cookie -> w / 2
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
        }
    }
}
