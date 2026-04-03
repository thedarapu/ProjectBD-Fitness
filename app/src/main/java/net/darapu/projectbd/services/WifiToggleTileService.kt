package net.darapu.projectbd.services

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.wifi.WifiManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import net.darapu.projectbd.R

class WifiToggleTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        
        // WifiManager.isWifiEnabled is deprecated in API 31+, but still widely used for simple checks
        val isWifiEnabled = try {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled
        } catch (e: Exception) {
            false
        }
        
        tile.state = if (isWifiEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "ProjectBD wifi toggle"
        tile.subtitle = if (isWifiEnabled) "On" else "Off"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_wifi_custom)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        // Tell our Accessibility Service to automate the switch when the panel opens!
        WifiAccessibilityService.isAutomatingWifi = true

        val intent = Intent(Settings.Panel.ACTION_WIFI)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        startActivityAndCollapse(pendingIntent)
    }
}
