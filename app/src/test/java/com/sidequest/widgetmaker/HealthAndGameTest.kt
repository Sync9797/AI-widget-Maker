package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.HealthMath
import com.sidequest.widgetmaker.feeds.MonsterQuestMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class HealthAndGameTest {
    private val now = LocalDateTime.of(2026, 11, 11, 15, 0)
    private val zone = ZoneOffset.UTC
    private fun ms(t: LocalDateTime) = t.toInstant(zone).toEpochMilli()

    @Test
    fun healthValues() {
        val v = HealthMath.values(
            steps = 3508, stepGoal = 8000, activeKcal = 411.6, totalKcal = 1850.2, exerciseMinutes = 45,
            heart = listOf(HealthMath.Pulse(ms(now.minusMinutes(5)), 72)),
            resting = 58,
            workout = HealthMath.Workout("Run", LocalDateTime.of(2026, 11, 11, 7, 0), 32),
            sleep = HealthMath.Sleep(432, 84, 96),
            now = now, zone = zone,
        )
        assertEquals("3,508", v["health.steps"])
        assertEquals("43", v["health.stepsPct"])
        assertEquals("412", v["health.kcal"])
        assertEquals("72", v["health.heart"])
        assertEquals("58", v["health.restingHeart"])
        assertEquals("Today 7 AM", v["health.workout.when"])
        assertEquals("32 min", v["health.workout.duration"])
        assertEquals("45 min", v["health.exercise"])
        assertEquals("7h 12m", v["health.sleep"])
        assertEquals("1h 24m", v["health.sleep.deep"])
    }

    @Test
    fun oldHeartRateIsntShownAsCurrent() {
        val v = HealthMath.values(null, 8000, null, null, null, listOf(HealthMath.Pulse(ms(now.minusHours(5)), 90)), null, null, null, now, zone)
        assertNull(v["health.heart"])
        assertEquals("90", v["health.heartMax"])
    }

    @Test
    fun effortGrowsWithHardWork() {
        fun day(bpm: Int, minutes: Int) = (0..minutes).map { HealthMath.Pulse(it * 60_000L, bpm) }
        val rest = HealthMath.effort(day(62, 600), resting = 60)
        val walk = HealthMath.effort(day(105, 60), resting = 60)
        val run = HealthMath.effort(day(165, 60), resting = 60)
        assertTrue("rest $rest", rest < 2)
        assertTrue("walk $walk < run $run", walk < run)
        assertTrue("run $run stays under 21", run in 8.0..21.0)
    }

    @Test
    fun monsterQuestSummary() {
        val nowMs = ms(now)
        val body = """{"hero":"Aaron","level":23,"steps":3508,"torches":4,"gold":1200,
            "eggs":[{"name":"Ember Pup","progress":100,"left":"0 m","ready":true},{"name":"Tide Sprite","progress":61.7,"left":"1.2 km","ready":false}],
            "adventures":[{"title":"Clear the cellar","place":"Joe's Coffee","team":"Ember, Pip","startMs":${nowMs - 3_600_000},"endMs":${nowMs + 3_600_000}},
                          {"title":"Scout the river","place":"Park","team":"Tide","startMs":${nowMs - 7_200_000},"endMs":${nowMs - 60_000}}],
            "sleep":{"minutes":432,"deep":84}}"""
        val v = MonsterQuestMath.values(body, nowMs)
        assertEquals("Aaron", v["mq.hero"])
        assertEquals("3,508", v["mq.steps"])
        assertEquals("1,200", v["mq.gold"])
        assertEquals("2", v["mq.eggs"])
        assertEquals("1", v["mq.eggs.ready"])
        assertEquals("Ready to hatch!", v["mq.egg.1.left"])
        assertEquals("1.2 km to go", v["mq.egg.2.left"])
        assertEquals("62", v["mq.egg.2.progress"])
        assertEquals("1 h left", v["mq.adv.1.left"])
        assertEquals("50", v["mq.adv.1.progress"])
        assertEquals("Done! Collect your loot", v["mq.adv.2.left"])
        assertEquals("1", v["mq.adventures.done"])
        assertEquals("7h 12m", v["mq.sleep"])
    }
}

class IfSetTest {
    @Test
    fun listRowsHideWithoutData() {
        val tokens = com.sidequest.widgetmaker.spec.Tokens(LocalDateTime.now(), null, false, feeds = mapOf("mq.egg.1" to "Ember Pup"))
        fun row(token: String) = com.sidequest.widgetmaker.spec.TextNode("x", show = com.sidequest.widgetmaker.spec.Show(ifSet = token))
        assertTrue(com.sidequest.widgetmaker.spec.visible(row("{mq.egg.1}"), 300, 300, tokens))
        assertTrue(!com.sidequest.widgetmaker.spec.visible(row("{mq.egg.2}"), 300, 300, tokens))
        // Without values (just sizing), rows count as shown.
        assertTrue(com.sidequest.widgetmaker.spec.visible(row("{mq.egg.2}"), 300, 300))
    }
}
