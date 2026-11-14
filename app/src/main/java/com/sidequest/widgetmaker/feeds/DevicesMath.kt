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
     * devices.N.name, .level ("95", or "" when the device doesn't say), .pct ("95%" or "--"), .num ("95" or
     * "--", for rings), .icon (an icon name), .charging ("Charging" or "") and .kind; devices.count. The phone
     * first, then the strap, then the rest from the emptiest up (what needs charging soonest), and last the
     * connected devices that don't report a battery.
     */
    fun values(devices: List<Device>, max: Int = 8): Map<String, String> {
        val shown = devices.filter { it.level in -1..100 }
        val ordered = shown.filter { it.kind == Kind.phone } + shown.filter { it.kind == Kind.strap } +
            shown.filter { it.kind != Kind.phone && it.kind != Kind.strap && it.level >= 0 }.sortedBy { it.level } +
            shown.filter { it.kind != Kind.phone && it.kind != Kind.strap && it.level < 0 }
        val out = mutableMapOf("devices.count" to ordered.size.coerceAtMost(max).toString())
        ordered.take(max).forEachIndexed { i, d ->
            val n = i + 1
            val known = d.level >= 0
            out["devices.$n.name"] = d.name
            out["devices.$n.level"] = if (known) d.level.toString() else ""
            out["devices.$n.pct"] = if (known) "${d.level}%" else "--"
            out["devices.$n.num"] = if (known) d.level.toString() else "--"
            out["devices.$n.icon"] = d.kind.icon
            out["devices.$n.kind"] = d.kind.name
            out["devices.$n.charging"] = if (d.charging) "Charging" else ""
        }
        return out
    }

    /** The devices in [values] (as [values] made them), so a widget can leave some out and number the rest again. */
    fun parse(values: Map<String, String>): List<Device> = (1..(values["devices.count"]?.toIntOrNull() ?: 0)).mapNotNull { n ->
        val name = values["devices.$n.name"] ?: return@mapNotNull null
        Device(
            name,
            values["devices.$n.level"]?.toIntOrNull() ?: -1,
            values["devices.$n.kind"]?.let { k -> Kind.entries.firstOrNull { it.name == k } } ?: Kind.other,
            charging = values["devices.$n.charging"] == "Charging",
        )
    }

    /** [values] with the devices named in [hidden] left out (one widget's choice), the rest numbered again. */
    fun hide(values: Map<String, String>, hidden: Set<String>): Map<String, String> {
        if (hidden.isEmpty()) return values
        val kept = parse(values).filter { it.name !in hidden }
        return values.filterKeys { !it.startsWith("devices.") } + values(kept)
    }

    // android.bluetooth.BluetoothClass.Device.Major values.
    const val MAJOR_PHONE = 0x0200
    const val MAJOR_AUDIO_VIDEO = 0x0400
    const val MAJOR_WEARABLE = 0x0700
}
