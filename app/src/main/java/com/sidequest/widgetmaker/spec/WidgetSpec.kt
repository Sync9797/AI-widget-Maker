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
    /** The panel's outline: rounded, pill, circle, squircle, arch or cookie. */
    val shape: WidgetShape = WidgetShape.rounded,
    val root: Node,
)

/** A saved design plus what it was made from. */
@Serializable
data class SavedDesign(
    val id: String,
    val prompt: String,
    val design: WidgetDesign,
    val createdAt: Long,
)

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
) {
    fun fits(width: Int, height: Int) =
        width >= minWidth && height >= minHeight &&
            (maxWidth == 0 || width <= maxWidth) && (maxHeight == 0 || height <= maxHeight)

    fun has(tokens: Tokens?): Boolean {
        val token = ifSet ?: return true
        val value = tokens?.fill(token)?.trim() ?: return true
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

/** Empty space: [size] dp, or all the leftover space when [grow] is set. */
@Serializable
@SerialName("spacer")
data class SpacerNode(
    val size: Int = 8,
    override val grow: Boolean = false,
    override val show: Show? = null,
    override val tap: String? = null,
) : Node
