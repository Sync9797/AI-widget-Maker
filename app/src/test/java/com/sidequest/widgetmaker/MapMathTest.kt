package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.MapMath
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.MapNode
import com.sidequest.widgetmaker.spec.TapKind
import com.sidequest.widgetmaker.spec.Taps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapMathTest {
    @Test
    fun worldPixels() {
        // The middle of the world map, and a known tile: San Francisco at zoom 14 is tile x 2620, y 6332.
        val (x0, y0) = MapMath.pixel(0.0, 0.0, 1)
        assertEquals(256.0, x0, 1e-6)
        assertEquals(256.0, y0, 1e-6)
        val (x, y) = MapMath.pixel(37.7749, -122.4194, 14)
        assertEquals(2620, (x / 256).toInt())
        assertEquals(6332, (y / 256).toInt())
    }

    @Test
    fun tilesCoverThePicture() {
        val (cx, cy) = MapMath.pixel(37.7749, -122.4194, 14)
        val tiles = MapMath.tiles(cx, cy, 960, 600, 14)
        // Every pixel of the picture is under some tile.
        for (px in listOf(0, 479, 959)) for (py in listOf(0, 299, 599)) {
            assertTrue("$px,$py", tiles.any { px in it.left until it.left + 256 && py in it.top until it.top + 256 })
        }
        assertTrue(tiles.size in 12..20)
        // The tile holding the middle sits where the center pixel falls.
        val middle = tiles.single { 480 in it.left until it.left + 256 && 300 in it.top until it.top + 256 }
        assertEquals((cx / 256).toInt(), middle.x)
    }

    @Test
    fun mapElementParsesAndOpensMaps() {
        val design = DesignParser.parse("""{"name":"T","root":{"type":"map","height":9999,"grow":true}}""")
        assertEquals(400, (design.root as MapNode).height)
        assertEquals(TapKind.commute, Taps.mainKind(design))
    }
}
