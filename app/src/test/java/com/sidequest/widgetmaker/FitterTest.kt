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
        val tight = Fitter.fit(column, 200f, 100f, 1f, tokens, shows = { true })
        assertTrue(tight.k < 1f)
        assertTrue(tight.k >= Fitter.MIN_SHRINK)
        assertEquals(1f, Fitter.fit(column, 200f, 400f, 1f, tokens, shows = { true }).k)
    }

    @Test
    fun textTooWideShrinksToFitItsLine() {
        val price = TextNode("$0.9123456", size = 44, maxLines = 1)
        val fitted = Fitter.fit(ColumnNode(children = listOf(price)), 130f, 200f, 1f, tokens, shows = { true }).root as ColumnNode
        val size = (fitted.children[0] as TextNode).size
        assertTrue("shrunk from 44 to $size", size < 44)
        assertTrue(size >= (44 * Fitter.MIN_TEXT).toInt())
        // And the home screen shrinks it further if it must, so it's never cut short.
        assertTrue((fitted.children[0] as TextNode).autoFit)
        // Text that fits keeps its size.
        val short = Fitter.fit(ColumnNode(children = listOf(TextNode("$1", size = 44, maxLines = 1))), 130f, 200f, 1f, tokens, shows = { true }).root as ColumnNode
        assertEquals(44, (short.children[0] as TextNode).size)
    }

    @Test
    fun crowdedRowsCutTheirLongestText() {
        val row = RowNode(children = listOf(TextNode("Galaxy Watch Ultra Classic"), TextNode("100%")))
        val fitted = Fitter.fit(row, 100f, 40f, 1f, tokens, shows = { true }).root as RowNode
        val name = fitted.children[0] as TextNode
        assertTrue("the long name gives way", name.grow)
        assertEquals(1, name.maxLines)
        assertFalse("the percent keeps its size", fitted.children[1].grow)
        // A row with room is left alone.
        assertEquals(row, Fitter.fit(row, 400f, 40f, 1f, tokens, shows = { true }).root)
    }

    @Test
    fun shortcutsOpenTheirAppAndPillsParse() {
        assertEquals(com.sidequest.widgetmaker.spec.TapAction.App("com.anthropic.claude"), com.sidequest.widgetmaker.spec.Taps.resolve("app:com.anthropic.claude", tokens))
        assertEquals(null, com.sidequest.widgetmaker.spec.Taps.resolve("app:bad package!", tokens))
        val claude = com.sidequest.widgetmaker.spec.Starters.all.first { it.title == "Claude" }.design
        assertEquals("app:com.anthropic.claude", com.sidequest.widgetmaker.spec.Taps.forWidget(claude))
        val pill = com.sidequest.widgetmaker.spec.DesignParser.parse("""{"name":"P","root":{"type":"progress","height":34,"text":"{battery}%"}}""").root as com.sidequest.widgetmaker.spec.ProgressNode
        assertTrue(pill.pill)
        assertEquals(34, pill.height)
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

    @Test
    fun scrollingListsShowOnlyWholeRows() {
        // Five 30 dp rows in a 100 dp list: the ones that fit whole share out the room left below them,
        // and the first that doesn't starts past the bottom edge instead of showing cut off.
        val rows = List(5) { ColumnNode(children = listOf(com.sidequest.widgetmaker.spec.SpacerNode(size = 30))) }
        val list = ColumnNode(children = rows, scroll = true, grow = true)
        val fitted = Fitter.fit(ColumnNode(children = listOf(list)), 200f, 100f, 1f, tokens, shows = { true }).root as ColumnNode
        val spaced = fitted.children[0] as ColumnNode
        var top = 0
        val tops = mutableListOf<Int>()
        for (c in spaced.children) {
            if (c is ColumnNode) { tops += top; top += 30 } else top += (c as com.sidequest.widgetmaker.spec.SpacerNode).size
        }
        val whole = tops.count { it + 30 <= 100 }
        assertTrue("at least one row shows whole", whole >= 1)
        // The first row that doesn't fit starts past the edge, with room to spare: it never peeks in.
        assertTrue("row ${whole + 1} starts at ${tops[whole]}", tops[whole] >= 100 + Fitter.HIDE_MARGIN / 2)
        // And the whole rows reach down close to the edge (the gaps opened up), not bunched at the top.
        assertTrue(tops[whole - 1] + 30 >= 100 - 30)
        // A list that fits entirely is left as it is.
        val short = Fitter.fit(ColumnNode(children = listOf(ColumnNode(children = rows.take(2), scroll = true, grow = true))), 200f, 100f, 1f, tokens, shows = { true }).root as ColumnNode
        assertEquals(2, (short.children[0] as ColumnNode).children.size)
    }
}
