package com.oxdm.feature.browser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme

class BrowserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                BrowserScreen(
                    onClose = { finish() },
                    onDownloadRequested = { url, filename, cookies, referrer, mimeType ->
                        triggerAppDownload(url, filename, cookies, referrer, mimeType)
                    }
                )
            }
        }
    }

    private fun triggerAppDownload(
        url: String,
        filename: String?,
        cookies: String?,
        referrer: String?,
        mimeType: String?
    ) {
        try {
            // Launch AddDownloadActivity to configure parts, category, and start download
            val intent = Intent().apply {
                setClassName(packageName, "com.oxdm.app.ui.AddDownloadActivity")
                action = Intent.ACTION_VIEW
                data = Uri.parse(url)
                putExtra("EXTRA_FILENAME", filename)
                putExtra("EXTRA_COOKIES", cookies)
                putExtra("EXTRA_REFERRER", referrer)
                putExtra("EXTRA_MIME_TYPE", mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
