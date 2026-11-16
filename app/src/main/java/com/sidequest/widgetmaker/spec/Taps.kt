package com.sidequest.widgetmaker.spec

/** The kinds of app a widget tap can open. */
enum class TapKind(val label: String) {
    weather("Weather"),
    crypto("Crypto"),
    news("News"),
    events("Events"),
    calendar("Calendar"),
    clock("Clock"),
    commute("Commute (maps)"),
    battery("Battery"),
    health("Health & fitness"),
    game("Side Quest"),
    music("Music"),
}

/** What a tap does once its values are filled in. */
sealed interface TapAction {
    /** Opens a web page or article. */
    data class Link(val url: String) : TapAction

    /** Opens one calendar event (one occurrence of it). */
    data class CalendarEvent(val id: Long, val beginMs: Long, val endMs: Long) : TapAction

    /** Opens the app picked for this kind (or asks which). */
    data class Open(val kind: TapKind) : TapAction

    /** Opens Side Quest on a screen: "eggs", "adventures" or "quests". */
    data class GameScreen(val screen: String) : TapAction

    /** Asks Side Quest to "complete" a quest, "claim" an adventure or "hatch" an egg. */
    data class GameAction(val action: String, val id: String) : TapAction

    /** Play/pause ("toggle"), "next" or "prev" on whatever is playing. */
    data class Media(val command: String) : TapAction

    /** Opens an app by its package name ("app:com.anthropic.claude"), or its store page if it isn't installed. */
    data class App(val packageName: String) : TapAction

    /** Starts a new event in the calendar app ("calendar-new"). */
    data object NewEvent : TapAction
}

/**
 * Works out what tapping each part of a widget does.
 *
 * A design can say it with "tap": an app kind ("weather") or a link value
 * ("{news.1.link}"). Without one, a headline, event or calendar entry opens
 * its own link, and the widget as a whole opens the app for what it mostly
 * shows (worked out from the values it uses).
 */
object Taps {
    private val TOKEN = Regex("\\{([\\w.]+)\\}")
    private val LISTED = Regex("^(news|events|calendar|mq\\.egg|mq\\.adv|mq\\.quest)\\.(\\d)$")

    /** The tap for one element: its own, or its item's link for a headline, event or calendar entry. */
    fun forNode(node: Node): String? {
        node.tap?.let { return it }
        // The time opens the Clock app; a date opens the calendar.
        if (node is MonthNode) return TapKind.calendar.name
        if (node is AnalogNode) return TapKind.clock.name
        if (node is ClockNode) return if (node.format.contains('h', ignoreCase = true)) TapKind.clock.name else TapKind.calendar.name
        if (node !is TextNode) return null
        val listed = TOKEN.findAll(node.text).map { it.groupValues[1] }.firstNotNullOfOrNull { LISTED.find(it) }
        return listed?.let { "{${it.groupValues[1]}.${it.groupValues[2]}.link}" }
    }

    /** The tap for the widget as a whole: the root's own, or the app kind for what it mostly shows. */
    fun forWidget(design: WidgetDesign): String? {
        design.root.tap?.let { return it }
        val kind = mainKind(design) ?: return null
        // A Side Quest widget opens the game on what it shows most: eggs, adventures or quests.
        if (kind == TapKind.game) gameScreen(design)?.let { return "mq-open:$it" }
        return kind.name
    }

    /** Which game screen a design is mostly about, from its Side Quest values. */
    fun gameScreen(design: WidgetDesign): String? {
        val text = DesignParser.encode(design)
        val counts = mapOf(
            "eggs" to Regex("\\{mq\\.egg").findAll(text).count(),
            "adventures" to Regex("\\{mq\\.adv").findAll(text).count(),
            "quests" to Regex("\\{mq\\.quest").findAll(text).count(),
        )
        return counts.filterValues { it > 0 }.maxByOrNull { it.value }?.key
    }

    /** The kind of data a design shows most, by how many elements use it. */
    fun mainKind(design: WidgetDesign): TapKind? {
        // A design that says what it opens (like a big clock with the weather under it) is that kind.
        design.root.tap?.let { tap -> TapKind.entries.firstOrNull { it.name == tap } }?.let { return it }
        return mostOf(kindsIn(design.root))
    }

    /**
     * The tap for a section of a bigger widget (a row or column below the top): a tile opens the app
     * for what it mostly shows, and a plain row or column that shows one kind of thing opens that.
     * So in a briefing the weather tile opens the weather and the steps line opens your health app,
     * rather than the whole widget opening one app. Side Quest parts are left to the widget's tap.
     */
    fun forSection(node: Node): String? {
        node.tap?.let { return it }
        val tile = when (node) {
            is ColumnNode -> node.background != null || node.tone != null
            is RowNode -> node.background != null || node.tone != null
            else -> return null
        }
        val counts = kindsIn(node)
        val kind = if (tile) mostOf(counts) else counts.keys.singleOrNull()
        return kind?.takeIf { it != TapKind.game }?.name
    }

    // Ties go to the more specific kinds (data before the clock).
    private fun mostOf(counts: Map<TapKind, Int>): TapKind? =
        counts.entries.maxWithOrNull(compareBy({ it.value }, { -it.key.ordinal }))?.key

    /** How many elements under [root] use each kind of data. */
    private fun kindsIn(root: Node): Map<TapKind, Int> {
        val counts = mutableMapOf<TapKind, Int>()
        fun add(kind: TapKind?) { if (kind != null) counts[kind] = (counts[kind] ?: 0) + 1 }
        fun visit(node: Node) {
            when (node) {
                is ColumnNode -> node.children.forEach(::visit)
                is RowNode -> node.children.forEach(::visit)
                is TextNode -> TOKEN.findAll(node.text).forEach { add(kindOf(it.groupValues[1])) }
                is ClockNode -> add(if (node.format.contains('h', ignoreCase = true)) TapKind.clock else TapKind.calendar)
                is IconNode -> add(if (node.name == IconName.weather || node.live?.contains("weather.") == true) TapKind.weather else null)
                is ProgressNode -> TOKEN.findAll(node.value).forEach { add(kindOf(it.groupValues[1])) }
                is ArtNode, is TimerNode -> add(TapKind.music)
                is SleepNode -> add(TapKind.health)
                is MonthNode -> repeat(3) { add(TapKind.calendar) }
                is MapNode -> repeat(3) { add(TapKind.commute) }
                is AnalogNode -> repeat(2) { add(TapKind.clock) }
                is ScaleNode -> TOKEN.findAll(node.value).forEach { add(kindOf(it.groupValues[1])) }
                is AppIconNode -> Unit
                is ChartNode -> TOKEN.findAll(node.series).forEach { add(kindOf(it.groupValues[1])) }
                is RingNode -> TOKEN.findAll(node.value).forEach { add(kindOf(it.groupValues[1])) }
                is SpacerNode, is DividerNode -> Unit
            }
        }
        visit(root)
        return counts
    }

    private fun kindOf(token: String): TapKind? = when (token.substringBefore('.')) {
        "weather" -> TapKind.weather
        "crypto" -> TapKind.crypto
        "news" -> TapKind.news
        "events" -> TapKind.events
        "calendar" -> TapKind.calendar
        "traffic", "map" -> TapKind.commute
        "battery", "charging", "devices" -> TapKind.battery
        "health", "noop" -> TapKind.health
        "mq" -> TapKind.game
        "music" -> TapKind.music
        else -> null
    }

    /** Fills in a tap's values. Null when there's nothing to open yet (no data). */
    fun resolve(raw: String, tokens: Tokens): TapAction? {
        val value = tokens.fill(raw).trim()
        if (value.isEmpty() || value == "--" || value.contains('{')) return null
        if (value.startsWith("https://") || value.startsWith("http://")) return TapAction.Link(value)
        if (value.startsWith("mq-open:")) {
            return value.removePrefix("mq-open:").takeIf { it in setOf("eggs", "adventures", "quests") }?.let { TapAction.GameScreen(it) }
        }
        if (value.startsWith("mq-action:")) {
            val parts = value.removePrefix("mq-action:").split(':', limit = 2)
            return if (parts.size == 2 && parts[0] in setOf("complete", "claim", "hatch") && parts[1].isNotBlank()) TapAction.GameAction(parts[0], parts[1]) else null
        }
        if (value.startsWith("music:")) {
            return value.removePrefix("music:").takeIf { it in setOf("toggle", "next", "prev") }?.let { TapAction.Media(it) }
        }
        if (value == "calendar-new") return TapAction.NewEvent
        if (value.startsWith("app:")) {
            return value.removePrefix("app:").trim().takeIf { Regex("[A-Za-z0-9_.]+").matches(it) }?.let { TapAction.App(it) }
        }
        if (value.startsWith("calendar-event:")) {
            val parts = value.removePrefix("calendar-event:").split(':').mapNotNull { it.toLongOrNull() }
            return if (parts.size == 3) TapAction.CalendarEvent(parts[0], parts[1], parts[2]) else null
        }
        return runCatching { TapAction.Open(TapKind.valueOf(value)) }.getOrNull()
    }
}
