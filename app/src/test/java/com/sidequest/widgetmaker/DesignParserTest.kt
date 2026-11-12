package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.ai.Prompts
import com.sidequest.widgetmaker.spec.ClockNode
import com.sidequest.widgetmaker.spec.ColumnNode
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.IconName
import com.sidequest.widgetmaker.spec.IconNode
import com.sidequest.widgetmaker.spec.RowNode
import com.sidequest.widgetmaker.spec.TextNode
import com.sidequest.widgetmaker.spec.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import com.sidequest.widgetmaker.spec.MaterialSlot
import com.sidequest.widgetmaker.spec.Role
import java.time.LocalDateTime
import java.util.Locale

class DesignParserTest {
    @Test
    fun parsesThePromptsOwnExample() {
        val example = Prompts.SYSTEM.substringAfter("Example (a calm clock widget")
        val design = DesignParser.parse(example)
        assertEquals("Calm clock", design.name)
        val root = design.root as ColumnNode
        assertTrue(root.children[1] is ClockNode)
        val row = root.children[2] as RowNode
        assertEquals(IconName.battery, (row.children[1] as IconNode).name)
    }

    @Test
    fun theExampleShowsMoreAsItGrows() {
        val design = DesignParser.parse(Prompts.SYSTEM.substringAfter("Example (a calm clock widget"))
        fun count(node: com.sidequest.widgetmaker.spec.Node, w: Int, h: Int): Int {
            if (!com.sidequest.widgetmaker.spec.visible(node, w, h)) return 0
            val kids = when (node) {
                is ColumnNode -> node.children
                is RowNode -> node.children
                else -> emptyList()
            }
            return 1 + kids.sumOf { count(it, w, h) }
        }
        val small = count(design.root, 160, 80)
        val wide = count(design.root, 320, 80)
        val square = count(design.root, 160, 160)
        val large = count(design.root, 320, 320)
        assertTrue(small < wide)
        assertTrue(small < square)
        assertTrue(square < large)
    }

    @Test
    fun showLimits() {
        val show = com.sidequest.widgetmaker.spec.Show(minWidth = 200, maxHeight = 150)
        assertTrue(show.fits(250, 100))
        assertTrue(!show.fits(150, 100))
        assertTrue(!show.fits(250, 200))
        val parsed = DesignParser.parse("""{"root":{"type":"text","text":"x","show":{"minWidth":5000}}}""")
        assertEquals(1000, parsed.root.show!!.minWidth)
    }

    @Test
    fun findsJsonInsideAFenceAndText() {
        val reply = "Here you go:\n```json\n{\"root\":{\"type\":\"text\",\"text\":\"Hi\"}}\n```"
        val design = DesignParser.parse(reply)
        assertEquals("Hi", (design.root as TextNode).text)
    }

    @Test
    fun clampsSizesAndFixesBadColors() {
        val design = DesignParser.parse(
            """{"padding":500,"background":"blue","root":{"type":"text","text":"x","size":999,"color":"#12"}}""",
        )
        assertEquals(32, design.padding)
        assertEquals("#E61E1E2E", design.background)
        val text = design.root as TextNode
        assertEquals(72, text.size)
        assertEquals("#FFFFFFFF", text.color)
    }

    @Test
    fun unknownEnumValuesFallBackToDefaults() {
        val design = DesignParser.parse("""{"root":{"type":"icon","name":"rocket"}}""")
        assertEquals(IconName.star, (design.root as IconNode).name)
    }

    @Test
    fun rejectsTooManyElements() {
        val children = (1..DesignParser.MAX_NODES).joinToString(",") { """{"type":"text","text":"$it"}""" }
        assertThrows(DesignParser.InvalidDesign::class.java) {
            DesignParser.parse("""{"root":{"type":"column","children":[$children]}}""")
        }
    }

    @Test
    fun rejectsReplyWithoutJson() {
        assertThrows(DesignParser.InvalidDesign::class.java) { DesignParser.parse("Sorry, no.") }
    }

    @Test
    fun roundTrips() {
        val design = DesignParser.parse(Prompts.SYSTEM.substringAfter("Example (a calm clock widget"))
        assertEquals(design, DesignParser.parse(DesignParser.encode(design)))
    }

    @Test
    fun fillsTokens() {
        val tokens = Tokens(LocalDateTime.of(2026, 3, 4, 8, 15), battery = 76, charging = true, locale = Locale.UK)
        assertEquals("Good morning, Wednesday 76% Charging {nope}", tokens.fill("{greeting}, {day} {battery}% {charging} {nope}"))
        assertEquals(0.76f, tokens.progress("{battery}"), 0.001f)
        assertEquals(1f, tokens.progress("250"), 0.001f)
    }
}

class WidgetThemeTest {
    private val glass = com.sidequest.widgetmaker.spec.Styler(com.sidequest.widgetmaker.spec.WidgetTheme.glass)
    private val material = com.sidequest.widgetmaker.spec.Styler(com.sidequest.widgetmaker.spec.WidgetTheme.material)
    private val original = com.sidequest.widgetmaker.spec.Styler(com.sidequest.widgetmaker.spec.WidgetTheme.original)
    private val baseline = com.sidequest.widgetmaker.spec.Baseline

    @Test
    fun originalKeepsTheDesignsColors() {
        assertEquals(0xFF00FF66.toInt(), original.argb(com.sidequest.widgetmaker.spec.Role.Text, "#FF00FF66", dark = false))
    }

    @Test
    fun glassMakesTextWhiteButKeepsItsFade() {
        val role = com.sidequest.widgetmaker.spec.Role.Text
        assertEquals(0xFFFFFFFF.toInt(), glass.argb(role, "#FF00FF66", dark = false))
        assertEquals(0xB3FFFFFF.toInt(), glass.argb(role, "#B3123456", dark = false))
        // Very faint text is raised so it stays readable on glass.
        assertEquals(0x99FFFFFF.toInt(), glass.argb(role, "#33FFFFFF", dark = false))
        // Accents keep their color.
        assertEquals(0xFF7CF29C.toInt(), glass.argb(com.sidequest.widgetmaker.spec.Role.Accent, "#FF7CF29C", dark = false))
    }

    @Test
    fun materialMapsEveryColorToASystemSlot() {
        assertEquals(MaterialSlot.Surface, material.materialSlot(Role.Background, "#E61E1E2E"))
        assertEquals(MaterialSlot.Primary, material.materialSlot(Role.Accent, "#FFFF0000"))
        assertEquals(MaterialSlot.Text, material.materialSlot(Role.Text, "#FFFFFFFF"))
        assertEquals(MaterialSlot.Text80, material.materialSlot(Role.Text, "#CCFFFFFF"))
        assertEquals(MaterialSlot.Text65, material.materialSlot(Role.Text, "#80FFFFFF"))
        assertEquals(MaterialSlot.Up, material.materialSlot(Role.TrendUp, "#FF4ADE80"))
        assertEquals(baseline.color(MaterialSlot.Surface, true), material.argb(Role.Background, "#E61E1E2E", dark = true))
        assertEquals(baseline.color(MaterialSlot.Surface, false), material.argb(Role.Background, "#E61E1E2E", dark = false))
        assertTrue(material.followsDarkMode)
        assertTrue(!glass.followsDarkMode)
    }
}

class ShapesAndBlurTest {
    private val design = DesignParser.parse("""{"cornerRadius":20,"padding":14,"root":{"type":"text","text":"x"}}""")

    @Test
    fun ovalOneRowWidgets() {
        val look = com.sidequest.widgetmaker.spec.Look(pillMaxHeight = 64)
        val p = com.sidequest.widgetmaker.spec.Shapes.panel(320, 90, design, look)
        assertEquals(com.sidequest.widgetmaker.spec.WidgetShape.pill, p.shape)
        assertEquals(64, p.height)
        assertEquals(32, p.radius)
        // Taller widgets keep their shape.
        assertEquals(com.sidequest.widgetmaker.spec.WidgetShape.rounded, com.sidequest.widgetmaker.spec.Shapes.panel(320, 160, design, look).shape)
    }

    @Test
    fun lookOverridesRadiusAndInset() {
        val look = com.sidequest.widgetmaker.spec.Look(cornerRadius = 36, inset = 8)
        val p = com.sidequest.widgetmaker.spec.Shapes.panel(160, 160, design, look)
        assertEquals(144, p.width)
        assertEquals(36, p.radius)
        assertEquals(20, com.sidequest.widgetmaker.spec.Shapes.panel(160, 160, design, com.sidequest.widgetmaker.spec.Look()).radius)
    }

    @Test
    fun circlesAreSquare() {
        val p = com.sidequest.widgetmaker.spec.Shapes.panel(320, 160, design, com.sidequest.widgetmaker.spec.Look(), com.sidequest.widgetmaker.spec.WidgetShape.circle)
        assertEquals(p.width, p.height)
        assertEquals(80, p.radius)
    }

    @Test
    fun wallpaperAreaUnderAWidget() {
        val B = com.sidequest.widgetmaker.spec.Backdrop
        // A 2000x2000 wallpaper on a 1000x2000 screen is scaled 1x and centered: 500 px cut off each side.
        val area = B.wallpaperArea(2000, 2000, 1000, 2000, com.sidequest.widgetmaker.spec.Backdrop.Box(100, 200, 300, 400))
        assertEquals(com.sidequest.widgetmaker.spec.Backdrop.Box(600, 200, 800, 400), area)
    }

    @Test
    fun blurSmoothsAndBrightnessPicksTextColor() {
        val B = com.sidequest.widgetmaker.spec.Backdrop
        val w = 20; val h = 20
        val px = IntArray(w * h) { i -> if ((i % w) < 10) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        B.blur(px, w, h, radius = 3)
        val mid = (px[10 * w + 9] shr 16) and 0xFF
        assertTrue("edge is blended: $mid", mid in 60..200)
        assertTrue(B.luminance(IntArray(16) { 0xFFF0F0F0.toInt() }) > B.LIGHT_ABOVE)
        assertTrue(B.luminance(IntArray(16) { 0xFF202020.toInt() }) < B.LIGHT_ABOVE)
    }
}
