package net.darapu.projectbd.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.darapu.projectbd.ui.screens.automation.UnlockAndLaunchActivity

class WifiAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isAutomatingWifi = false
        
        @Volatile
        var isAutomatingHotspot = false

        @Volatile
        var targetHotspotState: Boolean? = null
        
        @Volatile
        var shouldLockScreenAfter = false
        
        @Volatile
        var instance: WifiAccessibilityService? = null
        
        val isServiceEnabled: Boolean get() = instance != null
        
        fun wakeUpAndLaunchTetherSettings(context: Context, enable: Boolean, wasScreenOff: Boolean = false) {
            val launcherContext = instance ?: context
            
            val intent = Intent(launcherContext, net.darapu.projectbd.ui.screens.automation.UnlockAndLaunchActivity::class.java).apply {
                putExtra("enable", enable)
                putExtra("wasScreenOff", wasScreenOff)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            try {
                launcherContext.startActivity(intent)
                Log.d("ProjectBD", "Launched UnlockAndLaunchActivity to bypass keyguard")
            } catch (e: Exception) {
                Log.e("ProjectBD", "Failed to launch UnlockAndLaunchActivity", e)
                // Fallback to direct launch if activity fails to start
                launchTetherSettings(context, enable, wasScreenOff)
            }
        }
        
        fun launchTetherSettings(context: Context, enable: Boolean, wasScreenOff: Boolean = false) {
            val launcherContext = instance ?: context
            
            if (instance == null) {
                Log.w("ProjectBD", "Accessibility Service is not running! Fallback to BroadcastReceiver context")
            }
            
            isAutomatingHotspot = true
            targetHotspotState = enable
            shouldLockScreenAfter = wasScreenOff
            
            // Try AOSP WifiTetherSettingsActivity first to get directly to the Hotspot switch
            val intent1 = Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.Settings\$WifiTetherSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            // Try AOSP TetherSettings fallback (main tethering menu)
            val intent2 = Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.TetherSettings")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            
            // Try Samsung specific TetherSettings
            val intent3 = Intent(Intent.ACTION_MAIN).apply {
                setClassName("com.android.settings", "com.android.settings.Settings\$WifiApSettingsActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }

            try {
                // IMPORTANT: use the AccessibilityService context to start the activity
                // to completely bypass Android 10+ background activity restrictions.
                launcherContext.startActivity(intent1)
                Log.d("ProjectBD", "Launched AOSP WifiTetherSettingsActivity")
            } catch (e1: Exception) {
                Log.e("ProjectBD", "Failed AOSP WifiTetherSettingsActivity", e1)
                try {
                    launcherContext.startActivity(intent2)
                    Log.d("ProjectBD", "Launched AOSP TetherSettings")
                } catch (e2: Exception) {
                    Log.e("ProjectBD", "Failed AOSP TetherSettings", e2)
                    try {
                        launcherContext.startActivity(intent3)
                        Log.d("ProjectBD", "Launched Samsung WifiApSettingsActivity")
                    } catch (e3: Exception) {
                        Log.e("ProjectBD", "Failed Samsung WifiApSettingsActivity", e3)
                        isAutomatingHotspot = false 
                        val intent4 = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        try {
                            launcherContext.startActivity(intent4)
                            Log.d("ProjectBD", "Launched WIRELESS_SETTINGS without automation")
                        } catch (e4: Exception) {
                            Log.e("ProjectBD", "Failed WIRELESS_SETTINGS", e4)
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("ProjectBD", "Accessibility Service Connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) {
            instance = null
        }
        Log.d("ProjectBD", "Accessibility Service Unbound")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutomatingWifi && !isAutomatingHotspot) return

        val rootNode = rootInActiveWindow ?: return
        
        if (isAutomatingWifi) {
            if (findAndClickSwitch(rootNode, null)) {
                isAutomatingWifi = false
                closePanel()
            }
        } else if (isAutomatingHotspot) {
            // First, try to find the "Wi-Fi hotspot" row if we are stuck on the main TetherSettings screen
            val hotspotRow = findNodeByText(rootNode, "Wi-Fi hotspot")
            if (hotspotRow != null && hotspotRow.isClickable && hotspotRow.className != "android.widget.Switch") {
                // We're on the main TetherSettings screen, click the sub-menu first!
                hotspotRow.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return // Wait for the next accessibility event to click the actual switch
            }

            // Otherwise, look for the actual switch to toggle
            if (findAndClickSwitch(rootNode, targetHotspotState)) {
                isAutomatingHotspot = false
                targetHotspotState = null
                closePanel()
            }
        }
    }

    private fun closePanel() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(500) // wait slightly longer to ensure the toggle registers
            performGlobalAction(GLOBAL_ACTION_BACK) // Simulate "Back"
            performGlobalAction(GLOBAL_ACTION_HOME) // And go home just in case
            
            // Re-lock the screen if it was off when we started
            if (shouldLockScreenAfter) {
                delay(500)
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            }
            shouldLockScreenAfter = false
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) {
            // Find the closest clickable parent if the text node itself isn't clickable
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) return current
                current = current.parent
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun findAndClickSwitch(node: AccessibilityNodeInfo, targetState: Boolean?): Boolean {
        if (node.className == "android.widget.Switch" || node.className == "android.widget.ToggleButton") {
            if (targetState != null) {
                if (node.isChecked == targetState) {
                    return true // Already in correct state, act as if clicked
                }
            }
            // Sometimes the switch isn't clickable directly but its parent is
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            } else {
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    parent = parent.parent
                }
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickSwitch(child, targetState)) {
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
        // Required
    }
}