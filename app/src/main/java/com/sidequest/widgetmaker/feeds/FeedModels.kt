package com.sidequest.widgetmaker.feeds

import kotlinx.serialization.Serializable

/** A place found by name, with its coordinates. */
@Serializable
data class Place(val name: String, val lat: Double, val lon: Double)

/** What the user set up in Data sources. */
@Serializable
data class FeedSettings(
    /** Your town: used for weather, local news and nearby events. */
    val place: Place? = null,
    val fahrenheit: Boolean = true,

    /** Coin symbols, like BTC and ETH. */
    val coins: List<String> = emptyList(),
    /** CoinGecko currency code, like usd or eur. */
    val currency: String = "usd",

    /** Free key from developer.tomtom.com. */
    val tomtomKey: String = "",
    val home: Place? = null,
    val work: Place? = null,

    /** News search; blank means news about your town. */
    val newsTopic: String = "",

    /** Optional free key from developer.ticketmaster.com, for exact listings; without it, events come from local news. */
    val ticketmasterKey: String = "",
    val eventsRadiusMiles: Int = 25,

    /** Show your upcoming calendar events (needs the calendar permission). */
    val calendar: Boolean = false,
) {
    val weatherReady get() = place != null
    val cryptoReady get() = coins.isNotEmpty()
    val trafficReady get() = tomtomKey.isNotBlank() && home != null && work != null
    val newsReady get() = place != null || newsTopic.isNotBlank()
    val eventsReady get() = place != null
    val calendarReady get() = calendar

    /** One line per feed for Claude, so it knows what a design can show right now. */
    fun summary(): String = listOf(
        "weather" to if (weatherReady) "set up for ${place!!.name}" else "not set up",
        "crypto" to if (cryptoReady) "set up for ${coins.joinToString(", ")}" else "not set up",
        "traffic" to if (trafficReady) "set up (home to work)" else "not set up",
        "news" to if (newsReady) "set up (${newsTopic.ifBlank { place!!.name }})" else "not set up",
        "events" to when {
            !eventsReady -> "not set up"
            ticketmasterKey.isNotBlank() -> "set up near ${place!!.name} (Ticketmaster listings)"
            else -> "set up near ${place!!.name} (local articles about things to do: names are headlines, often long, and {events.N.when} is a day named in the headline or when it was posted)"
        },
        "calendar" to if (calendarReady) "set up (the user's own upcoming events)" else "not set up",
    ).joinToString("\n") { (k, v) -> "- $k: $v" }
}

/**
 * The latest values from every feed, as token name → text (like
 * "weather.temp" → "18°"). Saved on the phone and read by the widgets.
 */
@Serializable
data class FeedSnapshot(
    val values: Map<String, String> = emptyMap(),
    val updatedAt: Long = 0,
    /** Feed name → why its last refresh failed. */
    val errors: Map<String, String> = emptyMap(),
)
