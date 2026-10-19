package com.vibe.stashdrop

import com.vibe.stashdrop.features.stashshelf.UrlPrivacySanitizer
import org.junit.Assert.*
import org.junit.Test

class UrlPrivacySanitizerTest {
    @Test fun removesTrackingWithoutReencodingContent() {
        assertEquals("https://example.com/a%2Fb?q=a%26b&q=c#part%201",
            UrlPrivacySanitizer.clean("https://example.com/a%2Fb?utm_source=x&q=a%26b&fbclid=123&q=c#part%201"))
    }
    @Test fun handlesEncodedAndCaseInsensitiveKeys() {
        assertEquals("https://example.com/?keep=1",UrlPrivacySanitizer.clean("https://example.com/?%75tm_source=a&FBCLID=b&keep=1"))
    }
    @Test fun removesEmptyQueryButKeepsFragment() {
        assertEquals("https://example.com/#anchor",UrlPrivacySanitizer.clean("https://example.com/?si=x&gclid=y#anchor"))
    }
    @Test fun preservesNonHttpAndMalformedInput() {
        for(value in listOf("some text", "mailto:a@example.com?ref=keep", "https://example.com/?q=%ZZ", "content://media/1?ref=keep")) assertEquals(value,UrlPrivacySanitizer.clean(value))
    }
    @Test fun sanitizesMultipleLinksInsideText() {
        assertEquals("Read https://a.com/x and https://b.com/?q=2",UrlPrivacySanitizer.cleanText("Read https://a.com/x?utm_campaign=c and https://b.com/?si=1&q=2"))
    }
    @Test fun preservesProsePunctuation() {
        assertEquals("See (https://example.com/).",UrlPrivacySanitizer.cleanText("See (https://example.com/?utm_source=x)."))
        assertEquals("https://example.com/wiki/Thing_(one)",UrlPrivacySanitizer.cleanText("https://example.com/wiki/Thing_(one)?utm_source=x"))
    }
    @Test fun keepsNonTrackingQueryValues() {
        assertEquals("https://a.com/?query=utm_source&signal=si",UrlPrivacySanitizer.clean("https://a.com/?query=utm_source&signal=si"))
    }
}
