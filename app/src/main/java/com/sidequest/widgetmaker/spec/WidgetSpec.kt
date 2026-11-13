package com.sidequest.widgetmaker.spec

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A widget design: what Claude writes and what the widget renders.
 *
 * Android widgets can't run downloaded code, so a design is a small tree of
 * building blocks (rows, columns, text, a live clock, icons, progress bars)
 * that the app knows how to draw on the home screen and the lock screen.
 */
@Serializable
data class WidgetDesign(
    val name: String = "My widget",
    /** Background color of the whole widget, "#RRGGBB" or "#AARRGGBB". */
    val background: String = "#E61E1E2E",
    val cornerRadius: Int = 24,
    val padding: Int = 14,
    /** The panel's outline: rounded, pill, circle, squircle, arch, cookie, clover, flower, pebble, ticket, stadium or leaf. */
    val shape: WidgetShape = WidgetShape.rounded,
    /** The whole widget fills up to a value, like a battery (see [Fill]). */
    val fill: Fill? = null,
    val root: Node,
)

/**
 * The panel fills from left to right up to [value] (0-100, like "{battery}"),
 * in the theme's accent: the wallpaper color on One UI, the phone's primary
 * color on Material 3, frosted white on Glass, [color] in Original. While the
 * value is at or below [levelsUpTo], [levels] color it instead (orange, then red,
 * for a low battery).
 */
@Serializable
data class Fill(
    val value: String,
    val color: String = "#FFFFFFFF",
    val levels: List<Level>? = null,
    val levelsUpTo: Float? = null,
)

/** A saved design plus what it was made from. */
@Serializable
data class SavedDesign(
    val id: String,
    val prompt: String,
    val design: WidgetDesign,
    val createdAt: Long,
    /** Made with Claude (described, changed or pasted), not a ready-made design saved as is. Null in older saves. */
    val custom: Boolean? = null,
) {
    /** Whether you made it: older saves count as yours unless they're named after a ready-made design. */
    val madeByYou: Boolean get() = custom ?: Starters.all.none { it.title == prompt }
}

enum class HAlign { start, center, end }
enum class VAlign { top, center, bottom }
enum class Weight { light, normal, medium, bold }

/**
 * Icons the widget can draw (each is a drawable in the app). "weather" is
 * live: it shows sun, moon, cloud, rain, snow or storm for the current weather.
 */
enum class IconName { star, heart, bolt, moon, sun, battery, calendar, music, cloud, rain, snow, storm, car, news, ticket, coin, umbrella, pin, flame, egg, paw, steps, check, play, pause, next, previous, weather }

@Serializable
sealed interface Node {
    /** In a row or column: take up the leftover space. */
    val grow: Boolean
    /** Sizes this element appears at; null means every size. */
    val show: Show?
    /** What tapping it does: an app kind ("weather") or a link value ("{news.1.link}"). See [Taps]. */
    val tap: String?
}

/**
 * Makes an element appear only at some widget sizes (in dp, the widget's
 * whole size), so a bigger widget shows more. 0 means no limit.
 */
@Serializable
data class Show(
    val minWidth: Int = 0,
    val minHeight: Int = 0,
    val maxWidth: Int = 0,
    val maxHeight: Int = 0,
    /** Only when this value has data, like "{mq.egg.4}" (so a list shows only the items there are). */
    val ifSet: String? = null,
    /** Only when these values are all empty, like "{calendar.soon}{music.playing}" (for "otherwise" parts). */
    val ifEmpty: String? = null,
) {
    fun fits(width: Int, height: Int) =
        width >= minWidth && height >= minHeight &&
            (maxWidth == 0 || width <= maxWidth) && (maxHeight == 0 || height <= maxHeight)

    fun has(tokens: Tokens?): Boolean {
        tokens ?: return true
        if (ifSet != null && !set(tokens.fill(ifSet))) return false
        if (ifEmpty != null && set(tokens.fill(ifEmpty).replace("--", ""))) return false
        return true
    }

    private fun set(filled: String): Boolean {
        val value = filled.trim()
        return value.isNotEmpty() && value != "--" && !value.contains('{')
    }
}

/** Whether [node] appears in a widget this size (and, with [tokens], whether its data is there). */
fun visible(node: Node, width: Int, height: Int, tokens: Tokens? = null) =
    node.show?.let { it.fits(width, height) && it.has(tokens) } ?: true

@Serializable
@SerialName("column")
data class ColumnNode(
    val children: List<Node> = emptyList(),
    val align: HAlign = HAlign.start,
    val verticalAlign: VAlign = VAlign.top,
    val background: String? = null,
    val cornerRadius: Int = 0,
    val padding: Int = 0,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

@Serializable
@SerialName("row")
data class RowNode(
    val children: List<Node> = emptyList(),
    val align: HAlign = HAlign.start,
    val verticalAlign: VAlign = VAlign.center,
    val background: String? = null,
    val cornerRadius: Int = 0,
    val padding: Int = 0,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/** Text. May contain tokens like {date} or {battery}; see [Tokens]. */
@Serializable
@SerialName("text")
data class TextNode(
    val text: String,
    val size: Int = 14,
    val color: String = "#FFFFFFFF",
    val weight: Weight = Weight.normal,
    val align: HAlign = HAlign.start,
    val maxLines: Int = 2,
    /**
     * Colors the text by a value going up or down, like "{crypto.BTC.change}":
     * [upColor] when it starts with "+", [downColor] with "-", else [color].
     */
    val trendOf: String? = null,
    val upColor: String = "#FF4ADE80",
    val downColor: String = "#FFF87171",
    /** Colors it by a number's level, like "{health.charge}": see [Level]. Wins over [trendOf]. */
    val levelOf: String? = null,
    val levels: List<Level>? = null,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/**
 * A live clock or date that the system keeps up to date every minute.
 * [format] is an Android date pattern, like "h:mm", "HH:mm" or "EEE d MMM".
 */
@Serializable
@SerialName("clock")
data class ClockNode(
    val format: String = "h:mm",
    val size: Int = 40,
    val color: String = "#FFFFFFFF",
    val weight: Weight = Weight.light,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

@Serializable
@SerialName("icon")
data class IconNode(
    val name: IconName = IconName.star,
    /** A value holding an icon name, like "{weather.h1.icon}": the icon follows it. */
    val live: String? = null,
    val size: Int = 20,
    val color: String = "#FFFFFFFF",
    /** Colors it by a number's level, like "{battery}": see [Level]. */
    val levelOf: String? = null,
    val levels: List<Level>? = null,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/** A bar from 0 to 100. [value] is a number or "{battery}". */
@Serializable
@SerialName("progress")
data class ProgressNode(
    val value: String = "{battery}",
    val color: String = "#FF7CF29C",
    val trackColor: String = "#33FFFFFF",
    val height: Int = 6,
    /** Colors it by a number's level, like "{battery}": see [Level]. */
    val levelOf: String? = null,
    val levels: List<Level>? = null,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/** A thin line between rows, [thickness] dp. */
@Serializable
@SerialName("divider")
data class DividerNode(
    val color: String = "#26FFFFFF",
    val thickness: Int = 1,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/** The album art of what's playing (a music note when there's none), [size] dp square. */
@Serializable
@SerialName("art")
data class ArtNode(
    val size: Int = 56,
    val cornerRadius: Int = 12,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/**
 * One step of a color scale: the color for values from [from] up (to the next
 * step). For example Charge: 0 red, 34 yellow, 67 green.
 */
@Serializable
data class Level(val from: Float, val color: String)

/** How full a [Fill] is (0-1), and its color: a level color (role Level) or its accent. */
fun fillLevel(fill: Fill, tokens: Tokens): Pair<Float, Pair<Role, String>> {
    val fraction = tokens.progress(fill.value)
    val upTo = fill.levelsUpTo
    val low = if (upTo == null || fraction * 100 <= upTo) levelColor(fill.value, fill.levels, tokens) else null
    return fraction to (low?.let { Role.Level to it } ?: (Role.Accent to fill.color))
}

/** The color for [value] on [levels]: the highest step it reaches, or null (no number, or no steps). */
fun levelColor(value: String?, levels: List<Level>?, tokens: Tokens): String? {
    if (value == null || levels.isNullOrEmpty()) return null
    val number = Regex("-?[0-9]+(\\.[0-9]+)?").find(tokens.fill(value).replace(",", ""))?.value?.toFloatOrNull() ?: return null
    return levels.sortedBy { it.from }.lastOrNull { number >= it.from }?.color ?: levels.minByOrNull { it.from }?.color
}

/**
 * Time counting live, kept up to date by the system without redrawing:
 * "music" is how far into the song you are (it stops while paused).
 */
@Serializable
@SerialName("timer")
data class TimerNode(
    val of: String = "music",
    val size: Int = 12,
    val color: String = "#B3FFFFFF",
    val weight: Weight = Weight.normal,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/**
 * A line chart of a series of numbers, like today's heart rate
 * ("{health.heart.series}"), with a soft fill under the line.
 */
@Serializable
@SerialName("chart")
data class ChartNode(
    val series: String = "{health.heart.series}",
    val height: Int = 90,
    val color: String = "#FFD2542A",
    /** Fixed bottom and top of the scale (like 0 and 300 for stress); null fits the series. */
    val min: Int? = null,
    val max: Int? = null,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/**
 * A ring filled to [value] (0-100), like NOOP's Charge, Effort and Rest,
 * with [text] in the middle.
 */
@Serializable
@SerialName("ring")
data class RingNode(
    val value: String = "{health.charge}",
    val text: String = "{health.charge}",
    val size: Int = 72,
    val thickness: Int = 8,
    val textSize: Int = 22,
    val color: String = "#FFD9A33B",
    val trackColor: String = "#33FFFFFF",
    val textColor: String = "#FFFFFFFF",
    val levelOf: String? = null,
    val levels: List<Level>? = null,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node {
    /**
     * This ring shrunk to fit [room] (the height there is, in dp before scaling), with its line and
     * text in proportion, so a ring in a slim pill isn't cut off. Never grows it.
     */
    fun fitted(room: Int): RingNode {
        if (room >= size || room <= 0) return this
        val r = maxOf(room, 16).toFloat() / size
        return copy(size = maxOf(room, 16), thickness = maxOf(2, (thickness * r).toInt()), textSize = maxOf(8, (textSize * r).toInt()))
    }
}

/** Last night's sleep as a bar of stages (awake, light, deep, REM), [height] dp tall. */
@Serializable
@SerialName("sleep")
data class SleepNode(
    val height: Int = 14,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node

/** Empty space: [size] dp, or all the leftover space when [grow] is set. */
@Serializable
@SerialName("spacer")
data class SpacerNode(
    val size: Int = 8,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node
