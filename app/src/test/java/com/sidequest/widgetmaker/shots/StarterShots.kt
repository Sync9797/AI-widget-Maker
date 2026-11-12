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
import com.sidequest.widgetmaker.spec.Look
import com.sidequest.widgetmaker.spec.WidgetShape

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
        // A battery level, so battery widgets (and the battery fill) have something to show.
        org.robolectric.Shadows.shadowOf(context.getSystemService(android.os.BatteryManager::class.java))
            .setIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY, 64)
        // Previews refresh on a timer; drive the clock by hand so the test can settle.
        compose.mainClock.autoAdvance = false
        val shots = WidgetTheme.everywhere.flatMap { theme -> Starters.all.mapIndexed { i, s -> Triple(theme, i, s) } }
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

    /** Every shape, and the oval 1-row look, in each theme. */
    @Test
    fun shapes() {
        val out = File("build/screenshots").apply { mkdirs() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "feeds.json").writeText(
            DesignParser.json.encodeToString(FeedSnapshot.serializer(), FeedSnapshot(SAMPLE, System.currentTimeMillis())),
        )
        compose.mainClock.autoAdvance = false
        val clock = Starters.all.first { it.title == "Clock and weather" }.design
        val pill = Starters.all.first { it.title == "Battery" }.design
        var theme by mutableStateOf(WidgetTheme.glass)
        compose.setContent {
            Column(
                Modifier.testTag("shot")
                    .background(Brush.linearGradient(listOf(Color(0xFF4B6CB7), Color(0xFFE1A95F))))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Shapes · ${theme.label}", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WidgetShape.entries.forEach { DesignPreview(clock, PreviewSize.Square, theme, shape = it) }
                }
                Text("Oval 1-row widgets, max height 64 · edge 1.5 · radius 32", color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val look = Look(cornerRadius = 32, edge = 1.5f, pillMaxHeight = 64)
                    DesignPreview(pill, PreviewSize.Strip, theme, look = look)
                    DesignPreview(pill, PreviewSize.Small, theme, look = look)
                    DesignPreview(clock, PreviewSize.Square, theme, look = look)
                }
            }
        }
        WidgetTheme.everywhere.forEach { t ->
            compose.runOnUiThread { theme = t }
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            val view = compose.activity.window.decorView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(full))
            compose.mainClock.advanceTimeBy(500)
            compose.waitForIdle()
            full.eraseColor(0)
            view.draw(Canvas(full))
            val bounds = compose.onNodeWithTag("shot").fetchSemanticsNode().boundsInRoot
            val bitmap = Bitmap.createBitmap(full, 0, 0, bounds.right.toInt().coerceAtMost(full.width), bounds.bottom.toInt().coerceAtMost(full.height))
            File(out, "shapes-${t.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private companion object {
        val SAMPLE = mapOf(
            "weather.temp" to "72°", "weather.feelsLike" to "70°", "weather.high" to "78°", "weather.low" to "55°",
            "weather.rain" to "40%", "weather.wind" to "9 mph", "weather.condition" to "Partly cloudy",
            "weather.icon" to "cloud", "weather.place" to "Austin",
            "weather.h1.time24" to "15:00", "weather.h1.time" to "3 PM", "weather.h1.temp" to "73°", "weather.h1.rain" to "10%", "weather.h1.icon" to "sun",
            "weather.h2.time24" to "16:00", "weather.h2.time" to "4 PM", "weather.h2.temp" to "74°", "weather.h2.rain" to "20%", "weather.h2.icon" to "cloud",
            "weather.h3.time24" to "17:00", "weather.h3.time" to "5 PM", "weather.h3.temp" to "71°", "weather.h3.rain" to "60%", "weather.h3.icon" to "rain",
            "weather.h4.time24" to "18:00", "weather.h4.time" to "6 PM", "weather.h4.temp" to "68°", "weather.h4.rain" to "80%", "weather.h4.icon" to "rain",
            "weather.h5.time24" to "19:00", "weather.h5.time" to "7 PM", "weather.h5.temp" to "64°", "weather.h5.rain" to "30%", "weather.h5.icon" to "moon",
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
            "health.steps" to "3,508", "health.stepsPct" to "44", "health.stepGoal" to "8,000", "health.kcal" to "412",
            "health.heart" to "72", "health.effort" to "8.4", "health.workout" to "Run", "health.workout.duration" to "32 min",
            "health.workout.when" to "Today 7 AM", "health.sleep" to "7h 12m", "health.sleep.deep" to "1h 24m",
            "health.hrv" to "64 ms", "health.restingHeart" to "52", "health.spo2" to "97%", "health.respRate" to "14.8",
            "health.vo2max" to "48", "health.charge" to "78", "health.charge.label" to "Charged", "health.source" to "NOOP",
            "weather.outlook" to "Rain possible tonight",
            "health.stepsLeft" to "4,492", "health.stepsLeft.text" to "4,492 steps to go", "health.sleep.rem" to "1h 35m",
            "health.sleep.light" to "4h 10m", "health.sleep.awake" to "22m", "health.sleep.avg" to "6h 54m",
            "health.sleep.vsAvg" to "18m more than usual", "health.sleep.stages" to "A10,L40,D35,L50,R25,L45,D30,R30,A6,L60,R40,L30",
            "health.sleep.n1.day" to "Fri", "health.sleep.n1" to "6h 40m", "health.sleep.n1.pct" to "74", "health.sleep.n2.day" to "Sat", "health.sleep.n2" to "8h 5m", "health.sleep.n2.pct" to "90", "health.sleep.n3.day" to "Sun", "health.sleep.n3" to "7h 30m", "health.sleep.n3.pct" to "83", "health.sleep.n4.day" to "Mon", "health.sleep.n4" to "6h 10m", "health.sleep.n4.pct" to "69", "health.sleep.n5.day" to "Tue", "health.sleep.n5" to "7h 0m", "health.sleep.n5.pct" to "78", "health.sleep.n6.day" to "Wed", "health.sleep.n6" to "6h 54m", "health.sleep.n6.pct" to "77", "health.sleep.n7.day" to "Thu", "health.sleep.n7" to "7h 12m", "health.sleep.n7.pct" to "80", "health.sleep.bed" to "11:42 PM", "health.sleep.wake" to "7:05 AM",
            "health.heart.series" to "67,70,72,68,67,70,71,75,78,73,76,70,71,69,71,68,65,70,71,73,75,76,76,80,76,73,77,73,75,75,80,138,122,131,140,125,128,133,133,138,144,144,144,146,146,146,146,142,141,136,130,126,127,124,122,126,126,132,136,134,134,136,136,139,138,140,143,143,146,143,142,146,140,138,141,145,146,142,146,145,146,146,146,141,146,146,143,146,146,144,142,137,132,133,137,138", "health.heart.last" to "76", "health.heartMin" to "58",
            "health.heartMid" to "102", "health.heartMax" to "146", "health.heart.start" to "7:55 AM", "health.heart.mid" to "9:24 AM",
            "health.heart.end" to "10:54 AM", "health.effort.pct" to "45", "health.rest" to "91",
            "calendar.tomorrow" to "Team standup", "calendar.tomorrow.when" to "9 AM",
            "countdown.1" to "Beach trip", "countdown.1.days" to "12", "countdown.1.unit" to "days", "countdown.1.text" to "12 days",
            "countdown.1.date" to "Nov 24", "countdown.2" to "Christmas", "countdown.2.text" to "43 days",
            "countdown.3" to "New Year", "countdown.3.text" to "50 days",
            "mq.hero" to "Aaron", "mq.level" to "23", "mq.steps" to "3,508", "mq.torches" to "4", "mq.sleep" to "7h 12m",
            "mq.eggs" to "3", "mq.eggs.ready" to "1",
            "mq.egg.1" to "Ember Pup", "mq.egg.1.progress" to "100", "mq.egg.1.left" to "Ready to hatch!",
            "mq.egg.2" to "Tide Sprite", "mq.egg.2.progress" to "62", "mq.egg.2.left" to "1.2 km to go",
            "mq.egg.3" to "Gale Finch", "mq.egg.3.progress" to "15", "mq.egg.3.left" to "4.3 km to go",
            "mq.adventures" to "2",
            "mq.adv.1" to "Clear the cellar", "mq.adv.1.team" to "Ember, Pip", "mq.adv.1.left" to "1 h 20 min left", "mq.adv.1.progress" to "40",
            "mq.adv.2" to "Scout the river", "mq.adv.2.team" to "Tide", "mq.adv.2.left" to "Done! Collect your loot", "mq.adv.2.progress" to "100",
            "mq.egg.1.hatch" to "mq-action:hatch:e1", "mq.adv.2.claim" to "mq-action:claim:a2",
            "mq.quests" to "6", "mq.quest.1" to "Write report", "mq.quest.1.when" to "Today 3 PM", "mq.quest.1.done" to "mq-action:complete:q1",
            "mq.quest.2" to "Water plants", "mq.quest.2.done" to "mq-action:complete:q2",
            "mq.quest.3" to "Call the bank", "mq.quest.3.when" to "Tomorrow", "mq.quest.3.done" to "mq-action:complete:q3",
            "mq.party.1" to "Ember", "mq.party.1.level" to "Lv 24", "mq.party.1.hp" to "52/60", "mq.party.1.hpPct" to "86",
            "mq.party.2" to "Pip", "mq.party.2.level" to "Lv 21", "mq.party.2.hp" to "18/55", "mq.party.2.hpPct" to "33",
            "mq.bounty.1" to "Walk 40,000 steps", "mq.bounty.1.progress" to "12,400 / 40,000", "mq.bounty.1.pct" to "31",
            "mq.bounty.2" to "Two 60-minute phone-free sessions", "mq.bounty.2.progress" to "1 / 2", "mq.bounty.2.pct" to "50",
            "mq.bounty.3" to "Complete 10 quests", "mq.bounty.3.progress" to "7 / 10", "mq.bounty.3.pct" to "70",
            "mq.torch.pct" to "48", "mq.torch.next" to "520 steps to the next torch",
            "mq.focus" to "Phone-free · 23 min left", "mq.focus.pct" to "62",
            "updated" to "3:12 PM",
        )
    }
}
