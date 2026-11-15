package com.sidequest.widgetmaker

import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The shrink-to-fit text layouts inflate as a launcher would, autosizing on, one line. */
@RunWith(RobolectricTestRunner::class)
class FitTextLayoutTest {
    @Test
    fun analogClockInflatesAsALauncherWould() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val view = RemoteViews(context.packageName, R.layout.analog_clock).apply(context, FrameLayout(context))
        assertTrue(view is android.widget.AnalogClock)
    }

    @Test
    fun everyFitLayoutAutosizes() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (w in listOf("n", "m", "b")) for (size in listOf(10, 44, 96)) {
            val id = context.resources.getIdentifier("fit_text_${w}_$size", "layout", context.packageName)
            assertTrue("fit_text_${w}_$size", id != 0)
            val views = RemoteViews(context.packageName, id).apply { setTextViewText(R.id.fit_text, "$0.931234") }
            val text = views.apply(context, FrameLayout(context)) as TextView
            assertEquals(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM, text.autoSizeTextType)
            assertEquals(1, text.maxLines)
            assertEquals("$0.931234", text.text.toString())
        }
    }
}
