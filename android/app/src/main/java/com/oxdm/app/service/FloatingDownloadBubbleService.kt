package com.oxdm.app.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.oxdm.app.ui.AddDownloadActivity

/**
 * Floating on-screen quick download bubble. Stays accessible while browsing
 * in Chrome or any app, allowing 1-tap download of the copied or active link.
 */
class FloatingDownloadBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val bubble = ImageView(this).apply {
            setImageResource(android.R.drawable.stat_sys_download)
            setBackgroundColor(0xFFE07A5F.toInt())
            setPadding(24, 24, 24, 24)
            elevation = 16f
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = Math.abs(event.rawX - initialTouchX)
                    val diffY = Math.abs(event.rawY - initialTouchY)
                    // If tap rather than drag
                    if (diffX < 15 && diffY < 15) {
                        onBubbleClicked()
                    }
                    true
                }
                else -> false
            }
        }

        floatingBubbleView = bubble
        windowManager?.addView(bubble, params)
    }

    private fun onBubbleClicked() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip
        val copiedText = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0)?.text?.toString()?.trim()
        } else null

        val intent = Intent(this, AddDownloadActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            if (copiedText != null && (copiedText.startsWith("http://") || copiedText.startsWith("https://") || copiedText.startsWith("magnet:"))) {
                data = Uri.parse(copiedText)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        floatingBubbleView?.let { windowManager?.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FloatingDownloadBubbleService::class.java)
            context.startService(intent)
        }
    }
}
