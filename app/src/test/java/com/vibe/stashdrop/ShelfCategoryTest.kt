package com.vibe.stashdrop

import com.vibe.stashdrop.features.stashshelf.*
import org.junit.Assert.*
import org.junit.Test

class ShelfCategoryTest {
    private fun text(value:String)=StashItemEntity("t",value,null,"text/plain")
    private fun file(mime:String,collection:String="Inbox")=StashItemEntity("f",null,"saved",mime,collection)
    @Test fun mediaUsesMimeAndStickerCollection() {
        assertEquals(ShelfCategory.IMAGES,file("image/jpeg").category())
        assertEquals(ShelfCategory.VIDEOS,file("video/mp4").category())
        assertEquals(ShelfCategory.STICKERS,file("image/webp").category())
        assertEquals(ShelfCategory.STICKERS,file("image/gif").category())
        assertEquals(ShelfCategory.STICKERS,file("image/png","Stickers").category())
        assertEquals(ShelfCategory.FILES,file("application/pdf").category())
        assertEquals(ShelfCategory.IMAGES,file("image/webp","Photos").category())
    }
    @Test fun urlInProseWinsOverDigitsAndOtpKeywords() {
        assertEquals(ShelfCategory.URLS,text("Verify at https://example.com/otp/123456").category())
        assertEquals(ShelfCategory.URLS,text("Reading: HTTPS://example.com").category())
    }
    @Test fun codesRequireStandaloneDigitsOrOtpContext() {
        for(value in listOf("654321","Your OTP is 4589","Verification code: 12345678","ओटीपी 123456"))
            assertEquals(value,ShelfCategory.OTP,text(value).category())
        for(value in listOf("Budget for 2026","Call me at 1234567890","Note without code","OTP unavailable"))
            assertEquals(value,ShelfCategory.TEXT,text(value).category())
    }
    @Test fun filtersAreExclusiveExceptAllAndDoNotModifyItems() {
        val item=file("image/webp")
        assertTrue(ShelfCategory.ALL.matches(item));assertTrue(ShelfCategory.STICKERS.matches(item))
        assertFalse(ShelfCategory.IMAGES.matches(item))
        assertEquals("Inbox",item.collection)
    }
}
