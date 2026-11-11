package com.sidequest.widgetmaker.shots

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.graphics.Canvas
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.ui.DesignPreview
import com.sidequest.widgetmaker.ui.PreviewSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sidequest.widgetmaker.feeds.FeedSnapshot
import com.sidequest.widgetmaker.spec.DesignParser

/** Draws every starter at every size and theme into build/screenshots, for looking at by eye. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1400dp-h1200dp-xhdpi")
class StarterShots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun render() {
        val out = File("build/screenshots").apply { mkdirs() }
        // Realistic sample data, as if every data source were set up.
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "feeds.json").writeText(
            DesignParser.json.encodeToString(FeedSnapshot.serializer(), FeedSnapshot(SAMPLE, System.currentTimeMillis())),
        )
        // Previews refresh on a timer; drive the clock by hand so the test can settle.
        compose.mainClock.autoAdvance = false
        val shots = WidgetTheme.entries.flatMap { theme -> Starters.all.mapIndexed { i, s -> Triple(theme, i, s) } }
        var current by mutableStateOf(shots.first())
        compose.setContent {
            val (theme, _, starter) = current
            // A wallpaper-like backdrop, so glass reads as it would on a home screen.
            Column(
                Modifier.testTag("shot")
                    .background(Brush.linearGradient(listOf(Color(0xFF4B6CB7), Color(0xFFE1A95F))))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("${starter.title} · ${theme.label}", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    PreviewSize.entries.forEach { DesignPreview(starter.design, it, theme) }
                }
            }
        }
        shots.forEach { shot ->
            compose.runOnUiThread { current = shot }
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            val (theme, i, starter) = shot
            compose.waitForIdle()
            val view = compose.activity.window.decorView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            // The first draw can still show the previous shot; settle once more, then draw for real.
            view.draw(Canvas(full))
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            full.eraseColor(0)
            view.draw(Canvas(full))
            val bounds = compose.onNodeWithTag("shot").fetchSemanticsNode().boundsInRoot
            val bitmap = Bitmap.createBitmap(full, 0, 0, bounds.right.toInt().coerceAtMost(full.width), bounds.bottom.toInt().coerceAtMost(full.height))
            File(out, "%s-%d-%s.png".format(theme.name, i + 1, starter.title.replace(' ', '_'))).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private companion object {
        val SAMPLE = mapOf(
            "weather.temp" to "72°", "weather.feelsLike" to "70°", "weather.high" to "78°", "weather.low" to "55°",
            "weather.rain" to "40%", "weather.wind" to "9 mph", "weather.condition" to "Partly cloudy",
            "weather.icon" to "cloud", "weather.place" to "Austin",
            "weather.h1.time" to "3 PM", "weather.h1.temp" to "73°", "weather.h1.icon" to "sun",
            "weather.h2.time" to "4 PM", "weather.h2.temp" to "74°", "weather.h2.icon" to "cloud",
            "weather.h3.time" to "5 PM", "weather.h3.temp" to "71°", "weather.h3.icon" to "rain",
            "weather.h4.time" to "6 PM", "weather.h4.temp" to "68°", "weather.h4.icon" to "rain",
            "weather.h5.time" to "7 PM", "weather.h5.temp" to "64°", "weather.h5.icon" to "moon",
            "weather.d1.day" to "Thu", "weather.d1.high" to "75°", "weather.d1.low" to "52°", "weather.d1.icon" to "sun",
            "weather.d2.day" to "Fri", "weather.d2.high" to "68°", "weather.d2.low" to "49°", "weather.d2.icon" to "storm",
            "weather.d3.day" to "Sat", "weather.d3.high" to "70°", "weather.d3.low" to "50°", "weather.d3.icon" to "cloud",
            "crypto.BTC.price" to "$97,412", "crypto.BTC.change" to "+2.4%",
            "crypto.ETH.price" to "$3,411", "crypto.ETH.change" to "-1.1%",
            "crypto.SOL.price" to "$212.40", "crypto.SOL.change" to "+5.8%",
            "crypto.XRP.price" to "$2.31", "crypto.XRP.change" to "-0.4%",
            "traffic.commute" to "24 min", "traffic.delay" to "+6 min", "traffic.distance" to "12.3 mi", "traffic.to" to "Work",
            "news.1" to "City council approves new downtown park after years of debate", "news.1.source" to "KXAN",
            "news.2" to "Food truck festival returns to Zilker this weekend", "news.2.source" to "Austin Chronicle",
            "news.3" to "New rail line opening pushed to spring", "news.3.source" to "KVUE",
            "news.4" to "Local high school wins state robotics title", "news.4.source" to "Community Impact",
            "events.1" to "Austin FC vs. LA Galaxy", "events.1.when" to "Sat 7:30 PM", "events.1.venue" to "Q2 Stadium",
            "events.2" to "Jazz Night", "events.2.when" to "Today 8 PM", "events.2.venue" to "Elephant Room",
            "events.3" to "Comedy Showcase", "events.3.when" to "Fri 9 PM", "events.3.venue" to "Cap City Comedy",
            "events.4" to "Indie Rock Live", "events.4.when" to "Sun 7 PM", "events.4.venue" to "Mohawk",
            "calendar.1" to "Dentist", "calendar.1.when" to "Today 3 PM", "calendar.1.where" to "Smile Dental, 5th St",
            "calendar.2" to "Team standup", "calendar.2.when" to "Tomorrow 9 AM",
            "calendar.3" to "Mom's birthday", "calendar.3.when" to "Fri",
            "calendar.4" to "Soccer practice", "calendar.4.when" to "Sat 10 AM", "calendar.today" to "1 more today",
            "updated" to "3:12 PM",
        )
    }
}
