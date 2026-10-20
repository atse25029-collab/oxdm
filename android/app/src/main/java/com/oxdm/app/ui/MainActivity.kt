package com.oxdm.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.oxdm.app.service.DownloadForegroundService
import com.oxdm.app.ui.screens.MainDashboardScreen
import com.oxdm.app.ui.theme.OxdmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure foreground service is ready for ongoing operations
        DownloadForegroundService.start(this)

        setContent {
            OxdmTheme {
                MainDashboardScreen(
                    onOpenBrowser = {
                        try {
                            val intent = Intent(this, Class.forName("com.oxdm.feature.browser.BrowserActivity"))
                            startActivity(intent)
                        } catch (e: ClassNotFoundException) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}
