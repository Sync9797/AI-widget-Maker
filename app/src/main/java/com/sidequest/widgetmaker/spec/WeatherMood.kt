package com.sidequest.widgetmaker.spec

/**
 * A weather widget's color for the weather now, like Samsung's weather widgets: sky blue by day, warm
 * around sunrise and sunset, slate teal in rain, indigo in snow, lavender when it's cloudy. At night
 * there's none: the widget keeps its usual look. [top] and [bottom] are the gradient's ends (ARGB).
 */
enum class WeatherMood(val top: Int, val bottom: Int) {
    day(0xFF3A6CC0.toInt(), 0xFF5C96D2.toInt()),
    golden(0xFF7F86AE.toInt(), 0xFFE39A5E.toInt()),
    rain(0xFF3D6A75.toInt(), 0xFF50808A.toInt()),
    snow(0xFF3C4889.toInt(), 0xFF5160A6.toInt()),
    cloudy(0xFF5B5990.toInt(), 0xFF7370A6.toInt());

    companion object {
        /** How long either side of sunrise and sunset counts as golden. */
        const val GOLDEN_MS = 45 * 60_000L

        /** The mood for [design] now: only for weather widgets, and only with Settings' weather moods on. */
        fun forDesign(design: WidgetDesign, look: Look, feeds: Map<String, String>, nowMs: Long): WeatherMood? =
            if (look.weatherMoods && Taps.mainKind(design) == TapKind.weather) of(feeds, nowMs) else null

        /** The mood for the weather values [feeds] at [nowMs], or null at night (or with no weather yet). */
        fun of(feeds: Map<String, String>, nowMs: Long): WeatherMood? {
            val code = feeds["weather.code"]?.toIntOrNull() ?: return null
            val sunrise = feeds["weather.sunrise"]?.toLongOrNull()
            val sunset = feeds["weather.sunset"]?.toLongOrNull()
            val (night, nearSun) = if (sunrise != null && sunset != null) {
                (nowMs < sunrise - GOLDEN_MS || nowMs > sunset + GOLDEN_MS) to
                    (kotlin.math.abs(nowMs - sunrise) <= GOLDEN_MS || kotlin.math.abs(nowMs - sunset) <= GOLDEN_MS)
            } else (feeds["weather.isDay"] == "0") to false
            if (night) return null
            return when (code) {
                in 51..67, in 80..82, in 95..99 -> rain
                in 71..77, 85, 86 -> snow
                3, 45, 48 -> cloudy
                else -> if (nearSun) golden else day
            }
        }
    }
}
