package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.DevicesMath
import com.sidequest.widgetmaker.spec.ColumnNode
import com.sidequest.widgetmaker.spec.Fitter
import com.sidequest.widgetmaker.spec.RowNode
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class FitterTest {
    private val tokens = Tokens(LocalDateTime.of(2026, 11, 14, 9, 0), 64, false)

    @Test
    fun tooTallShrinksTooShortStays() {
        val column = ColumnNode(children = List(6) { TextNode("Line $it", size = 20, maxLines = 1) })
        // Six 20 sp lines need about 142 dp: in 100 dp it shrinks, but no further than the floor.
        val tight = Fitter.fit(column, 200f, 100f, 1f, tokens) { true }
        assertTrue(tight.k < 1f)
        assertTrue(tight.k >= Fitter.MIN_SHRINK)
        assertEquals(1f, Fitter.fit(column, 200f, 400f, 1f, tokens) { true }.k)
    }

    @Test
    fun crowdedRowsCutTheirLongestText() {
        val row = RowNode(children = listOf(TextNode("Galaxy Watch Ultra Classic"), TextNode("100%")))
        val fitted = Fitter.fit(row, 100f, 40f, 1f, tokens) { true }.root as RowNode
        val name = fitted.children[0] as TextNode
        assertTrue("the long name gives way", name.grow)
        assertEquals(1, name.maxLines)
        assertFalse("the percent keeps its size", fitted.children[1].grow)
        // A row with room is left alone.
        assertEquals(row, Fitter.fit(row, 400f, 40f, 1f, tokens) { true }.root)
    }

    @Test
    fun everyConnectedDeviceIsListedAndWidgetsCanHideSome() {
        val values = DevicesMath.values(
            listOf(
                DevicesMath.Device("Phone", 92, DevicesMath.Kind.phone),
                DevicesMath.Device("Car kit", -1, DevicesMath.Kind.other),
                DevicesMath.Device("Buds", 40, DevicesMath.Kind.buds),
            ),
        )
        assertEquals("3", values["devices.count"])
        assertEquals("Car kit", values["devices.3.name"]) // No battery reading: listed last.
        assertEquals("--", values["devices.3.pct"])
        assertEquals("", values["devices.3.level"])
        val hidden = DevicesMath.hide(values + ("weather.temp" to "72°"), setOf("Buds"))
        assertEquals("2", hidden["devices.count"])
        assertEquals("Car kit", hidden["devices.2.name"])
        assertEquals(null, hidden["devices.3.name"])
        assertEquals("72°", hidden["weather.temp"])
    }
}
