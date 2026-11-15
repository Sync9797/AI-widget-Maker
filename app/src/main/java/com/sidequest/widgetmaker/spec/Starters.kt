package com.sidequest.widgetmaker.spec

/**
 * Ready-made designs that work without any AI: pick one, save it, add it.
 * Each is responsive (more appears as the widget grows) and suits every theme.
 */
object Starters {
    data class Starter(val title: String, val about: String, val design: WidgetDesign) {
        /** Worked out once: the gallery groups by it each time it draws. */
        val category: Category by lazy { categoryOf(this) }
    }

    /** How the app groups ready-made designs. */
    enum class Category(val label: String) {
        everyday("Everyday"),
        clock("Clock & date"),
        weather("Weather"),
        calendar("Calendar"),
        health("Health & fitness"),
        music("Music"),
        info("Money, news & travel"),
        battery("Battery"),
        game("Side Quest"),
    }

    private val EVERYDAY = setOf("Daily briefing", "Briefing list", "Smart stack", "Countdown", "Claude", "Gemini")

    fun categoryOf(s: Starter): Category {
        if (s.title in EVERYDAY) return Category.everyday
        return when (Taps.mainKind(s.design)) {
            TapKind.weather -> Category.weather
            TapKind.calendar -> DesignParser.encode(s.design).let { if (it.contains("calendar.") || it.contains("\"type\":\"month\"")) Category.calendar else Category.clock }
            TapKind.clock -> Category.clock
            TapKind.health -> Category.health
            TapKind.music -> Category.music
            TapKind.crypto, TapKind.news, TapKind.events, TapKind.commute -> Category.info
            TapKind.battery -> Category.battery
            TapKind.game -> Category.game
            null -> Category.everyday
        }
    }

    val all: List<Starter> by lazy {
        StarterDesigns.SOURCES.map { (title, about, json) -> Starter(title, about, DesignParser.parse(json)) }
    }

}
