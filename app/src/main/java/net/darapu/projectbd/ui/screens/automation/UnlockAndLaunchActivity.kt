package net.darapu.projectbd.ui.screens.automation

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import net.darapu.projectbd.services.WifiAccessibilityService

class UnlockAndLaunchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ProjectBD", "UnlockAndLaunchActivity created to dismiss keyguard")

        // Turn on screen and dismiss keyguard
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val enable = intent.getBooleanExtra("enable", true)
        val wasScreenOff = intent.getBooleanExtra("wasScreenOff", false)
        
        // Launch the actual settings activity, passing the screen state
        WifiAccessibilityService.launchTetherSettings(this, enable, wasScreenOff)
        
        // Finish this transparent activity immediately
        finish()
    }
}