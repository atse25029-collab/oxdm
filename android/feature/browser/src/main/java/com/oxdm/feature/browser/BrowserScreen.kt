package com.oxdm.feature.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

data class SniffedMedia(
    val url: String,
    val type: String,
    val title: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    onClose: () -> Unit,
    onDownloadRequested: (url: String, filename: String?, cookies: String?, referrer: String?, mimeType: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Multi-Tab state
    val tabs = remember { mutableStateListOf(TabModel(url = "https://google.com", title = "Google")) }
    var activeTabIndex by remember { mutableStateOf(0) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Active tab and page state
    val activeTab = tabs.getOrNull(activeTabIndex) ?: tabs.first()
    var inputUrl by remember(activeTab.id) { mutableStateOf(activeTab.url) }
    var pageProgress by remember(activeTab.id) { mutableFloatStateOf(0f) }

    // Sniffed media state
    val sniffedMediaList = remember { mutableStateListOf<SniffedMedia>() }
    var showSniffedMediaSheet by remember { mutableStateOf(false) }

    // Interception callback
    val onDownload: (String, String?, String?, String?, String?) -> Unit = { url, filename, cookies, referrer, mime ->
        onDownloadRequested(url, filename, cookies, referrer, mime)
    }

    val onMediaSniffed: (String, String, String) -> Unit = { url, type, title ->
        if (sniffedMediaList.none { it.url == url }) {
            sniffedMediaList.add(SniffedMedia(url, type, title))
        }
    }

    BackHandler {
        if (showTabSwitcher) {
            showTabSwitcher = false
        } else if (activeTab.webView?.canGoBack() == true) {
            activeTab.webView?.goBack()
        } else if (tabs.size > 1) {
            tabs.removeAt(activeTabIndex)
            activeTabIndex = (activeTabIndex - 1).coerceAtLeast(0)
        } else {
            onClose()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            singleLine = true,
                            placeholder = { Text("Search or type URL") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Browser")
                        }
                    },
                    actions = {
                        // Sniffed media badge icon
                        if (sniffedMediaList.isNotEmpty()) {
                            IconButton(onClick = { showSniffedMediaSheet = true }) {
                                BadgedBox(badge = { Badge { Text("${sniffedMediaList.size}") } }) {
                                    Icon(
                                        Icons.Default.SmartDisplay,
                                        contentDescription = "Sniffed Media",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Tab Switcher button
                        Surface(
                            onClick = { showTabSwitcher = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp).size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${tabs.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Overflow Menu
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New Tab") },
                                onClick = {
                                    showMenu = false
                                    tabs.add(TabModel())
                                    activeTabIndex = tabs.lastIndex
                                    sniffedMediaList.clear()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reload") },
                                onClick = {
                                    showMenu = false
                                    activeTab.webView?.reload()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Desktop site", modifier = Modifier.weight(1f))
                                        if (activeTab.isDesktopMode) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    activeTab.isDesktopMode = !activeTab.isDesktopMode
                                    configureDesktopMode(activeTab.webView, activeTab.isDesktopMode)
                                    activeTab.webView?.reload()
                                }
                            )
                        }
                    }
                )

                if (pageProgress in 0.01f..0.99f) {
                    LinearProgressIndicator(
                        progress = { pageProgress },
                        modifier = Modifier.fillMaxWidth().height(2.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Render active WebView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    createConfiguredWebView(
                        context = ctx,
                        tab = activeTab,
                        onUrlChanged = { url -> inputUrl = url },
                        onProgressChanged = { prog -> pageProgress = prog },
                        onDownload = onDownload,
                        onMediaSniffed = onMediaSniffed
                    ).also { activeTab.webView = it }
                },
                update = { webView ->
                    activeTab.webView = webView
                }
            )

            // Tab Switcher Modal
            if (showTabSwitcher) {
                TabSwitcherSheet(
                    tabs = tabs,
                    activeTabIndex = activeTabIndex,
                    onSelectTab = { index ->
                        activeTabIndex = index
                        showTabSwitcher = false
                    },
                    onCloseTab = { index ->
                        if (tabs.size > 1) {
                            tabs.removeAt(index)
                            if (activeTabIndex >= tabs.size) {
                                activeTabIndex = tabs.lastIndex
                            }
                        } else {
                            tabs[0] = TabModel()
                        }
                    },
                    onNewTab = {
                        tabs.add(TabModel())
                        activeTabIndex = tabs.lastIndex
                        showTabSwitcher = false
                        sniffedMediaList.clear()
                    },
                    onDismiss = { showTabSwitcher = false }
                )
            }

            // Sniffed Media Bottom Sheet
            if (showSniffedMediaSheet) {
                SniffedMediaSheet(
                    mediaList = sniffedMediaList,
                    onDownloadMedia = { media ->
                        onDownload(media.url, null, CookieManager.getInstance().getCookie(media.url), activeTab.url, null)
                        showSniffedMediaSheet = false
                    },
                    onDismiss = { showSniffedMediaSheet = false }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: android.content.Context,
    tab: TabModel,
    onUrlChanged: (String) -> Unit,
    onProgressChanged: (Float) -> Unit,
    onDownload: (String, String?, String?, String?, String?) -> Unit,
    onMediaSniffed: (String, String, String) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // 1. Session, Logins & Cookie Persistence Configuration
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = true
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // 2. Inject oxdm Kiwi-style Extension Bridge
        addJavascriptInterface(
            OxdmExtensionBridge(onDownload, onMediaSniffed),
            OxdmExtensionBridge.JS_INTERFACE_NAME
        )

        // 3. Web Clients
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress / 100f)
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { tab.title = it }
            }
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                tab.favicon = icon
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let {
                    tab.url = it
                    onUrlChanged(it)
                }
                // Inject our extension script into the page!
                view?.evaluateJavascript(OxdmExtensionBridge.INJECTED_EXTENSION_SCRIPT, null)
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val uri = request?.url?.toString()
                if (uri != null) {
                    val lower = uri.lowercase()
                    if (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains(".mp4")) {
                        onMediaSniffed(uri, "Stream/Video", tab.title)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        // 4. Download Listener Interception
        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
            val cookies = CookieManager.getInstance().getCookie(url)
            onDownload(url, null, cookies, tab.url, mimetype)
        }

        loadUrl(tab.url)
    }
}

private fun configureDesktopMode(webView: WebView?, isDesktop: Boolean) {
    if (webView == null) return
    val defaultAgent = WebSettings.getDefaultUserAgent(webView.context)
    if (isDesktop) {
        webView.settings.userAgentString = defaultAgent.replace("Android", "X11").replace("Mobile", "")
    } else {
        webView.settings.userAgentString = defaultAgent
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<TabModel>,
    activeTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Open Tabs (${tabs.size})", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onNewTab) {
                    Icon(Icons.Default.Add, contentDescription = "New Tab")
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                items(tabs.indices.toList()) { index ->
                    val tab = tabs[index]
                    val isSelected = index == activeTabIndex
                    Card(
                        onClick = { onSelectTab(index) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tab.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tab.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onCloseTab(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Tab")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SniffedMediaSheet(
    mediaList: List<SniffedMedia>,
    onDownloadMedia: (SniffedMedia) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Sniffed Media & Streams (${mediaList.size})", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                items(mediaList) { media ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(media.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(media.url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDownloadMedia(media) }) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
