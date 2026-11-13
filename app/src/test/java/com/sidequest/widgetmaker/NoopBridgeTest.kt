package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.Health
import com.sidequest.widgetmaker.feeds.NoopMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class NoopBridgeTest {
    private val snap = NoopMath.Snapshot(
        charge = 75, rest = 91, effort = 45, heart = 62, heartStale = false, battery = 82, connected = true,
        updatedAtMs = 0, series = "1000:60,1060:64,1120:70",
    )

    @Test
    fun theForksOwnScoresBecomeWidgetValues() {
        val v = NoopMath.values(snap, ZoneOffset.UTC)
        assertEquals("75", v["health.charge"])
        assertEquals("Charged", v["health.charge.label"])
        assertEquals("91", v["health.rest"])
        assertEquals("45", v["health.effort"])
        assertEquals("45", v["health.effort.pct"])
        assertEquals("62", v["health.heart"])
        assertEquals("62", v["health.heart.last"])
        assertEquals("60", v["health.heartMin"])
        assertEquals("82%", v["noop.battery"])
        assertEquals("NOOP", v["health.source"])
        assertTrue(v.getValue("health.heart.series").startsWith("60,"))
    }

    @Test
    fun blanksStayBlankAndACarriedOverReadingIsNotNow() {
        val v = NoopMath.values(snap.copy(charge = -1, heartStale = true, series = ""), ZoneOffset.UTC)
        assertNull(v["health.charge"])
        assertNull(v["health.heart"])
        assertEquals("62", v["health.heart.last"])
    }

    @Test
    fun aFullDayFromHealthConnectBeatsTheForksThreeHours() {
        val hc = mapOf("health.heart.series" to "70,72", "health.heartMax" to "140", "health.charge" to "50")
        val merged = NoopMath.merge(hc, NoopMath.values(snap, ZoneOffset.UTC))
        assertEquals("70,72", merged["health.heart.series"])
        assertEquals("140", merged["health.heartMax"])
        // But NOOP's own score replaces the estimate.
        assertEquals("75", merged["health.charge"])
        assertEquals("62", merged["health.heart.last"])
    }

    @Test
    fun everyNoopBuildIsRecognised() {
        listOf("com.noop.whoop", "com.not.noop.oop", "com.not.noop.oop.staging", "com.not.noop.oop.debug", "com.not.noop.oop.demo")
            .forEach { assertTrue(it, it in Health.NOOP_PACKAGES) }
    }

    @Test
    fun theCheckScreenSaysWhatTheForkShares() {
        val now = 11 * 60_000L
        val fresh = snap.copy(updatedAtMs = 60_000L)
        assertEquals(
            "heart rate 62 live, 3-minute trace, Charge 75%, no stress yet, no coach brief, strap connected, updated 10 min ago",
            NoopMath.describe(fresh, now),
        )
        val empty = snap.copy(charge = -1, heart = -1, connected = false, updatedAtMs = 0, series = "")
        assertEquals("no heart rate yet, no trace yet, no stress yet, no coach brief, strap not connected, never updated", NoopMath.describe(empty, now))
        assertEquals(
            "heart rate 62 (not live), 3-minute trace, Charge 75%, no stress yet, no coach brief, strap connected, updated 2 h ago",
            NoopMath.describe(fresh.copy(heartStale = true), 60_000L + 2 * 3_600_000L),
        )
    }

    @Test
    fun stressBecomesWidgetValues() {
        // 9:00, 10:00 (unscored), 11:00 and 12:00 UTC.
        val v = NoopMath.stress("32400:2.8:0,36000:-:0,39600:1.25:1,43200:1.6:0", ZoneOffset.UTC)
        assertEquals("1.6", v["noop.stress"])
        assertEquals("Moderate", v["noop.stress.label"])
        assertEquals("53", v["noop.stress.pct"])
        assertEquals("1.9", v["noop.stress.avg"])
        assertEquals("2.8", v["noop.stress.peak"])
        assertEquals("9:00 AM", v["noop.stress.peakAt"])
        assertEquals("280,125,160", v["noop.stress.series"])
        assertEquals("9:00 AM", v["noop.stress.start"])
        assertEquals("12:00 PM", v["noop.stress.end"])
        assertTrue(NoopMath.stress("", ZoneOffset.UTC).isEmpty())
        assertTrue(NoopMath.stress("100:-:0", ZoneOffset.UTC).isEmpty())
    }

    @Test
    fun theCoachBriefLosesItsMarkdown() {
        val brief = "### Today's Coaching Brief\n* **Readiness:** Your **charge is 67%**.\n\n* **Training Plan:** Hit a quality session."
        val at = java.time.Instant.parse("2026-11-13T05:28:00Z").toEpochMilli()
        val v = NoopMath.coach(brief, at, ZoneOffset.UTC, today = java.time.LocalDate.of(2026, 11, 13))
        assertEquals("Today's Coaching Brief", v["noop.coach.title"])
        assertEquals("• Readiness: Your charge is 67%.\n• Training Plan: Hit a quality session.", v["noop.coach.body"])
        assertEquals("Readiness: Your charge is 67%.", v["noop.coach.summary"])
        assertEquals("5:28 AM", v["noop.coach.time"])
        assertEquals("Fri Nov 13", NoopMath.coach(brief, at, ZoneOffset.UTC, today = java.time.LocalDate.of(2026, 11, 14))["noop.coach.time"])
        assertEquals("Coach brief", NoopMath.coach("Rest well today.", 0, ZoneOffset.UTC)["noop.coach.title"])
        assertTrue(NoopMath.coach("  \n", 0, ZoneOffset.UTC).isEmpty())
    }

    @Test
    fun ringsShrinkToFitASlimPill() {
        val ring = com.sidequest.widgetmaker.spec.RingNode(size = 44, thickness = 5, textSize = 14)
        // A pill 56 tall keeps 9 above and below: 38 for the ring.
        val pill = ring.fitted(38)
        assertEquals(38, pill.size)
        assertEquals(4, pill.thickness)
        assertEquals(12, pill.textSize)
        assertEquals(ring, ring.fitted(120))
        assertEquals(16, ring.fitted(6).size)
    }
}
