package com.sidequest.widgetmaker.feeds

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import com.sidequest.widgetmaker.spec.DesignParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Data sources: where their settings and latest values are kept, and how
 * they're fetched. Weather (Open-Meteo), crypto (CoinGecko) and news (Google
 * News) need no account; traffic (TomTom) and events (Ticketmaster) need the
 * user's own free key.
 */
class Feeds(private val context: Context) {
    private val prefs = context.getSharedPreferences("feeds", Context.MODE_PRIVATE)
    private val snapshotFile = File(context.filesDir, "feeds.json")

    var settings: FeedSettings
        get() = prefs.getString("settings", null)
            ?.let { runCatching { DesignParser.json.decodeFromString(FeedSettings.serializer(), it) }.getOrNull() }
            ?: FeedSettings()
        set(value) = prefs.edit().putString("settings", DesignParser.json.encodeToString(FeedSettings.serializer(), value)).apply()

    fun snapshot(): FeedSnapshot = synchronized(LOCK) {
        if (!snapshotFile.exists()) return FeedSnapshot()
        runCatching { DesignParser.json.decodeFromString(FeedSnapshot.serializer(), snapshotFile.readText()) }
            .getOrDefault(FeedSnapshot())
    }

    private fun save(snapshot: FeedSnapshot) = synchronized(LOCK) {
        val tmp = File(snapshotFile.parentFile, snapshotFile.name + ".tmp")
        tmp.writeText(DesignParser.json.encodeToString(FeedSnapshot.serializer(), snapshot))
        tmp.renameTo(snapshotFile)
    }

    /**
     * Fetches every feed that's set up, in parallel, and saves what came back.
     * [only] limits it to some feeds (the rest keep their last values).
     */
    suspend fun refresh(only: Set<String>? = null): FeedSnapshot = withContext(Dispatchers.IO) {
        val s = settings
        val previous = snapshot()
        coroutineScope {
            val jobs = listOfNotNull(
                if (s.weatherReady) "weather" to async { runCatching { weather(s) } } else null,
                if (s.cryptoReady) "crypto" to async { runCatching { crypto(s) } } else null,
                if (s.trafficReady) "traffic" to async { runCatching { traffic(s) } } else null,
                if (s.newsReady) "news" to async { runCatching { news(s) } } else null,
                if (s.eventsReady) "events" to async { runCatching { events(s) } } else null,
                if (s.calendarReady) "calendar" to async { runCatching { calendar() } } else null,
                if (s.healthReady) "health" to async { runCatching { Health.read(context, s.stepGoal) } } else null,
                if (s.monsterQuest && MonsterQuest.installed(context)) "mq" to async { runCatching { MonsterQuest.read(context) } } else null,
            ).filter { only == null || it.first in only }
            // Feeds not refreshed this time keep their last values.
            val values = if (only == null) mutableMapOf() else previous.values.filterKeys { k -> only.none { k.startsWith("$it.") } }.toMutableMap()
            val errors = mutableMapOf<String, String>()
            jobs.forEach { (name, job) ->
                job.await()
                    .onSuccess { values.putAll(it) }
                    .onFailure { e ->
                        errors[name] = e.message ?: e.javaClass.simpleName
                        // Keep showing the last good values for a feed that failed this time.
                        values.putAll(previous.values.filterKeys { it.startsWith("$name.") })
                    }
            }
            values["updated"] = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
            FeedSnapshot(values, System.currentTimeMillis(), errors).also { save(it) }
        }
    }

    // ---- Lookups used by the settings screen ----

    /** A town by name, for weather, news and events. */
    suspend fun findTown(name: String): Place? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", name.trim())
            .addQueryParameter("count", "1")
            .addQueryParameter("language", "en")
            .build()
        FeedParsers.place(get(url))
    }

    /** A street address, for traffic. */
    suspend fun findAddress(address: String, tomtomKey: String): Place? = withContext(Dispatchers.IO) {
        val url = HttpUrl.Builder().scheme("https").host("api.tomtom.com")
            .addPathSegments("search/2/geocode")
            .addPathSegment(address.trim() + ".json")
            .addQueryParameter("key", tomtomKey.trim())
            .addQueryParameter("limit", "1")
            .build()
        FeedParsers.tomtomPlace(get(url))
    }

    // ---- Each feed ----

    private fun weather(s: FeedSettings): Map<String, String> {
        val place = s.place!!
        val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", place.lat.toString())
            .addQueryParameter("longitude", place.lon.toString())
            .addQueryParameter("current", "temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m")
            .addQueryParameter("hourly", "temperature_2m,weather_code,is_day,precipitation_probability")
            .addQueryParameter("forecast_hours", "7")
            .addQueryParameter("daily", "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weather_code")
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "4")
            .addQueryParameter("temperature_unit", if (s.fahrenheit) "fahrenheit" else "celsius")
            .addQueryParameter("wind_speed_unit", if (s.fahrenheit) "mph" else "kmh")
            .build()
        return FeedParsers.weather(get(url), place, s.fahrenheit)
    }

    private fun crypto(s: FeedSettings): Map<String, String> {
        val ids = s.coins.map { it.uppercase() }.distinct().associateWith { symbol ->
            FeedParsers.KNOWN_COINS[symbol] ?: coinIds.getOrPut(symbol) {
                val url = "https://api.coingecko.com/api/v3/search".toHttpUrl().newBuilder()
                    .addQueryParameter("query", symbol).build()
                FeedParsers.coinSearch(get(url), symbol) ?: throw IOException("No coin with the symbol $symbol")
            }
        }
        val url = "https://api.coingecko.com/api/v3/simple/price".toHttpUrl().newBuilder()
            .addQueryParameter("ids", ids.values.joinToString(","))
            .addQueryParameter("vs_currencies", s.currency.lowercase())
            .addQueryParameter("include_24hr_change", "true")
            .build()
        return FeedParsers.crypto(get(url), ids, s.currency.lowercase())
    }

    private fun traffic(s: FeedSettings): Map<String, String> {
        // Mornings: the trip to work. From noon: the trip home.
        val toWork = LocalTime.now().hour < 12
        val (from, to) = if (toWork) s.home!! to s.work!! else s.work!! to s.home!!
        val url = HttpUrl.Builder().scheme("https").host("api.tomtom.com")
            .addPathSegments("routing/1/calculateRoute")
            .addPathSegment("${from.lat},${from.lon}:${to.lat},${to.lon}")
            .addPathSegment("json")
            .addQueryParameter("key", s.tomtomKey.trim())
            .addQueryParameter("traffic", "true")
            .addQueryParameter("travelMode", "car")
            .build()
        return FeedParsers.traffic(get(url), toWork, miles = s.fahrenheit)
    }

    private fun news(s: FeedSettings): Map<String, String> {
        val query = s.newsTopic.ifBlank { s.place!!.name.substringBefore(",") }
        return FeedParsers.news(get(newsUrl(query)))
    }

    /** A Google News search feed in the phone's language and country. */
    private fun newsUrl(query: String): HttpUrl {
        val locale = Locale.getDefault()
        val country = locale.country.ifBlank { "US" }
        val language = locale.language.ifBlank { "en" }
        return "https://news.google.com/rss/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query)
            .addQueryParameter("hl", "$language-$country")
            .addQueryParameter("gl", country)
            .addQueryParameter("ceid", "$country:$language")
            .build()
    }

    private fun events(s: FeedSettings): Map<String, String> {
        val place = s.place!!
        if (s.ticketmasterKey.isBlank()) {
            // No account: recent local articles about things to do, from Google News.
            val town = place.name.substringBefore(",")
            val url = newsUrl("(events OR concerts OR festival OR \"things to do\") \"$town\" when:14d")
            return FeedParsers.eventArticles(get(url), LocalDate.now())
        }
        val url = "https://app.ticketmaster.com/discovery/v2/events.json".toHttpUrl().newBuilder()
            .addQueryParameter("apikey", s.ticketmasterKey.trim())
            .addQueryParameter("latlong", "${place.lat},${place.lon}")
            .addQueryParameter("radius", s.eventsRadiusMiles.toString())
            .addQueryParameter("unit", "miles")
            .addQueryParameter("sort", "date,asc")
            .addQueryParameter("size", "5")
            .build()
        return FeedParsers.events(get(url), LocalDate.now())
    }

    /** Whether the user allowed reading the calendar. */
    fun canReadCalendar() =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** The next week's events from the phone's visible calendars. */
    private fun calendar(): Map<String, String> {
        if (!canReadCalendar()) throw IOException("Calendar permission is off. Turn it on in Settings.")
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 24 * 3600_000L)
            ContentUris.appendId(it, now + 7 * 24 * 3600_000L)
        }.build()
        val columns = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.EVENT_ID,
        )
        val local = ZoneId.systemDefault()
        val events = mutableListOf<FeedParsers.CalendarEvent>()
        context.contentResolver.query(
            uri, columns, "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            while (c.moveToNext() && events.size < 50) {
                val allDay = c.getInt(3) == 1
                // All-day events are stored at midnight UTC.
                val zone = if (allDay) ZoneOffset.UTC else local
                fun time(ms: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), zone)
                events += FeedParsers.CalendarEvent(
                    title = c.getString(0).orEmpty(),
                    start = time(c.getLong(1)),
                    end = time(c.getLong(2)),
                    allDay = allDay,
                    location = c.getString(4).orEmpty(),
                    id = c.getLong(5),
                    beginMs = c.getLong(1),
                    endMs = c.getLong(2),
                )
            }
        }
        return FeedParsers.calendar(events.sortedBy { it.start }, LocalDateTime.now())
    }

    private fun get(url: HttpUrl): String {
        val request = Request.Builder().url(url).header("User-Agent", "AIWidgetMaker/0.2 (Android)").build()
        http.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) throw IOException("The API key wasn't accepted (${url.host})")
            if (response.code == 429) throw IOException("Too many requests to ${url.host}; it will retry later")
            if (!response.isSuccessful) throw IOException("${url.host} answered ${response.code}")
            return response.body?.string() ?: throw IOException("${url.host} sent nothing back")
        }
    }

    private companion object {
        val LOCK = Any()
        val http: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        /** Coin symbols looked up this run of the app. */
        val coinIds = mutableMapOf<String, String>()
    }
}
