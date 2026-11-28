package com.sidequest.widgetmaker.spec

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Makes a design fit inside its panel before it's drawn, so nothing runs off the edge or out of the shape:
 *
 * - When the content is taller than the room inside the panel, everything shrinks together (down to
 *   [MIN_SHRINK] of its size), rather than the bottom being cut off.
 * - Text too wide for its line gets smaller (down to [MIN_TEXT] of its size); in a row too wide for its room,
 *   the widest texts shrink first, then take the room that's left and end in "…", rather than pushing the
 *   rest off the side.
 *
 * Sizes are estimated (a widget can't measure its text before the launcher lays it out), from each
 * character's typical width, erring on the wide side. Pure, so the widget and the preview fit the same way.
 */
object Fitter {
    /** The smallest a design shrinks to, as a part of its size. */
    const val MIN_SHRINK = 0.62f

    /** The smallest a single text gets to fit its line, as a part of its size, before it's cut short with "…". */
    const val MIN_TEXT = 0.5f

    /** The smallest a text sharing a row's room gets (a title beside a time) before it ends in "…". */
    const val MIN_SHARED = 0.85f

    data class Result(val root: Node, val k: Float)

    /**
     * [root] fitted into [width]×[height] dp (the room inside the panel's padding) at scale [k]. [shows] says
     * which elements appear at this size (size rules and "ifSet").
     */
    /**
     * How wide text is, in dp, at a text size in sp. On the phone it's measured with the font the home screen
     * draws with, at the phone's font size setting; [Estimate] guesses from typical character widths (tests).
     */
    /** Rows of a scrolling list the widget is sized to show at once (the rest scroll into view). */
    const val SCROLL_ROWS = 3

    fun interface TextMeasure {
        fun width(text: String, sizeSp: Float, weight: Weight): Float
    }

    /** Typical character widths, erring wide. */
    val Estimate = TextMeasure { text, size, weight -> textWidth(text, size, weight) }

    fun fit(
        root: Node, width: Float, height: Float, k: Float, tokens: Tokens,
        measure: TextMeasure = Estimate, fontScale: Float = 1f, shows: (Node) -> Boolean,
    ): Result {
        val m = Measure(tokens, shows, measure, fontScale)
        var scale = k
        repeat(3) {
            val natural = m.height(root, width, scale)
            if (natural <= height + 0.5f) return Result(m.spaced(m.rows(root, width, scale), width, height, scale), scale)
            scale = max(k * MIN_SHRINK, scale * (height / natural))
        }
        return Result(m.spaced(m.rows(root, width, scale), width, height, scale), scale)
    }

    /** How wide [text] is at [size] sp, in dp, roughly (from typical character widths). */
    fun textWidth(text: String, size: Float, weight: Weight = Weight.normal): Float {
        var em = 0f
        for (c in text) em += when {
            c.isDigit() -> 0.58f
            c == ' ' -> 0.28f
            c.isUpperCase() -> 0.68f
            c.isLowerCase() -> 0.53f
            c in ".,:;'|!il" -> 0.28f
            c in "%@MW" -> 0.85f
            c.code > 0x2000 -> 1.0f // Symbols and emoji.
            else -> 0.6f
        }
        return em * size * (if (weight == Weight.bold) 1.06f else 1f)
    }

    private class Measure(val tokens: Tokens, val shows: (Node) -> Boolean, val measure: TextMeasure, val fontScale: Float) {
        /** [text]'s width at [size] sp, in dp, with a little to spare (fonts differ a bit from phone to phone). */
        fun textWidth(text: String, size: Float, weight: Weight = Weight.normal) = measure.width(text, size, weight) * 1.04f

        /** A line of text [size] sp tall, in dp, at the phone's font size setting. */
        fun line(size: Float) = size * fontScale * 1.18f

        fun kids(node: Node) = when (node) {
            is ColumnNode -> node.children.filter(shows)
            is RowNode -> node.children.filter(shows)
            else -> emptyList()
        }

        fun clockText(node: ClockNode) = runCatching {
            DateTimeFormatter.ofPattern(node.format, Locale.getDefault()).format(tokens.now)
        }.getOrElse { "00:00" }

        /** The width a node takes when nothing squeezes it, in dp. */
        fun width(node: Node, k: Float): Float = when (node) {
            is TextNode -> textWidth(tokens.fill(node.text), node.size * k, node.weight) + if (node.highlight) 4 * k else if (node.chip) 18 * k else 0f
            is ClockNode -> textWidth(clockText(node), node.size * k, node.weight)
            is IconNode -> node.size * k * 1.12f
            is RingNode -> node.size * k
            is ArtNode -> node.size * k
            is AppIconNode -> node.size * k
            is AnalogNode -> node.size * k
            is ScaleNode -> if (node.vertical) 30 * k else 0f
            is TimerNode -> textWidth("00:00", node.size * k)
            is SpacerNode -> if (node.grow) 0f else node.size * k
            is RowNode -> kids(node).sumOf { width(it, k).toDouble() }.toFloat() + 2 * node.padding * k
            is ColumnNode -> (kids(node).maxOfOrNull { width(it, k) } ?: 0f) + 2 * node.padding * k
            else -> 0f // Bars, lines, charts, maps and months take the width they're given.
        }

        /** The height a node needs at [avail] dp wide, in dp. */
        fun height(node: Node, avail: Float, k: Float): Float = when (node) {
            is TextNode -> {
                val w = textWidth(tokens.fill(node.text), node.size * k, node.weight)
                val lines = min(node.maxLines, max(1, ceil(w / max(1f, avail)).toInt()))
                lines * line(node.size * k) + if (node.highlight) 2 * k else if (node.chip) 8 * k else 0f
            }
            is ClockNode -> line(node.size * k)
            is IconNode -> node.size * k
            is RingNode -> node.size * k
            is ArtNode -> node.size * k
            is AppIconNode -> node.size * k
            is AnalogNode -> node.size * k
            // A flat ruler, with room for its taller marker tick.
            is ScaleNode -> if (!node.vertical) 26 * k else if (node.grow || node.length <= 0) 40 * k else node.length * k
            is TimerNode -> line(node.size * k)
            is ProgressNode -> node.height * k
            is DividerNode -> node.thickness * k
            is ChartNode -> node.height * k
            is SleepNode -> node.height * k
            is SpacerNode -> if (node.grow) 0f else node.size * k
            // A map that grows can be small; a fixed one is as tall as it says.
            is MapNode -> if (node.grow) 40 * k else node.height * k
            is MonthNode -> {
                val weeks = MonthGrid.weeks(java.time.YearMonth.from(tokens.now), MonthGrid.firstDay(node)).size
                val header = if (node.header) line((node.size + 4) * k) + max(2, node.size / 3) * k else 0f
                header + (weeks + 1) * node.size * k * 1.3f
            }
            is ColumnNode -> {
                val inner = avail - 2 * node.padding * k
                // A scrolling list is sized for its first few rows: the rest scroll into view instead of shrinking it all.
                val counted = if (node.scroll) kids(node).take(SCROLL_ROWS) else kids(node)
                counted.sumOf { height(it, inner, k).toDouble() }.toFloat() + 2 * node.padding * k
            }
            is RowNode -> {
                val inner = avail - 2 * node.padding * k
                val widths = shares(node, inner, k)
                (kids(node).mapIndexed { i, c -> height(c, widths[i], k) }.maxOrNull() ?: 0f) + 2 * node.padding * k
            }
        }

        /** The width each visible child of a row gets: its own, and the growing ones share what's left. */
        fun shares(node: RowNode, inner: Float, k: Float): List<Float> {
            val children = kids(node)
            val fixed = children.filter { !it.grow }.sumOf { width(it, k).toDouble() }.toFloat()
            val growing = children.count { it.grow }
            val left = max(0f, inner - fixed)
            return children.map { if (it.grow) left / max(1, growing) else min(width(it, k), inner) }
        }

        /**
         * [node] with each row that's too wide for its room fixed: its widest texts take the leftover room (and
         * end in "…") until the rest fits.
         */
        /**
         * [text] at a smaller size when its one line is wider than [avail] dp: just small enough to fit, but no
         * smaller than [MIN_TEXT] of its size. Text that may wrap onto more lines is left as it is.
         */
        fun shrunk(text: TextNode, avail: Float, k: Float): TextNode {
            if (text.maxLines > 1 && tokens.fill(text.text).trim().contains(' ')) return text
            val w = width(text, k)
            if (w <= avail + 0.5f || w <= 0f) return text
            val size = max(ceil(text.size * MIN_TEXT).toInt(), (text.size * avail / w).toInt())
            // One line: the home screen also shrinks it to fit itself, as its measure is the real one.
            return text.copy(size = min(size, text.size), autoFit = text.maxLines == 1 || !tokens.fill(text.text).trim().contains(' '))
        }

        /**
         * A text sharing a row's leftover room (a title beside a time or a value): only a little smaller, then
         * cut short with "…" as usual, rather than shrinking to a size next to its neighbours.
         */
        fun eased(text: TextNode, avail: Float, k: Float): TextNode {
            val w = width(text, k)
            if (w <= avail + 0.5f || w <= 0f) return text
            val size = max(ceil(text.size * MIN_SHARED).toInt(), (text.size * avail / w).toInt())
            return if (size < text.size) text.copy(size = size) else text
        }

        /**
         * [node] laid out in [w]×[h] dp, with each scrolling list's gaps opened up so it shows only whole rows: the
         * rows that fit entirely share out the room left below them, and the next one starts right at the bottom
         * edge, ready to scroll in, instead of showing cut in half. However many rows there are, it adapts.
         */
        fun spaced(node: Node, w: Float, h: Float, k: Float): Node = when (node) {
            is ColumnNode -> {
                val innerW = w - 2 * node.padding * k
                val innerH = h - 2 * node.padding * k
                if (node.scroll) spreadRows(node, innerW, innerH, k) else {
                    val shown = kids(node)
                    val fixed = shown.filter { !it.grow }.sumOf { height(it, innerW, k).toDouble() }.toFloat()
                    val growH = max(0f, innerH - fixed) / max(1, shown.count { it.grow })
                    node.copy(children = node.children.map { c ->
                        if (!shows(c)) c else spaced(c, innerW, if (c.grow) growH else height(c, innerW, k), k)
                    })
                }
            }
            is RowNode -> {
                val inner = w - 2 * node.padding * k
                val innerH = h - 2 * node.padding * k
                val widths = shares(node, inner, k)
                var i = 0
                node.copy(children = node.children.map { c -> if (!shows(c)) c else spaced(c, widths[i++], innerH, k) })
            }
            else -> node
        }

        private fun spreadRows(list: ColumnNode, w: Float, h: Float, k: Float): ColumnNode {
            val shown = kids(list)
            var used = 0f
            var whole = 0
            for (row in shown) {
                val rowH = height(row, w, k)
                if (used + rowH > h + 0.5f) break
                used += rowH
                whole++
            }
            // Everything fits (nothing to scroll), or not even one row does: leave it as it is.
            if (whole == 0 || whole == shown.size) return list
            // The room left, in the design's units, shared by the gaps above rows 2 to whole + 1.
            val left = ((h - used) / k).toInt()
            if (left <= 0) return list
            val gaps = List(whole) { left / whole + if (it < left % whole) 1 else 0 }
            val out = mutableListOf<Node>()
            var seen = 0
            for (c in list.children) {
                if (shows(c)) {
                    if (seen in 1..whole && gaps[seen - 1] > 0) out += SpacerNode(size = gaps[seen - 1])
                    seen++
                }
                out += c
            }
            return list.copy(children = out)
        }

        fun rows(node: Node, avail: Float, k: Float): Node = when (node) {
            is ColumnNode -> {
                val inner = avail - 2 * node.padding * k
                node.copy(children = node.children.map { c ->
                    when {
                        !shows(c) -> c
                        c is TextNode -> shrunk(c, inner, k)
                        else -> rows(c, inner, k)
                    }
                })
            }
            is RowNode -> {
                val inner = avail - 2 * node.padding * k
                var children = node.children
                fun fixedWidth() = children.filter { shows(it) && !it.grow }.sumOf { width(it, k).toDouble() }.toFloat()
                fun texts() = children.withIndex().filter { (_, c) -> shows(c) && !c.grow && c is TextNode }.sortedByDescending { (_, c) -> width(c, k) }
                // First the widest texts get smaller (to MIN_TEXT of their size), then, if it still doesn't fit,
                // they take the room that's left and end in "...".
                for ((i, c) in texts()) {
                    val over = fixedWidth() - inner
                    if (over <= 0.5f) break
                    children = children.toMutableList().also { it[i] = shrunk(c as TextNode, width(c, k) - over, k) }
                }
                for ((i, _) in texts()) {
                    if (fixedWidth() <= inner + 0.5f) break
                    children = children.toMutableList().also { it[i] = (it[i] as TextNode).copy(grow = true, maxLines = 1, autoFit = true) }
                }
                val fitted = node.copy(children = children)
                val widths = shares(fitted, inner, k)
                var w = 0
                // Texts that share the leftover room shrink to fit their share too.
                fitted.copy(children = fitted.children.map { c ->
                    if (!shows(c)) c else {
                        val share = widths[w++]
                        if (c is TextNode) (if (c.grow && node.children.count { shows(it) } > 1) eased(c, share, k) else shrunk(c, share, k)) else rows(c, share, k)
                    }
                })
            }
            else -> node
        }
    }
}
