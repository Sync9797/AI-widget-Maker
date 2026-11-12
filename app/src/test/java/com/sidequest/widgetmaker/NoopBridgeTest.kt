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
}
