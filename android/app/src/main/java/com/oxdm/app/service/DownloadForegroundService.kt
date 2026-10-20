package com.oxdm.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.oxdm.app.OxdmApplication
import com.oxdm.app.R
import com.oxdm.app.data.JobPhase
import com.oxdm.app.data.JobUiModel
import com.oxdm.app.data.OxdmBridge
import com.oxdm.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var observerJob: Job? = null
    private var lastNotificationUpdateTime = 0L

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "oxdm:DownloadWakeLock"
        ).apply { setReferenceCounted(false) }

        startObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_ALL -> {
                OxdmBridge.jobs.value
                    .filter { it.phase == JobPhase.Downloading }
                    .forEach { OxdmBridge.pauseJob(it.id) }
            }
            ACTION_RESUME_ALL -> {
                OxdmBridge.jobs.value
                    .filter { it.phase == JobPhase.Paused || it.phase == JobPhase.Queued }
                    .forEach { OxdmBridge.resumeJob(it.id) }
            }
        }
        return START_STICKY
    }

    private fun startObserver() {
        observerJob = serviceScope.launch {
            OxdmBridge.jobs.collectLatest { jobs ->
                val activeJobs = jobs.filter { it.phase == JobPhase.Downloading }
                if (activeJobs.isNotEmpty()) {
                    acquireWakeLock()
                    updateForegroundNotification(activeJobs)
                } else {
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }
    }

    private fun updateForegroundNotification(activeJobs: List<JobUiModel>) {
        val now = System.currentTimeMillis()
        // Throttle updates to at most once per second
        if (now - lastNotificationUpdateTime < 1000) return
        lastNotificationUpdateTime = now

        val totalSpeed = activeJobs.sumOf { it.speedBps }
        val speedStr = "${JobUiModel.formatBytes(totalSpeed)}/s"
        val totalBytes = activeJobs.sumOf { it.totalBytes }
        val downloadedBytes = activeJobs.sumOf { it.downloadedBytes }
        val progress = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

        val contentTitle = resources.getQuantityString(
            R.plurals.download_active_count,
            activeJobs.size,
            activeJobs.size,
            speedStr
        )
        val contentText = if (activeJobs.size == 1) {
            "${activeJobs.first().filename} • $progress%"
        } else {
            "${JobUiModel.formatBytes(downloadedBytes)} of ${JobUiModel.formatBytes(totalBytes)} • $progress%"
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseAllIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadForegroundService::class.java).apply { action = ACTION_PAUSE_ALL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, OxdmApplication.CHANNEL_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setProgress(100, progress, totalBytes <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.action_pause), pauseAllIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire(6 * 60 * 60 * 1000L /* 6 hours max safety */)
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_PAUSE_ALL = "com.oxdm.action.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.oxdm.action.RESUME_ALL"

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
