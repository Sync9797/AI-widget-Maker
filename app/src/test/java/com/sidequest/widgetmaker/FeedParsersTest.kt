package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.FeedParsers
import com.sidequest.widgetmaker.feeds.Place
import com.sidequest.widgetmaker.spec.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

/** Parses responses shaped like each service's real ones. */
class FeedParsersTest {
    private val austin = Place("Austin, Texas", 30.27, -97.74)

    @Test
    fun weather() {
        val body = """{"current":{"temperature_2m":71.6,"apparent_temperature":70.1,"weather_code":2,"is_day":1,
            "wind_speed_10m":8.7},"daily":{"temperature_2m_max":[78.2],"temperature_2m_min":[55.4],
            "precipitation_probability_max":[40]}}"""
        val v = FeedParsers.weather(body, austin, fahrenheit = true)
        assertEquals("72°", v["weather.temp"])
        assertEquals("78°", v["weather.high"])
        assertEquals("55°", v["weather.low"])
        assertEquals("40%", v["weather.rain"])
        assertEquals("9 mph", v["weather.wind"])
        assertEquals("Partly cloudy", v["weather.condition"])
        assertEquals("cloud", v["weather.icon"])
        assertEquals("Austin", v["weather.place"])
    }

    @Test
    fun forecast() {
        val body = """{"current":{"temperature_2m":60,"weather_code":0,"is_day":1},
            "hourly":{"time":["2026-11-11T14:00","2026-11-11T15:00","2026-11-11T16:00","2026-11-11T17:00",
              "2026-11-11T18:00","2026-11-11T19:00","2026-11-11T20:00"],
              "temperature_2m":[60,61.4,62,59,55,52,50],"weather_code":[0,0,3,61,61,0,0],"is_day":[1,1,1,1,0,0,0]},
            "daily":{"time":["2026-11-11","2026-11-12","2026-11-13","2026-11-14"],
              "temperature_2m_max":[70,72.6,65,80],"temperature_2m_min":[50,51,48,60],
              "precipitation_probability_max":[10,20,80,0],"weather_code":[0,3,63,95]}}"""
        val v = FeedParsers.weather(body, austin, fahrenheit = true)
        assertEquals("3 PM", v["weather.h1.time"])
        assertEquals("61°", v["weather.h1.temp"])
        assertEquals("sun", v["weather.h1.icon"])
        assertEquals("rain", v["weather.h3.icon"])
        assertEquals("moon", v["weather.h5.icon"])
        assertEquals("Thu", v["weather.d1.day"])
        assertEquals("73°", v["weather.d1.high"])
        assertEquals("rain", v["weather.d2.icon"])
        assertEquals("storm", v["weather.d3.icon"])
    }

    @Test
    fun liveIconsFollowTheirValue() {
        val tokens = Tokens(LocalDateTime.now(), null, false, feeds = mapOf("weather.icon" to "snow", "weather.h2.icon" to "rain"))
        val live = com.sidequest.widgetmaker.spec.IconNode(name = com.sidequest.widgetmaker.spec.IconName.sun, live = "{weather.h2.icon}")
        val current = com.sidequest.widgetmaker.spec.IconNode(name = com.sidequest.widgetmaker.spec.IconName.weather)
        val plain = com.sidequest.widgetmaker.spec.IconNode(name = com.sidequest.widgetmaker.spec.IconName.heart)
        val missing = com.sidequest.widgetmaker.spec.IconNode(name = com.sidequest.widgetmaker.spec.IconName.sun, live = "{weather.h9.icon}")
        assertEquals(com.sidequest.widgetmaker.R.drawable.icon_rain, com.sidequest.widgetmaker.widget.iconRes(live, tokens))
        assertEquals(com.sidequest.widgetmaker.R.drawable.icon_snow, com.sidequest.widgetmaker.widget.iconRes(current, tokens))
        assertEquals(com.sidequest.widgetmaker.R.drawable.icon_heart, com.sidequest.widgetmaker.widget.iconRes(plain, tokens))
        // No value yet: falls back to the current weather.
        assertEquals(com.sidequest.widgetmaker.R.drawable.icon_snow, com.sidequest.widgetmaker.widget.iconRes(missing, tokens))
    }

    @Test
    fun weatherIconsFollowDayAndNight() {
        assertEquals("sun", FeedParsers.weatherIcon(0, isDay = true))
        assertEquals("moon", FeedParsers.weatherIcon(0, isDay = false))
        assertEquals("rain", FeedParsers.weatherIcon(63, isDay = true))
        assertEquals("snow", FeedParsers.weatherIcon(73, isDay = true))
        assertEquals("storm", FeedParsers.weatherIcon(95, isDay = true))
    }

    @Test
    fun town() {
        val body = """{"results":[{"name":"Austin","latitude":30.26715,"longitude":-97.74306,"admin1":"Texas","country":"United States"}]}"""
        assertEquals(Place("Austin, Texas", 30.26715, -97.74306), FeedParsers.place(body))
        assertNull(FeedParsers.place("""{"generationtime_ms":0.5}"""))
    }

    @Test
    fun crypto() {
        val body = """{"bitcoin":{"usd":97412.33,"usd_24h_change":2.4417},"ethereum":{"usd":3410.5,"usd_24h_change":-1.06}}"""
        val v = FeedParsers.crypto(body, mapOf("BTC" to "bitcoin", "ETH" to "ethereum"), "usd")
        assertEquals("$97,412", v["crypto.BTC.price"])
        assertEquals("+2.4%", v["crypto.BTC.change"])
        assertEquals("$3,411", v["crypto.ETH.price"])
        assertEquals("-1.1%", v["crypto.ETH.change"])
        assertEquals("$0.1234", FeedParsers.money(0.12341, "usd"))
        assertEquals("€12.50", FeedParsers.money(12.5, "eur"))
    }

    @Test
    fun coinSearchMatchesTheExactSymbol() {
        val body = """{"coins":[{"id":"pepe-unchained","symbol":"PEPU"},{"id":"pepe","symbol":"PEPE"}]}"""
        assertEquals("pepe", FeedParsers.coinSearch(body, "pepe"))
        assertNull(FeedParsers.coinSearch(body, "NOPE"))
    }

    @Test
    fun traffic() {
        val body = """{"routes":[{"summary":{"lengthInMeters":19800,"travelTimeInSeconds":1460,"trafficDelayInSeconds":380}}]}"""
        val v = FeedParsers.traffic(body, toWork = true, miles = true)
        assertEquals("24 min", v["traffic.commute"])
        assertEquals("+6 min", v["traffic.delay"])
        assertEquals("12.3 mi", v["traffic.distance"])
        assertEquals("Work", v["traffic.to"])
        assertEquals("1 h 15 min", FeedParsers.duration(4500))
    }

    @Test
    fun tomtomAddress() {
        val body = """{"results":[{"address":{"freeformAddress":"500 Congress Ave, Austin, TX 78701"},"position":{"lat":30.2669,"lon":-97.7429}}]}"""
        assertEquals(Place("500 Congress Ave, Austin, TX 78701", 30.2669, -97.7429), FeedParsers.tomtomPlace(body))
    }

    @Test
    fun news() {
        val body = """<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel><title>Austin - Google News</title>
            <item><title>City council approves new park - KXAN</title><source url="https://kxan.com">KXAN</source></item>
            <item><title>Food truck festival returns &amp; grows - Austin Chronicle</title><source url="https://x">Austin Chronicle</source></item>
            </channel></rss>"""
        val v = FeedParsers.news(body)
        assertEquals("City council approves new park", v["news.1"])
        assertEquals("KXAN", v["news.1.source"])
        assertEquals("Food truck festival returns & grows", v["news.2"])
        assertNull(v["news.3"])
    }

    @Test
    fun eventsFromLocalNews() {
        val body = """<?xml version="1.0" encoding="UTF-8"?><rss version="2.0"><channel>
            <item><title>10 things to do in Austin this weekend - Austin Chronicle</title>
              <pubDate>Wed, 11 Nov 2026 14:00:00 GMT</pubDate><source url="https://x">Austin Chronicle</source></item>
            <item><title>Old festival recap - KXAN</title>
              <pubDate>Mon, 05 Oct 2026 10:00:00 GMT</pubDate><source url="https://y">KXAN</source></item>
            <item><title>Holiday market opens downtown - KVUE</title>
              <pubDate>Mon, 09 Nov 2026 10:00:00 GMT</pubDate><source url="https://z">KVUE</source></item>
            </channel></rss>"""
        val v = FeedParsers.eventArticles(body, today = LocalDate.of(2026, 11, 11))
        assertEquals("10 things to do in Austin this weekend", v["events.1"])
        assertEquals("Today", v["events.1.when"])
        assertEquals("Austin Chronicle", v["events.1.venue"])
        // The month-old article is skipped.
        assertEquals("Holiday market opens downtown", v["events.2"])
        assertEquals("Mon", v["events.2.when"])
        assertNull(v["events.3"])
    }

    @Test
    fun events() {
        val body = """{"_embedded":{"events":[
            {"name":"Austin FC vs. LA Galaxy","dates":{"start":{"localDate":"2026-11-14","localTime":"19:30:00"}},
             "_embedded":{"venues":[{"name":"Q2 Stadium"}]}},
            {"name":"Jazz Night","dates":{"start":{"localDate":"2026-11-11","localTime":"20:00:00"}},
             "_embedded":{"venues":[{"name":"Elephant Room"}]}}]}}"""
        val v = FeedParsers.events(body, today = LocalDate.of(2026, 11, 11))
        assertEquals("Austin FC vs. LA Galaxy", v["events.1"])
        assertEquals("Sat 7:30 PM", v["events.1.when"])
        assertEquals("Q2 Stadium", v["events.1.venue"])
        assertEquals("Today 8 PM", v["events.2.when"])
        assertEquals(emptyMap<String, String>(), FeedParsers.events("""{"page":{"totalElements":0}}""", LocalDate.now()))
    }

    @Test
    fun whenTextFarAway() {
        assertEquals("Dec 25 6 PM", FeedParsers.whenText(LocalDate.of(2026, 12, 25), LocalTime.of(18, 0), LocalDate.of(2026, 11, 11)))
    }

    @Test
    fun feedTokensFillAndTrend() {
        val tokens = Tokens(
            LocalDateTime.of(2026, 3, 4, 8, 15), battery = 50, charging = false, locale = Locale.US,
            feeds = mapOf("crypto.BTC.change" to "+2.4%", "crypto.ETH.change" to "-1.1%", "weather.temp" to "72°"),
        )
        assertEquals("72° · -- · {other.thing}", tokens.fill("{weather.temp} · {traffic.commute} · {other.thing}"))
        assertEquals(true, tokens.trend("{crypto.BTC.change}"))
        assertEquals(false, tokens.trend("{crypto.ETH.change}"))
        assertEquals(null, tokens.trend("{crypto.SOL.change}"))
    }
}
