package com.oxdm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.oxdm.app.data.OxdmBridge

class OxdmApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Initialize the native oxdm engine in application context
        val dbDir = filesDir.resolve("db").apply { mkdirs() }
        OxdmBridge.initialize(dbDir.absolutePath)
        com.oxdm.app.service.ClipboardMonitorService.start(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                getString(R.string.download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "oxdm_active_downloads"
    }
}
