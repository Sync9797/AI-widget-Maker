package com.sidequest.widgetmaker.feeds

/**
 * The battery of each device: this phone, the strap through not-NOOP, and Bluetooth devices that report
 * theirs. Free of Android, so it's tested on its own.
 */
object DevicesMath {
    /** What a device is, for its icon (an IconName). */
    enum class Kind(val icon: String) { phone("phone"), strap("band"), watch("watch"), buds("headphones"), headphones("headphones"), other("bluetooth") }

    data class Device(val name: String, val level: Int, val kind: Kind, val charging: Boolean = false)

    /** A Bluetooth device's kind from its name and its Bluetooth major class (BluetoothClass.Device.Major). */
    fun kindOf(name: String, majorClass: Int?): Kind {
        val n = name.lowercase()
        return when {
            listOf("whoop", "strap", "band", "fit", "oura", "ring").any { it in n } -> Kind.strap
            "watch" in n || majorClass == MAJOR_WEARABLE -> Kind.watch
            listOf("bud", "airpod", "pods", "earbud").any { it in n } -> Kind.buds
            majorClass == MAJOR_AUDIO_VIDEO || listOf("headphone", "head", "wh-", "xm", "quietcomfort").any { it in n } -> Kind.headphones
            majorClass == MAJOR_PHONE -> Kind.phone
            else -> Kind.other
        }
    }

    /**
     * devices.N.name, .level ("95"), .pct ("95%"), .icon (an icon name) and .charging ("Charging" or ""),
     * phone first, then the strap, then the rest from the emptiest up (what needs charging soonest);
     * devices.count. Devices without a battery reading are left out.
     */
    fun values(devices: List<Device>, max: Int = 8): Map<String, String> {
        val known = devices.filter { it.level in 0..100 }
        val ordered = known.filter { it.kind == Kind.phone } + known.filter { it.kind == Kind.strap } +
            known.filter { it.kind != Kind.phone && it.kind != Kind.strap }.sortedBy { it.level }
        val out = mutableMapOf("devices.count" to ordered.size.coerceAtMost(max).toString())
        ordered.take(max).forEachIndexed { i, d ->
            val n = i + 1
            out["devices.$n.name"] = d.name
            out["devices.$n.level"] = d.level.toString()
            out["devices.$n.pct"] = "${d.level}%"
            out["devices.$n.icon"] = d.kind.icon
            out["devices.$n.charging"] = if (d.charging) "Charging" else ""
        }
        return out
    }

    // android.bluetooth.BluetoothClass.Device.Major values.
    const val MAJOR_PHONE = 0x0200
    const val MAJOR_AUDIO_VIDEO = 0x0400
    const val MAJOR_WEARABLE = 0x0700
}
