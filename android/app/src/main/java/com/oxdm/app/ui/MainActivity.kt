package com.oxdm.app.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.oxdm.app.service.ClipboardMonitorService
import com.oxdm.app.service.DownloadForegroundService
import com.oxdm.app.service.FloatingDownloadBubbleService
import com.oxdm.app.ui.screens.MainDashboardScreen
import com.oxdm.app.ui.theme.OxdmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Ensure services are running
        DownloadForegroundService.start(this)
        ClipboardMonitorService.start(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            FloatingDownloadBubbleService.start(this)
        }

        // 2. Request essential runtime permissions
        requestPermissionsIfNeeded()

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
                    },
                    onSetDefaultBrowser = { requestDefaultBrowser() },
                    onRequestNotificationListener = { openNotificationListenerSettings() },
                    onRequestOverlay = { requestOverlayPermission() }
                )
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Android 11+ All Files Access
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    fun requestDefaultBrowser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) && !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                startActivity(roleIntent)
                return
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openNotificationListenerSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            FloatingDownloadBubbleService.start(this)
        }
    }
}
