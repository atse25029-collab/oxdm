package com.oxdm.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.oxdm.app.data.OxdmBridge
import com.oxdm.app.service.DownloadForegroundService
import com.oxdm.app.ui.screens.AddDownloadSheet
import com.oxdm.app.ui.theme.OxdmTheme
import java.util.regex.Pattern

/**
 * Universal dialog-style activity that catches ACTION_SEND (from Chrome Share menu),
 * ACTION_VIEW (from links / files), and clipboard shares.
 */
class AddDownloadActivity : ComponentActivity() {

    private val urlPattern = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetUrl = extractUrlFromIntent(intent)

        if (targetUrl.isNullOrBlank()) {
            finish()
            return
        }

        val suggestedFilename = intent.getStringExtra("EXTRA_FILENAME")
            ?: targetUrl.substringAfterLast("/").substringBefore("?").ifEmpty { null }

        setContent {
            OxdmTheme {
                AddDownloadSheet(
                    initialUrl = targetUrl,
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

        // 1. Check intent.data / dataString (ACTION_VIEW from browser links)
        intent.dataString?.let { dataStr ->
            findUrlInText(dataStr)?.let { return it }
        }

        // 2. Check EXTRA_TEXT (ACTION_SEND from Chrome Share menu)
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extraText ->
            findUrlInText(extraText)?.let { return it }
        }

        // 3. Check ClipData if present
        intent.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                val itemText = clipData.getItemAt(i)?.text?.toString()
                if (itemText != null) {
                    findUrlInText(itemText)?.let { return it }
                }
                val itemUri = clipData.getItemAt(i)?.uri?.toString()
                if (itemUri != null) {
                    findUrlInText(itemUri)?.let { return it }
                }
            }
        }

        return null
    }

    private fun findUrlInText(text: String): String? {
        val trimmed = text.trim()
        val matcher = urlPattern.matcher(trimmed)
        return if (matcher.find()) {
            matcher.group()
        } else if (trimmed.startsWith("magnet:", ignoreCase = true)) {
            trimmed
        } else {
            null
        }
    }
}
