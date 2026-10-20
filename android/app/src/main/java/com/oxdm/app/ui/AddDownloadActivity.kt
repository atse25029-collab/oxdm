package com.oxdm.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.oxdm.app.data.OxdmBridge
import com.oxdm.app.service.DownloadForegroundService
import com.oxdm.app.ui.screens.AddDownloadSheet
import com.oxdm.app.ui.theme.OxdmTheme

/**
 * Transparent, dialog-style activity that catches ACTION_SEND (Share sheet)
 * and ACTION_VIEW (direct downloadable links).
 *
 * Strict Zero-Overlay Guarantee: Uses normal Activity window tokens so NO
 * SYSTEM_ALERT_WINDOW permission is required and zero overlay crashes can occur.
 */
class AddDownloadActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = extractUrlFromIntent(intent)

        if (sharedUrl.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            OxdmTheme {
                AddDownloadSheet(
                    initialUrl = sharedUrl,
                    onDismiss = { finish() },
                    onConfirm = { url, filename, category, parts ->
                        OxdmBridge.addJob(url, filename, category, parts)
                        DownloadForegroundService.start(this)
                        finish()
                    }
                )
            }
        }
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (text != null && (text.startsWith("http://") || text.startsWith("https://"))) {
                    text.trim()
                } else null
            }
            Intent.ACTION_VIEW -> {
                intent.dataString
            }
            else -> null
        }
    }
}
