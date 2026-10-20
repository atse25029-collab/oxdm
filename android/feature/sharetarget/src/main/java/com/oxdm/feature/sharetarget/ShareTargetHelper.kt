package com.oxdm.feature.sharetarget

import android.net.Uri

object ShareTargetHelper {

    /**
     * Extracts and cleans URLs from arbitrary shared text blocks (e.g. from YouTube, Twitter, Chrome).
     */
    fun extractUrl(text: String?): String? {
        if (text == null) return null

        val words = text.split("\\s+".toRegex())
        for (word in words) {
            if (word.startsWith("http://", ignoreCase = true) || word.startsWith("https://", ignoreCase = true)) {
                return sanitizeUrl(word)
            }
        }
        return null
    }

    private fun sanitizeUrl(rawUrl: String): String {
        return try {
            val uri = Uri.parse(rawUrl)
            // Strip tracking queries if present
            uri.toString()
        } catch (e: Exception) {
            rawUrl
        }
    }
}
