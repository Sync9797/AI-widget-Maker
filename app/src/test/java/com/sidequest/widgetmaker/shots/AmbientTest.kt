package com.sidequest.widgetmaker.shots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.WidgetTheme
import com.sidequest.widgetmaker.widget.OneUiBackdrop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class AmbientTest {
    /** A fake album cover: two colored halves. */
    private fun cover(a: Int, b: Int): Bitmap {
        val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, 150f, 300f, Paint().apply { color = a })
        c.drawRect(150f, 0f, 300f, 300f, Paint().apply { color = b })
        return bmp
    }

    @Test
    fun frostsTheAlbumArtAndPicksTextToSuitIt() {
        val dark = OneUiBackdrop.renderArt(cover(0xFF3A0CA3.toInt(), 0xFF7209B7.toInt()), 500, 500, 400, 400)
        assertFalse("a dark cover gets light text", dark.light)
        assertEquals(400, dark.bitmap.width)
        val light = OneUiBackdrop.renderArt(cover(0xFFFFE8A3.toInt(), 0xFFFFC4D6.toInt()), 500, 250, 400, 200)
        assertTrue("a light cover gets dark text", light.light)
        // Keep a picture of the dark one to look at.
        File("build/screenshots").mkdirs()
        File("build/screenshots/ambient-sample.png").outputStream().use { dark.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun onlyMusicDesignsOfferAmbient() {
        assertTrue(WidgetTheme.playsMusic(Starters.all.first { it.title == "Now playing" }.design))
        assertTrue(WidgetTheme.playsMusic(Starters.all.first { it.title == "Music mini" }.design))
        assertFalse(WidgetTheme.playsMusic(Starters.all.first { it.title == "Weather forecast" }.design))
        assertFalse(WidgetTheme.ambient in WidgetTheme.everywhere)
    }
}
