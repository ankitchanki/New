package com.vibe.stashdrop.features.stashshelf

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

object UrlPrivacySanitizer {
    private val tracking = setOf("fbclid", "gclid", "dclid", "si", "igshid", "ref", "msclkid", "mc_cid", "mc_eid")
    fun clean(value: String): String {
        return runCatching {
            val uri = URI(value)
            if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.rawAuthority == null) return value
            val query = uri.rawQuery ?: return value
            val kept = query.split('&').filter { part ->
                val key = URLDecoder.decode(part.substringBefore('='), "UTF-8").lowercase(Locale.ROOT)
                !key.startsWith("utm_") && key !in tracking
            }.joinToString("&")
            buildString {
                append(uri.scheme); append("://"); append(uri.rawAuthority); append(uri.rawPath.orEmpty())
                if (kept.isNotEmpty()) { append('?'); append(kept) }
                uri.rawFragment?.let { append('#'); append(it) }
            }
        }.getOrDefault(value)
    }
    fun cleanText(text: String): String = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)
        .replace(text) { match ->
            var url = match.value
            var suffix = ""
            while (url.isNotEmpty()) {
                val last = url.last()
                val punctuation = last in ".,!?;:"
                val unmatched = (last == ')' && url.count { it == ')' } > url.count { it == '(' }) ||
                    (last == ']' && url.count { it == ']' } > url.count { it == '[' })
                if (!punctuation && !unmatched) break
                suffix = last + suffix; url = url.dropLast(1)
            }
            clean(url) + suffix
        }
}
