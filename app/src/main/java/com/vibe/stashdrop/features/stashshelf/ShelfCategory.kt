package com.vibe.stashdrop.features.stashshelf

/** Derived from existing content; no database migration or destructive reclassification. */
enum class ShelfCategory(val label: String) {
    ALL("All"), IMAGES("Images"), VIDEOS("Videos"), STICKERS("Stickers"), URLS("URLs"), OTP("OTP"), TEXT("Text"), FILES("Files");
    fun matches(item: StashItemEntity) = this == ALL || item.category() == this
}
private val webUrl = Regex("https?://[^\\s]+",RegexOption.IGNORE_CASE)
private val code = Regex("(?<![\\d])\\d{4,8}(?![\\d])")
private val codeWords = Regex("\\b(otp|verification|verify|passcode|one[ -]?time|security code|login code|authentication)\\b|ओटीपी|सत्यापन",RegexOption.IGNORE_CASE)
fun StashItemEntity.category(): ShelfCategory = when {
    fileName != null && mimeType.startsWith("video/") -> ShelfCategory.VIDEOS
    fileName != null && mimeType.startsWith("image/") && (collection.equals("Images",true) || collection.equals("Photos",true)) -> ShelfCategory.IMAGES
    fileName != null && mimeType.startsWith("image/") && (collection.equals("Stickers",true) || mimeType in setOf("image/webp","image/gif")) -> ShelfCategory.STICKERS
    fileName != null && mimeType.startsWith("image/") -> ShelfCategory.IMAGES
    fileName != null -> ShelfCategory.FILES
    webUrl.containsMatchIn(text.orEmpty()) -> ShelfCategory.URLS
    text.orEmpty().trim().matches(Regex("\\d{4,8}")) || (codeWords.containsMatchIn(text.orEmpty()) && code.containsMatchIn(text.orEmpty())) -> ShelfCategory.OTP
    else -> ShelfCategory.TEXT
}
