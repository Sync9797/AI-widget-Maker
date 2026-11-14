package com.sidequest.widgetmaker

import android.widget.FrameLayout
import android.widget.ViewFlipper
import androidx.test.core.app.ApplicationProvider
import com.sidequest.widgetmaker.widget.Meteocons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The "now and then" icon inflates as a launcher would, and starts on its animation. */
@RunWith(RobolectricTestRunner::class)
class CalmFlipperTest {
    @Test
    fun calmIconStartsPlaying() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val views = Meteocons.flipper(context, "rain", calm = true)!!
        val root = views.apply(context, FrameLayout(context)) as ViewFlipper
        assertEquals(Meteocons.CALM_STILLS + 1, root.childCount)
        assertEquals(Meteocons.CALM_STILLS, root.displayedChild)
        assertTrue(root.getChildAt(Meteocons.CALM_STILLS) is ViewFlipper)
    }
}
