package net.darapu.projectbd.services

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import android.widget.Toast

class BluetoothReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("ProjectBD", "BluetoothReceiver onReceive: $action")
        
        // Use the deprecated getParcelableExtra to ensure it works across all OEM variations
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wasScreenOff = !powerManager.isInteractive
        
        if (device != null) {
            val sharedPrefs = context.getSharedPreferences("ProjectBDPrefs", Context.MODE_PRIVATE)
            val targetDevices = sharedPrefs.getStringSet("target_bluetooth_devices", null)
            val minBattery = sharedPrefs.getInt("min_battery_level", 20)
            
            // Collect target MAC addresses (supporting fallback to legacy single device string)
            val targetMacs = mutableListOf<String>()
            if (targetDevices != null) {
                targetDevices.forEach { 
                    val mac = it.split("|").firstOrNull()
                    if (mac != null) targetMacs.add(mac)
                }
            } else {
                val oldMac = sharedPrefs.getString("target_bluetooth_mac", null)
                if (oldMac != null) targetMacs.add(oldMac)
            }
            
            Log.d("ProjectBD", "Connected Device MAC: ${device.address}, Target MACs: $targetMacs")
            
            if (targetMacs.any { device.address.equals(it, ignoreCase = true) }) {
                if (!WifiAccessibilityService.isServiceEnabled) {
                    Log.w("ProjectBD", "Accessibility Service is NOT enabled!")
                    Toast.makeText(context, "ProjectBD: Please enable Accessibility Service for automation!", Toast.LENGTH_LONG).show()
                }

                if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                    val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryPct = if (scale > 0) (level * 100 / scale) else 100
                    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                    if (batteryPct < minBattery && !isCharging) {
                        Log.d("ProjectBD", "Battery too low ($batteryPct%) and not charging. Skipping Hotspot.")
                        Toast.makeText(context, "ProjectBD: Battery low ($batteryPct%). Hotspot automation skipped.", Toast.LENGTH_LONG).show()
                    } else {
                        Log.d("ProjectBD", "Target device connected, turning on hotspot")
                        Toast.makeText(context, "ProjectBD: Device connected! Enabling Hotspot...", Toast.LENGTH_LONG).show()
                        try {
                            // Pass whether the screen was off so it can lock again when done
                            WifiAccessibilityService.wakeUpAndLaunchTetherSettings(context, true, wasScreenOff)
                        } catch (e: Exception) {
                            Log.e("ProjectBD", "Error launching TetherSettings", e)
                            Toast.makeText(context, "ProjectBD Error: Could not launch Settings", Toast.LENGTH_LONG).show()
                        }
                    }
                } else if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                    Log.d("ProjectBD", "Target device disconnected, turning off hotspot")
                    Toast.makeText(context, "ProjectBD: Device disconnected! Disabling Hotspot...", Toast.LENGTH_LONG).show()
                    try {
                        // Pass whether the screen was off so it can lock again when done
                        WifiAccessibilityService.wakeUpAndLaunchTetherSettings(context, false, wasScreenOff)
                    } catch (e: Exception) {
                        Log.e("ProjectBD", "Error launching TetherSettings", e)
                        Toast.makeText(context, "ProjectBD Error: Could not launch Settings", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.d("ProjectBD", "Device didn't match any targets. Target MACs: $targetMacs vs Connected: ${device.address}")
            }
        } else {
            Log.w("ProjectBD", "Received $action but device was null!")
        }
    }
}