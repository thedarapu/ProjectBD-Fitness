package net.darapu.projectbd.ui.screens.automation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.darapu.projectbd.services.WifiAccessibilityService

@Composable
fun AutomationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Mocks & Automation", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { 
            WifiAccessibilityService.launchTetherSettings(context, true)
        }) {
            Text(text = "Mock Bluetooth Connect (Enable Hotspot)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { 
            WifiAccessibilityService.launchTetherSettings(context, false)
        }) {
            Text(text = "Mock Bluetooth Disconnect (Disable Hotspot)")
        }
    }
}
