package com.oxdm.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.oxdm.app.OxdmApplication
import com.oxdm.app.ui.AddDownloadActivity
import java.util.regex.Pattern

/**
 * Intercepts download notifications posted by Google Chrome, Samsung Internet,
 * Firefox, and Android DownloadManager to offer instant 1-tap takeover by oxdm.
 */
class DownloadNotificationInterceptorService : NotificationListenerService() {

    private val urlPattern = Pattern.compile("https?://[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE)
    private val handledNotifications = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        val isBrowserOrDownloader = pkg in KNOWN_DOWNLOAD_PACKAGES || pkg.contains("download", ignoreCase = true)
        if (!isBrowserOrDownloader) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val combined = "$title $text $subText $bigText"

        // Check if this notification represents an active download
        val isDownloadNotice = combined.contains("download", ignoreCase = true) ||
                combined.contains("kB", ignoreCase = true) ||
                combined.contains("MB", ignoreCase = true) ||
                combined.contains("GB", ignoreCase = true) ||
                combined.contains("%")

        if (!isDownloadNotice) return

        val key = "${pkg}_${title.hashCode()}"
        if (handledNotifications.contains(key)) return
        handledNotifications.add(key)

        // Try extracting URL from notification content
        val matcher = urlPattern.matcher(combined)
        val extractedUrl = if (matcher.find()) matcher.group() else null

        // Offer oxdm multi-part turbo download
        showTakeoverNotification(pkg, title.ifEmpty { "File" }, extractedUrl)
    }

    private fun showTakeoverNotification(sourceApp: String, filename: String, url: String?) {
        val intent = Intent(this, AddDownloadActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            if (url != null) {
                data = Uri.parse(url)
            }
            putExtra("EXTRA_FILENAME", filename)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            filename.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, OxdmApplication.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Download with oxdm?")
            .setContentText("Accelerate '$filename' with multi-part segmented downloading.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.stat_sys_download, "Turbo Download (oxdm)", pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(filename.hashCode(), notification)
    }

    companion object {
        val KNOWN_DOWNLOAD_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.canary",
            "com.chrome.dev",
            "com.android.providers.downloads",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser"
        )
    }
}
