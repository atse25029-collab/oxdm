package com.oxdm.feature.browser

import android.webkit.JavascriptInterface

/**
 * JavaScript interface injected into every web page as `window.oxdm` and `window.chrome.downloads`.
 * Functions as the in-browser oxdm extension bridge (Kiwi / WebExtension style).
 */
class OxdmExtensionBridge(
    private val onDownloadIntercepted: (url: String, filename: String?, cookies: String?, referrer: String?, mimeType: String?) -> Unit,
    private val onMediaSniffed: (url: String, type: String, title: String) -> Unit
) {

    @JavascriptInterface
    fun interceptDownload(url: String, filename: String?, cookies: String?, referrer: String?, mimeType: String?) {
        onDownloadIntercepted(url, filename, cookies, referrer, mimeType)
    }

    @JavascriptInterface
    fun sniffMedia(url: String, type: String, title: String) {
        onMediaSniffed(url, type, title)
    }

    companion object {
        const val JS_INTERFACE_NAME = "OxdmNativeBridge"

        /**
         * WebExtension & Kiwi-compatible script injected into pages.
         * Sets up `window.chrome.downloads`, monitors `<video>`/`<audio>` tags,
         * intercepts download links and network streams.
         */
        val INJECTED_EXTENSION_SCRIPT = """
            (function() {
                if (window.__oxdm_injected__) return;
                window.__oxdm_injected__ = true;

                // 1. WebExtension API Compatibility Polyfill
                window.chrome = window.chrome || {};
                window.chrome.downloads = {
                    download: function(options, callback) {
                        if (options && options.url) {
                            window.OxdmNativeBridge.interceptDownload(
                                options.url,
                                options.filename || '',
                                document.cookie || '',
                                window.location.href,
                                ''
                            );
                            if (callback) callback(Date.now());
                        }
                    }
                };
                window.browser = window.browser || {};
                window.browser.downloads = window.chrome.downloads;

                // 2. Intercept Downloadable Link Clicks
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    while (target && target.tagName !== 'A') {
                        target = target.parentElement;
                    }
                    if (!target || !target.href) return;

                    var href = target.href;
                    var downloadAttr = target.getAttribute('download');
                    var extPattern = /\.(zip|rar|7z|tar|gz|apk|iso|exe|dmg|pdf|mp4|mkv|avi|mov|mp3|flac|wav)(\?.*)?$/i;

                    if (downloadAttr !== null || extPattern.test(href)) {
                        e.preventDefault();
                        e.stopPropagation();
                        window.OxdmNativeBridge.interceptDownload(
                            href,
                            downloadAttr || '',
                            document.cookie || '',
                            window.location.href,
                            ''
                        );
                    }
                }, true);

                // 3. Media Stream Sniffer (HTML5 Video / Audio & HLS / DASH)
                function scanMedia() {
                    var videos = document.querySelectorAll('video, audio');
                    videos.forEach(function(el) {
                        var src = el.currentSrc || el.src;
                        if (src && !src.startsWith('blob:') && !src.startsWith('data:')) {
                            window.OxdmNativeBridge.sniffMedia(src, el.tagName.toLowerCase(), document.title || 'Media');
                        }
                    });
                }

                // Initial scan and observer for dynamically inserted video elements
                scanMedia();
                var observer = new MutationObserver(function() { scanMedia(); });
                observer.observe(document.body || document.documentElement, { childList: true, subtree: true });

                // 4. Sniff Network Fetch / XHR for streaming manifests (.m3u8, .mpd)
                var origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = function() {
                        var url = arguments[0];
                        if (typeof url === 'string' && (url.includes('.m3u8') || url.includes('.mpd'))) {
                            window.OxdmNativeBridge.sniffMedia(url, 'stream', document.title || 'Stream');
                        }
                        return origFetch.apply(this, arguments);
                    };
                }
            })();
        """.trimIndent()
    }
}
