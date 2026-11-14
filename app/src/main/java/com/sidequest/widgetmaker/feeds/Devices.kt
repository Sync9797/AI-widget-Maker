package com.sidequest.widgetmaker.feeds

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import com.sidequest.widgetmaker.data.DesignStore
import com.sidequest.widgetmaker.spec.DesignParser
import com.sidequest.widgetmaker.widget.AiWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Battery levels for the Device batteries widgets, read when a widget draws: this phone, the WHOOP strap
 * through not-NOOP (Android can't see a strap's battery, since not-NOOP talks to it directly), and the
 * connected Bluetooth devices that report a battery level to Android (most earbuds, headphones and watches).
 */
object Devices {
    /** Whether the app may see Bluetooth devices (Android 12+ asks; older versions allow it). */
    fun canSee(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun values(context: Context): Map<String, String> = runCatching { DevicesMath.values(read(context)) }.getOrDefault(emptyMap())

    fun read(context: Context): List<DevicesMath.Device> {
        val out = mutableListOf<DevicesMath.Device>()
        val battery = context.getSystemService(BatteryManager::class.java)
        battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }?.let {
            out += DevicesMath.Device("Phone", it, DevicesMath.Kind.phone, charging = battery.isCharging)
        }
        NoopBridge.read(context)?.takeIf { it.battery in 0..100 }?.let {
            out += DevicesMath.Device("Strap", it.battery, DevicesMath.Kind.strap)
        }
        out += bluetooth(context)
        return out
    }

    /** Connected Bluetooth devices with a battery level. Android keeps these calls hidden, so they're looked up by name. */
    @Suppress("MissingPermission")
    private fun bluetooth(context: Context): List<DevicesMath.Device> {
        if (!canSee(context)) return emptyList()
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        val isConnected = runCatching { BluetoothDevice::class.java.getMethod("isConnected") }.getOrNull()
        val batteryLevel = runCatching { BluetoothDevice::class.java.getMethod("getBatteryLevel") }.getOrNull() ?: return emptyList()
        return adapter.bondedDevices.orEmpty().mapNotNull { device ->
            runCatching {
                if (isConnected != null && isConnected.invoke(device) != true) return@runCatching null
                val level = batteryLevel.invoke(device) as? Int ?: return@runCatching null
                if (level !in 0..100) return@runCatching null
                val name = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias else null) ?: device.name ?: "Device"
                DevicesMath.Device(name, level, DevicesMath.kindOf(name, device.bluetoothClass?.majorDeviceClass))
            }.getOrNull()
        }
    }
}

/**
 * A Bluetooth device connected, disconnected or reported a new battery level: redraw the widgets that show
 * device batteries.
 */
class DevicesChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val store = DesignStore(app)
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(app)
                manager.getGlanceIds(AiWidget::class.java).forEach { id ->
                    val design = store.designFor(manager.getAppWidgetId(id))?.design ?: return@forEach
                    if (DesignParser.encode(design).contains("{devices.")) AiWidget.refresh(app, id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
