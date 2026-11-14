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
 * through not-NOOP (Android can't see a strap's battery, since not-NOOP talks to it directly), and every
 * connected Bluetooth device, with its battery where it reports one to Android (most earbuds, headphones and
 * watches do). Each widget can leave devices out (its settings sheet).
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
        val strap = NoopBridge.read(context)?.takeIf { it.battery in 0..100 }
        strap?.let { out += DevicesMath.Device("Strap", it.battery, DevicesMath.Kind.strap) }
        out += bluetooth(context, strapKnown = strap != null)
        return out
    }

    /**
     * Every connected Bluetooth device, with its battery level where it tells Android (-1 where it doesn't):
     * paired devices that are connected, and Bluetooth LE devices connected without pairing. Android keeps the
     * battery and connected calls hidden, so they're looked up by name. The WHOOP strap is left out when
     * not-NOOP already gives its battery.
     */
    @Suppress("MissingPermission")
    private fun bluetooth(context: Context, strapKnown: Boolean): List<DevicesMath.Device> {
        if (!canSee(context)) return emptyList()
        val manager = context.getSystemService(BluetoothManager::class.java) ?: return emptyList()
        val adapter = manager.adapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        val isConnected = runCatching { BluetoothDevice::class.java.getMethod("isConnected") }.getOrNull()
        val batteryLevel = runCatching { BluetoothDevice::class.java.getMethod("getBatteryLevel") }.getOrNull()
        fun level(device: BluetoothDevice) = runCatching { batteryLevel?.invoke(device) as? Int }.getOrNull()?.takeIf { it in 0..100 } ?: -1
        val paired = adapter.bondedDevices.orEmpty().filter { device ->
            // Without the hidden "connected" call, a paired device counts as connected only when it reports a battery.
            runCatching { isConnected?.invoke(device) as? Boolean }.getOrNull() ?: (level(device) >= 0)
        }
        val le = runCatching { manager.getConnectedDevices(android.bluetooth.BluetoothProfile.GATT) }.getOrNull().orEmpty()
        return (paired + le).distinctBy { it.address }.mapNotNull { device ->
            runCatching {
                val name = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) device.alias else null) ?: device.name ?: return@runCatching null
                val kind = DevicesMath.kindOf(name, device.bluetoothClass?.majorDeviceClass)
                if (kind == DevicesMath.Kind.strap && strapKnown) return@runCatching null
                DevicesMath.Device(name, level(device), kind)
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
