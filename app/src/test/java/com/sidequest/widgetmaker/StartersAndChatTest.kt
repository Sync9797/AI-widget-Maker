package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.ai.Prompts
import com.sidequest.widgetmaker.spec.ColumnNode
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.spec.Node
import com.sidequest.widgetmaker.spec.RowNode
import com.sidequest.widgetmaker.spec.Starters
import com.sidequest.widgetmaker.spec.visible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartersAndChatTest {
    private fun count(node: Node, w: Int, h: Int): Int {
        if (!visible(node, w, h)) return 0
        val kids = when (node) {
            is ColumnNode -> node.children
            is RowNode -> node.children
            else -> emptyList()
        }
        return 1 + kids.sumOf { count(it, w, h) }
    }

    @Test
    fun everyStarterParses() {
        assertEquals(8, Starters.all.size)
        Starters.all.forEach { assertTrue(it.title, it.design.name.isNotBlank()) }
    }

    @Test
    fun everyStarterShowsMoreWhenBigger() {
        Starters.all.forEach { s ->
            val small = count(s.design.root, 160, 80)
            val large = count(s.design.root, 320, 320)
            assertTrue("${s.title}: shows something at 2x1", small > 0)
            assertTrue("${s.title}: grows ($small -> $large)", large > small)
        }
    }

    @Test
    fun clocksFitTheWidthTheyShowAt() {
        // A 4-digit time like "12:59" is about 2.6 em wide in the clock font; widgets are 160 dp at their narrowest.
        fun check(node: Node, minWidth: Int, padding: Int, title: String) {
            val shownFrom = maxOf(minWidth, node.show?.minWidth ?: 0)
            if (node is com.sidequest.widgetmaker.spec.ClockNode && node.format.startsWith("h")) {
                val needed = node.size * 2.6 + padding
                assertTrue("$title: a ${node.size} sp clock needs $needed dp, has $shownFrom", needed <= shownFrom)
            }
            val kids = when (node) {
                is ColumnNode -> node.children
                is RowNode -> node.children
                else -> emptyList()
            }
            kids.forEach { check(it, shownFrom, padding, title) }
        }
        Starters.all.forEach { check(it.design.root, 160, it.design.padding * 2, it.title) }
    }

    @Test
    fun startersSurviveARoundTrip() {
        Starters.all.forEach { s -> assertEquals(s.title, s.design, DesignParser.parse(DesignParser.encode(s.design))) }
    }

    @Test
    fun chatRequestHasEverythingClaudeNeeds() {
        val text = Prompts.forChat("a cozy clock", previous = null, dataSources = "- weather: set up for Austin")
        assertTrue(text.contains(Prompts.SYSTEM))
        assertTrue(text.contains("Design this widget: a cozy clock"))
        assertTrue(text.contains("weather: set up for Austin"))
        assertTrue(text.trimEnd().endsWith("into the app."))
    }

    @Test
    fun chatRequestToChangeADesignIncludesIt() {
        val previous = Starters.all.first().design
        val text = Prompts.forChat("make it purple", previous, dataSources = "")
        assertTrue(text.contains(DesignParser.encode(previous)))
        assertTrue(text.contains("Change it like this, keeping everything else: make it purple"))
    }

    @Test
    fun aTypicalChatReplyParses() {
        // Claude in the app often adds a sentence and a code fence around the JSON.
        val reply = "Here's your widget:\n\n```json\n" + DesignParser.encode(Starters.all[1].design) + "\n```\n\nEnjoy!"
        assertEquals(Starters.all[1].design, DesignParser.parse(reply))
    }
}
