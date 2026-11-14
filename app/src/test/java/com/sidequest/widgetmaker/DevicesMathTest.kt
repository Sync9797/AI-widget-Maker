package com.sidequest.widgetmaker

import com.sidequest.widgetmaker.feeds.DevicesMath
import com.sidequest.widgetmaker.feeds.DevicesMath.Device
import com.sidequest.widgetmaker.feeds.DevicesMath.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DevicesMathTest {
    @Test
    fun phoneThenStrapThenEmptiestFirst() {
        val out = DevicesMath.values(
            listOf(
                Device("Galaxy Watch", 80, Kind.watch),
                Device("Strap", 53, Kind.strap),
                Device("Buds", 15, Kind.buds),
                Device("Phone", 92, Kind.phone, charging = true),
                Device("Mouse", -1, Kind.other),
            ),
        )
        assertEquals("5", out["devices.count"])
        assertEquals("Phone", out["devices.1.name"])
        assertEquals("Charging", out["devices.1.charging"])
        assertEquals("band", out["devices.2.icon"])
        assertEquals("Buds", out["devices.3.name"])
        assertEquals("15%", out["devices.3.pct"])
        assertEquals("80", out["devices.4.level"])
        assertEquals("Mouse", out["devices.5.name"]) // No battery reading: listed last.
        assertNull(out["devices.6.name"])
    }

    @Test
    fun kindsFromNamesAndClasses() {
        assertEquals(Kind.strap, DevicesMath.kindOf("WHOOP 4A1234", null))
        assertEquals(Kind.buds, DevicesMath.kindOf("Galaxy Buds3 Pro", DevicesMath.MAJOR_AUDIO_VIDEO))
        assertEquals(Kind.watch, DevicesMath.kindOf("Galaxy Watch7", null))
        assertEquals(Kind.watch, DevicesMath.kindOf("Pixel", DevicesMath.MAJOR_WEARABLE))
        assertEquals(Kind.headphones, DevicesMath.kindOf("WH-1000XM5", null))
        assertEquals(Kind.other, DevicesMath.kindOf("Car kit", null))
    }
}
