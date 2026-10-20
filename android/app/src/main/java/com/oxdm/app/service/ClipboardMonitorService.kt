package com.oxdm.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.oxdm.app.OxdmApplication
import com.oxdm.app.R
import com.oxdm.app.ui.AddDownloadActivity

/**
 * Background clipboard listener service that detects copied download links
 * and immediately offers a 1-tap download prompt.
 */
class ClipboardMonitorService : Service() {

    private var clipboardManager: ClipboardManager? = null
    private var lastCopiedUrl: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    private fun checkClipboard() {
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount == 0) return

        val text = clip.getItemAt(0)?.text?.toString()?.trim() ?: return
        if (text == lastCopiedUrl) return

        if (isDownloadableUrl(text)) {
            lastCopiedUrl = text
            showDownloadPromptNotification(text)
        }
    }

    private fun isDownloadableUrl(text: String): Boolean {
        if (!text.startsWith("http://", ignoreCase = true) &&
            !text.startsWith("https://", ignoreCase = true) &&
            !text.startsWith("magnet:", ignoreCase = true)
        ) {
            return false
        }
        return true
    }

    private fun showDownloadPromptNotification(url: String) {
        val intent = Intent(this, AddDownloadActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val filename = url.substringAfterLast("/").substringBefore("?").ifEmpty { "file" }

        val notification = NotificationCompat.Builder(this, OxdmApplication.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Download with oxdm?")
            .setContentText(filename)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.stat_sys_download, "Start Download", pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_CLIPBOARD_PROMPT_ID, notification)
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_CLIPBOARD_PROMPT_ID = 2002

        fun start(context: Context) {
            val intent = Intent(context, ClipboardMonitorService::class.java)
            context.startService(intent)
        }
    }
}
