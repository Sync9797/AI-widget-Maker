package com.sidequest.widgetmaker.feeds

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns each service's response into token values. Kept free of Android and
 * networking so it can be unit tested with saved responses.
 */
object FeedParsers {
    private val json = Json { ignoreUnknownKeys = true }

    // ---- Weather (Open-Meteo) ----

    fun weather(body: String, place: Place, fahrenheit: Boolean): Map<String, String> {
        val root = json.parseToJsonElement(body).obj()
        val current = root["current"].obj()
        val daily = root["daily"].obj()
        val code = current["weather_code"].int() ?: 0
        val isDay = (current["is_day"].int() ?: 1) == 1
        val unit = if (fahrenheit) "mph" else "km/h"
        return forecast(root) + mapOf(
            "weather.temp" to degrees(current["temperature_2m"].double()),
            "weather.feelsLike" to degrees(current["apparent_temperature"].double()),
            "weather.high" to degrees(daily["temperature_2m_max"].first()),
            "weather.low" to degrees(daily["temperature_2m_min"].first()),
            "weather.rain" to (daily["precipitation_probability_max"].first()?.roundToInt()?.let { "$it%" } ?: "--"),
            "weather.wind" to (current["wind_speed_10m"].double()?.roundToInt()?.let { "$it $unit" } ?: "--"),
            "weather.condition" to condition(code),
            "weather.icon" to weatherIcon(code, isDay),
            "weather.mc" to meteocon(code, isDay),
            "weather.place" to place.name.substringBefore(","),
        )
    }

    private fun degrees(value: Double?) = value?.let { "${it.roundToInt()}°" } ?: "--"

    /**
     * The next hours (weather.h1 to h5: time, temp, icon) and the next days
     * (weather.d1 to d3: day, high, low, icon), for bigger widgets.
     */
    private fun forecast(root: JsonObject): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val hourly = root["hourly"].obj()
        val times = (hourly["time"] as? JsonArray)?.mapNotNull { it.str() }.orEmpty()
        val temps = hourly["temperature_2m"] as? JsonArray
        val codes = hourly["weather_code"] as? JsonArray
        val days = hourly["is_day"] as? JsonArray
        val rain = hourly["precipitation_probability"] as? JsonArray
        // The first hour returned is the current one; the forecast starts at the next.
        for (i in 1..5) {
            val time = times.getOrNull(i)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: break
            out["weather.h$i.time"] = time.format(DateTimeFormatter.ofPattern("h a", Locale.US))
            out["weather.h$i.time24"] = time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
            out["weather.h$i.temp"] = degrees(temps?.at(i))
            out["weather.h$i.rain"] = rain?.at(i)?.roundToInt()?.let { "$it%" } ?: "--"
            out["weather.h$i.icon"] = weatherIcon(codes?.at(i)?.roundToInt() ?: 0, (days?.at(i) ?: 1.0) >= 1.0)
            out["weather.h$i.mc"] = meteocon(codes?.at(i)?.roundToInt() ?: 0, (days?.at(i) ?: 1.0) >= 1.0)
        }
        outlook(times, codes, rain)?.let { out["weather.outlook"] = it }
        val daily = root["daily"].obj()
        val dates = (daily["time"] as? JsonArray)?.mapNotNull { it.str() }.orEmpty()
        for (i in 1..3) {
            val date = dates.getOrNull(i)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: break
            out["weather.d$i.day"] = date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
            out["weather.d$i.high"] = degrees((daily["temperature_2m_max"] as? JsonArray)?.at(i))
            out["weather.d$i.low"] = degrees((daily["temperature_2m_min"] as? JsonArray)?.at(i))
            out["weather.d$i.icon"] = weatherIcon((daily["weather_code"] as? JsonArray)?.at(i)?.roundToInt() ?: 0, true)
            out["weather.d$i.mc"] = meteocon((daily["weather_code"] as? JsonArray)?.at(i)?.roundToInt() ?: 0, true)
        }
        return out
    }

    /**
     * A line about the next 12 hours, like a weather app's: "Rain possible
     * tonight", "Snow likely this afternoon" or "Dry for the next 12 hours".
     */
    private fun outlook(times: List<String>, codes: JsonArray?, rain: JsonArray?): String? {
        if (times.size < 2) return null
        val start = runCatching { LocalDateTime.parse(times[0]) }.getOrNull() ?: return null
        var best = -1
        var bestChance = -1.0
        for (i in 1 until minOf(times.size, 13)) {
            val chance = rain?.at(i) ?: continue
            if (chance > bestChance) { bestChance = chance; best = i }
        }
        if (best < 0) return null
        val snow = (1 until minOf(times.size, 13)).any { (codes?.at(it)?.roundToInt() ?: 0) in setOf(71, 73, 75, 77, 85, 86) }
        val storm = (1 until minOf(times.size, 13)).any { (codes?.at(it)?.roundToInt() ?: 0) >= 95 }
        val what = when { storm -> "Storms"; snow -> "Snow"; else -> "Rain" }
        val time = runCatching { LocalDateTime.parse(times[best]) }.getOrNull() ?: return null
        return when {
            bestChance >= 60 -> "$what likely ${period(start, time)}"
            bestChance >= 25 -> "$what possible ${period(start, time)}"
            else -> "Dry for the next 12 hours"
        }
    }

    /** "this afternoon", "tonight", "tomorrow morning"... for a time soon after [now]. */
    fun period(now: LocalDateTime, time: LocalDateTime): String {
        val tomorrow = time.toLocalDate().isAfter(now.toLocalDate())
        val h = time.hour
        return when {
            tomorrow && h < 5 -> "tonight"
            tomorrow && h < 12 -> "tomorrow morning"
            tomorrow -> "tomorrow"
            h < 5 -> "overnight"
            h < 12 -> "this morning"
            h < 17 -> "this afternoon"
            h < 21 -> "this evening"
            else -> "tonight"
        }
    }

    private fun JsonArray.at(i: Int): Double? = (getOrNull(i) as? JsonPrimitive)?.doubleOrNull

    /** WMO weather codes, as Open-Meteo reports them. */
    fun condition(code: Int) = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "--"
    }

    /** Which icon a "weather" icon element shows. */
    /**
     * The Meteocon (by Bas Milius, MIT) for a WMO weather code, by day or night: its name in
     * res/drawable-nodpi, like "partly_cloudy_night_rain" for mc_partly_cloudy_night_rain_00.
     */
    fun meteocon(code: Int, isDay: Boolean): String {
        val dn = if (isDay) "day" else "night"
        return when (code) {
            0 -> "clear_$dn"
            1, 2 -> "partly_cloudy_$dn"
            3 -> "overcast_$dn"
            45, 48 -> "fog_$dn"
            51, 53, 55 -> "drizzle"
            56, 57, 66, 67 -> "sleet"
            61, 63, 65 -> "rain"
            71, 73, 75, 77 -> "snow"
            80, 81, 82 -> "partly_cloudy_${dn}_rain"
            85, 86 -> "partly_cloudy_${dn}_snow"
            95 -> "thunderstorms_${dn}_rain"
            96, 99 -> "thunderstorms_hail"
            else -> "overcast_$dn"
        }
    }

    fun weatherIcon(code: Int, isDay: Boolean) = when (code) {
        0, 1 -> if (isDay) "sun" else "moon"
        2, 3, 45, 48 -> "cloud"
        in 51..67, in 80..82 -> "rain"
        in 71..77, 85, 86 -> "snow"
        in 95..99 -> "storm"
        else -> "cloud"
    }

    /** Open-Meteo geocoding: a town name to a place. */
    fun place(body: String): Place? {
        val first = (json.parseToJsonElement(body).obj()["results"] as? JsonArray)?.firstOrNull().obj()
        val lat = first["latitude"].double() ?: return null
        val lon = first["longitude"].double() ?: return null
        val name = listOfNotNull(first["name"].str(), first["admin1"].str() ?: first["country"].str())
            .distinct().joinToString(", ")
        return Place(name, lat, lon)
    }

    // ---- Crypto (CoinGecko) ----

    /** Common symbols to CoinGecko ids; anything else is looked up with [coinSearch]. */
    val KNOWN_COINS = mapOf(
        "BTC" to "bitcoin", "ETH" to "ethereum", "SOL" to "solana", "XRP" to "ripple",
        "DOGE" to "dogecoin", "ADA" to "cardano", "BNB" to "binancecoin", "LTC" to "litecoin",
        "DOT" to "polkadot", "AVAX" to "avalanche-2", "LINK" to "chainlink", "SHIB" to "shiba-inu",
        "TRX" to "tron", "XLM" to "stellar", "USDT" to "tether", "USDC" to "usd-coin",
    )

    /** CoinGecko search: the id of the coin with this exact symbol, if any. */
    fun coinSearch(body: String, symbol: String): String? =
        (json.parseToJsonElement(body).obj()["coins"] as? JsonArray)
            ?.map { it.obj() }
            ?.firstOrNull { it["symbol"].str().equals(symbol, ignoreCase = true) }
            ?.get("id").str()

    /** CoinGecko simple/price for [ids] (symbol → id). */
    fun crypto(body: String, ids: Map<String, String>, currency: String): Map<String, String> {
        val root = json.parseToJsonElement(body).obj()
        val out = mutableMapOf<String, String>()
        ids.forEach { (symbol, id) ->
            val coin = root[id].obj()
            val price = coin[currency].double()
            val change = coin["${currency}_24h_change"].double()
            out["crypto.$symbol.price"] = price?.let { money(it, currency) } ?: "--"
            out["crypto.$symbol.change"] = change?.let { signed(it) + "%" } ?: "--"
            out["crypto.$symbol.name"] = symbol
        }
        return out
    }

    fun money(value: Double, currency: String): String {
        val pattern = when {
            value >= 1000 -> "#,##0"
            value >= 1 -> "#,##0.00"
            else -> "0.0000"
        }
        val number = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
            .apply { roundingMode = RoundingMode.HALF_UP }
            .format(value)
        return when (currency.lowercase()) {
            "usd", "cad", "aud", "nzd" -> "$$number"
            "eur" -> "€$number"
            "gbp" -> "£$number"
            "jpy" -> "¥$number"
            "inr" -> "₹$number"
            else -> "$number ${currency.uppercase()}"
        }
    }

    fun signed(value: Double): String {
        val text = DecimalFormat("0.0", DecimalFormatSymbols(Locale.US))
            .apply { roundingMode = RoundingMode.HALF_UP }
            .format(abs(value))
        return if (value < 0) "-$text" else "+$text"
    }

    // ---- Traffic (TomTom) ----

    /** TomTom geocode: an address to a place. */
    fun tomtomPlace(body: String): Place? {
        val first = (json.parseToJsonElement(body).obj()["results"] as? JsonArray)?.firstOrNull().obj()
        val position = first["position"].obj()
        val lat = position["lat"].double() ?: return null
        val lon = position["lon"].double() ?: return null
        return Place(first["address"].obj()["freeformAddress"].str() ?: "$lat,$lon", lat, lon)
    }

    /** TomTom route summary. [toWork] says which way the trip goes (for the label). */
    fun traffic(body: String, toWork: Boolean, miles: Boolean): Map<String, String> {
        val route = (json.parseToJsonElement(body).obj()["routes"] as? JsonArray)?.firstOrNull().obj()
        val summary = route["summary"].obj()
        val seconds = summary["travelTimeInSeconds"].int() ?: return emptyMap()
        val delay = summary["trafficDelayInSeconds"].int() ?: 0
        val meters = summary["lengthInMeters"].int() ?: 0
        val distance = if (miles) "%.1f mi".format(Locale.US, meters / 1609.34) else "%.1f km".format(Locale.US, meters / 1000.0)
        return mapOf(
            "traffic.commute" to duration(seconds),
            "traffic.delay" to if (delay >= 60) "+${duration(delay)}" else "No delays",
            "traffic.distance" to distance,
            "traffic.to" to if (toWork) "Work" else "Home",
        )
    }

    fun duration(seconds: Int): String {
        val minutes = (seconds / 60.0).roundToInt()
        return when {
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0 -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }
    }

    // ---- News (Google News RSS) ----

    /** A Google News RSS item. */
    data class Headline(val title: String, val source: String, val published: LocalDate?, val link: String = "")

    private val DOCTYPE = Regex("<!DOCTYPE(?:[^\\[>]|\\[[^\\]]*\\])*>", RegexOption.IGNORE_CASE)

    fun headlines(body: String): List<Headline> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // External entities off: the feed is remote content. Android's parser doesn't know this
            // feature (it throws, which broke news and events on phones), and doesn't fetch external
            // entities anyway, so it's set only where it exists; the DOCTYPE is dropped below either way.
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            isExpandEntityReferences = false
        }
        // An RSS feed never needs a DOCTYPE; without one, no entity can be declared.
        val safe = DOCTYPE.replace(body, "")
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(safe.toByteArray()))
        val items = doc.getElementsByTagName("item")
        return (0 until items.length).map { i ->
            val item = items.item(i) as Element
            fun text(tag: String) = item.getElementsByTagName(tag).item(0)?.textContent?.trim().orEmpty()
            val source = text("source")
            // Google appends " - Source" to each title.
            val title = if (source.isNotEmpty()) text("title").removeSuffix(" - $source") else text("title")
            val published = runCatching {
                java.time.ZonedDateTime.parse(text("pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate()
            }.getOrNull()
            Headline(title, source, published, text("link"))
        }
    }

    fun news(body: String, count: Int = 5, hideObituaries: Boolean = true, muted: List<String> = emptyList()): Map<String, String> {
        val out = mutableMapOf<String, String>()
        headlines(body).filter { keep(it, hideObituaries, muted) }.take(count).forEachIndexed { i, h ->
            out["news.${i + 1}"] = h.title
            out["news.${i + 1}.source"] = h.source
            out["news.${i + 1}.link"] = h.link
        }
        return out
    }

    /**
     * Events without an account: recent local articles about things to do,
     * from Google News. Headlines that look like events come first, crime and
     * accident news is dropped, and articles older than two weeks are skipped.
     * events.N is the headline, events.N.when the day it names ("This weekend",
     * "Sat", "Nov 14") or else when it was posted, events.N.venue the publication.
     */
    fun eventArticles(
        body: String,
        today: LocalDate,
        count: Int = 5,
        hideObituaries: Boolean = true,
        muted: List<String> = emptyList(),
    ): Map<String, String> {
        val recent = headlines(body)
            .filter { keep(it, hideObituaries, muted) }
            .filter { it.published == null || !it.published.isBefore(today.minusDays(14)) }
            .filter { eventScore(it.title) >= 0 }
        // Event-like headlines first (most event words first), then the rest, each in the feed's order.
        val ranked = recent.sortedByDescending { minOf(eventScore(it.title), 3) }
        val out = mutableMapOf<String, String>()
        ranked.take(count).forEachIndexed { i, h ->
            out["events.${i + 1}"] = h.title
            out["events.${i + 1}.when"] = headlineWhen(h.title, today)
                ?: if (h.published == today) "New today" else "Posted ${whenText(h.published, null, today)}".trim()
            out["events.${i + 1}.venue"] = h.source
            out["events.${i + 1}.link"] = h.link
        }
        return out
    }

    /** Words that mark a death notice, in the headline. */
    private val OBITUARY_WORDS = listOf(
        "obituar", "obit:", "obit -", " obit ", "passed away", "passes away", "in loving memory", "funeral",
        "memorial service", "celebration of life", "laid to rest", "visitation for", "remembering ",
        "in memoriam", "rest in peace", "survived by", "dies at", "died at", "condolence",
    )

    /** Sites that publish death notices. */
    private val OBITUARY_SOURCES = listOf("legacy", "echovita", "tribute", "funeral", "obituar", "dignity memorial", "everloved")

    /** Whether a headline reads like an obituary or death notice. */
    fun isObituary(h: Headline): Boolean {
        val t = " ${h.title.lowercase()} "
        val src = h.source.lowercase()
        return OBITUARY_WORDS.any { t.contains(it) } || OBITUARY_SOURCES.any { src.contains(it) } ||
            h.link.lowercase().let { "obituar" in it || "legacy.com" in it }
    }

    /** Whether to show a headline: not an obituary (when hidden), and none of the user's muted words. */
    fun keep(h: Headline, hideObituaries: Boolean, muted: List<String>): Boolean {
        if (hideObituaries && isObituary(h)) return false
        val t = h.title.lowercase()
        return muted.none { w -> w.isNotBlank() && t.contains(w.trim().lowercase()) }
    }

    private val EVENT_WORDS = listOf(
        "things to do", "this weekend", "weekend", "event", "concert", "festival", "fest ", "fair", "market",
        "parade", "live music", "show", "performance", "exhibit", "museum", "tickets", "tour", "comedy",
        "theater", "theatre", "celebrat", "holiday", "fireworks", "marathon", "5k", "game day", "opening",
        "tonight", "free ", "family", "kids", "food truck", "brewery", "art walk",
    )

    private val SKIP_WORDS = listOf(
        "police", "shooting", " shot", "killed", "kills", " dies", " died", " dead", "death", "crash", "arrest",
        "charged", "lawsuit", "murder", "stabb", "injur", "missing", "sentenced", "court", "suspect", "robbery",
    )

    /** How event-like a headline is: the number of event words, or -1 for crime and accident news. */
    fun eventScore(title: String): Int {
        val t = " ${title.lowercase()} "
        if (SKIP_WORDS.any { t.contains(it) }) return -1
        return EVENT_WORDS.count { t.contains(it) }
    }

    private val WEEKDAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    private val MONTH_DAY = Regex(
        "\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b",
        RegexOption.IGNORE_CASE,
    )

    /** The day a headline names, if any: "Tonight", "This weekend", "Sat", "Nov 14". */
    fun headlineWhen(title: String, today: LocalDate): String? {
        val t = title.lowercase()
        if ("tonight" in t) return "Tonight"
        if ("this weekend" in t) return "This weekend"
        if ("next weekend" in t) return "Next weekend"
        MONTH_DAY.find(title)?.let { m ->
            val month = m.groupValues[1].lowercase().take(3)
            val day = m.groupValues[2].toInt()
            if (day in 1..31) return month.replaceFirstChar { it.uppercase() } + " " + day
        }
        WEEKDAYS.firstOrNull { Regex("\\b$it\\b").containsMatchIn(t) }?.let { name ->
            val day = java.time.DayOfWeek.valueOf(name.uppercase())
            return if (day == today.dayOfWeek) "Today" else name.take(3).replaceFirstChar { it.uppercase() }
        }
        return null
    }

    // ---- Your calendar (on the phone) ----

    /** An upcoming event from the phone's calendar. */
    data class CalendarEvent(
        val title: String,
        val start: LocalDateTime,
        val end: LocalDateTime,
        val allDay: Boolean,
        val location: String,
        /** The calendar's event id and this occurrence's times, to open it on tap. */
        val id: Long = 0,
        val beginMs: Long = 0,
        val endMs: Long = 0,
    )

    /**
     * calendar.N (title), calendar.N.when ("Now", "Today 3 PM", "Tomorrow",
     * "Sat 9 AM"), calendar.N.where, and calendar.today ("2 more today").
     * [events] must be in start order; ones that have ended are skipped.
     */
    fun calendar(events: List<CalendarEvent>, now: LocalDateTime, count: Int = 5): Map<String, String> {
        val today = now.toLocalDate()
        val upcoming = events.filter { it.end.isAfter(now) }
        val out = mutableMapOf<String, String>()
        upcoming.take(count).forEachIndexed { i, e ->
            out["calendar.${i + 1}"] = e.title.ifBlank { "(No title)" }
            out["calendar.${i + 1}.when"] = when {
                e.allDay -> whenText(e.start.toLocalDate(), null, today).ifBlank { "All day" }
                !e.start.isAfter(now) -> "Now"
                else -> whenText(e.start.toLocalDate(), e.start.toLocalTime(), today)
            }
            out["calendar.${i + 1}.where"] = e.location
            if (e.id > 0) out["calendar.${i + 1}.link"] = "calendar-event:${e.id}:${e.beginMs}:${e.endMs}"
            // For "starts soon" (worked out when the widget draws): timed events only.
            if (!e.allDay && e.beginMs > 0) {
                out["calendar.${i + 1}.startMs"] = e.beginMs.toString()
                out["calendar.${i + 1}.endMs"] = e.endMs.toString()
            }
        }
        // Tomorrow's first event, for evening briefings.
        upcoming.firstOrNull { it.start.toLocalDate() == today.plusDays(1) }?.let { e ->
            out["calendar.tomorrow"] = e.title.ifBlank { "(No title)" }
            out["calendar.tomorrow.when"] = if (e.allDay) "All day" else e.start.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)).replace(":00", "")
            if (e.id > 0) out["calendar.tomorrow.link"] = "calendar-event:${e.id}:${e.beginMs}:${e.endMs}"
        }
        val leftToday = upcoming.count { it.start.toLocalDate() == today || (it.allDay && it.start.toLocalDate() == today) }
        out["calendar.today"] = when (leftToday) {
            0 -> "Nothing else today"
            1 -> "1 more today"
            else -> "$leftToday more today"
        }
        return out
    }

    // ---- Events (Ticketmaster Discovery) ----

    fun events(body: String, today: LocalDate, count: Int = 5): Map<String, String> {
        val events = (json.parseToJsonElement(body).obj()["_embedded"].obj()["events"] as? JsonArray) ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        events.take(count).forEachIndexed { i, element ->
            val event = element.obj()
            val start = event["dates"].obj()["start"].obj()
            val date = start["localDate"].str()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val time = start["localTime"].str()?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            val venue = (event["_embedded"].obj()["venues"] as? JsonArray)?.firstOrNull().obj()["name"].str()
            out["events.${i + 1}"] = event["name"].str().orEmpty()
            out["events.${i + 1}.link"] = event["url"].str().orEmpty()
            out["events.${i + 1}.when"] = whenText(date, time, today)
            out["events.${i + 1}.venue"] = venue.orEmpty()
        }
        return out
    }

    fun whenText(date: LocalDate?, time: LocalTime?, today: LocalDate): String {
        val day = when {
            date == null -> ""
            date == today -> "Today"
            date == today.plusDays(1) -> "Tomorrow"
            date.isBefore(today.plusDays(7)) -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
            else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        }
        val clock = time?.let {
            LocalDateTime.of(today, it).format(DateTimeFormatter.ofPattern(if (it.minute == 0) "h a" else "h:mm a", Locale.US))
        }
        return listOfNotNull(day.ifBlank { null }, clock).joinToString(" ")
    }

    // ---- JSON helpers ----

    private fun JsonElement?.obj(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())
    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
    private fun JsonElement?.double(): Double? = (this as? JsonPrimitive)?.doubleOrNull
    private fun JsonElement?.int(): Int? = (this as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.roundToInt() }
    private fun JsonElement?.first(): Double? = ((this as? JsonArray)?.firstOrNull() as? JsonPrimitive)?.doubleOrNull
}
