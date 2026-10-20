package com.oxdm.feature.browser

import android.graphics.Bitmap
import android.webkit.WebView
import java.util.UUID

data class TabModel(
    val id: String = UUID.randomUUID().toString(),
    var url: String = "https://google.com",
    var title: String = "New Tab",
    var favicon: Bitmap? = null,
    var isIncognito: Boolean = false,
    var isDesktopMode: Boolean = false,
    var webView: WebView? = null
)
