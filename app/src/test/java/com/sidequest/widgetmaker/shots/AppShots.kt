package com.sidequest.widgetmaker.shots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.feeds.FeedSnapshot
import com.sidequest.widgetmaker.ui.MakerScreen
import com.sidequest.widgetmaker.ui.MakerTheme
import com.sidequest.widgetmaker.ui.MakerViewModel
import com.sidequest.widgetmaker.ui.Page
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws each section of the app on a phone and on a tablet, into build/screenshots, for looking at by eye. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AppShots {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test @Config(qualifiers = "w411dp-h891dp-night-xhdpi")
    fun phone() = shots("phone")

    @Test @Config(qualifiers = "w1100dp-h820dp-night-xhdpi")
    fun tablet() = shots("tablet")

    private fun shots(name: String) {
        if (System.getProperty("shots.app") == null) return
        val out = File("build/screenshots").apply { mkdirs() }
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "feeds.json").writeText(
            DesignParser.json.encodeToString(FeedSnapshot.serializer(), FeedSnapshot(StarterShots.SAMPLE, System.currentTimeMillis())),
        )
        val vm = MakerViewModel(context as android.app.Application)
        compose.mainClock.autoAdvance = false
        var page by mutableStateOf(Page.Create)
        compose.setContent { MakerTheme { key(page) { MakerScreen(vm, start = page) } } }
        Page.entries.forEach { p ->
            compose.runOnUiThread { page = p }
            repeat(3) {
                compose.mainClock.advanceTimeBy(500)
                compose.waitForIdle()
                Thread.sleep(150)
            }
            val view = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bmp))
            File(out, "app-$name-${p.name}.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
