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
        // The first hour returned is the current one; the forecast starts at the next.
        for (i in 1..5) {
            val time = times.getOrNull(i)?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: break
            out["weather.h$i.time"] = time.format(DateTimeFormatter.ofPattern("h a", Locale.US))
            out["weather.h$i.temp"] = degrees(temps?.at(i))
            out["weather.h$i.icon"] = weatherIcon(codes?.at(i)?.roundToInt() ?: 0, (days?.at(i) ?: 1.0) >= 1.0)
        }
        val daily = root["daily"].obj()
        val dates = (daily["time"] as? JsonArray)?.mapNotNull { it.str() }.orEmpty()
        for (i in 1..3) {
            val date = dates.getOrNull(i)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: break
            out["weather.d$i.day"] = date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
            out["weather.d$i.high"] = degrees((daily["temperature_2m_max"] as? JsonArray)?.at(i))
            out["weather.d$i.low"] = degrees((daily["temperature_2m_min"] as? JsonArray)?.at(i))
            out["weather.d$i.icon"] = weatherIcon((daily["weather_code"] as? JsonArray)?.at(i)?.roundToInt() ?: 0, true)
        }
        return out
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
        return if (minutes < 60) "$minutes min" else "${minutes / 60} h ${minutes % 60} min"
    }

    // ---- News (Google News RSS) ----

    fun news(body: String, count: Int = 5): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            // External entities off: the feed is remote content.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isExpandEntityReferences = false
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(body.toByteArray()))
        val items = doc.getElementsByTagName("item")
        val out = mutableMapOf<String, String>()
        for (i in 0 until minOf(count, items.length)) {
            val item = items.item(i) as Element
            val source = item.getElementsByTagName("source").item(0)?.textContent?.trim().orEmpty()
            var title = item.getElementsByTagName("title").item(0)?.textContent?.trim().orEmpty()
            // Google appends " - Source" to each title.
            if (source.isNotEmpty()) title = title.removeSuffix(" - $source")
            out["news.${i + 1}"] = title
            out["news.${i + 1}.source"] = source
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
