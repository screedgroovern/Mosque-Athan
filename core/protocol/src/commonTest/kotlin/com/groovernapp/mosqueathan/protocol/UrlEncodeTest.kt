package com.groovernapp.mosqueathan.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Percent-encoding for a URL *path* segment.
 *
 * Worth testing rather than trusting, because the function it replaces got this wrong:
 * `java.net.URLEncoder` does form encoding, which writes a space as `+`. In a query
 * string that is right; in a path it is a literal plus sign, and the Kemenag city search
 * puts the query in the path.
 */
class UrlEncodeTest {

    @Test
    fun unreservedCharactersAreLeftAlone() {
        assertEquals("jakarta", urlEncodePathSegment("jakarta"))
        assertEquals("a-b_c.d~e", urlEncodePathSegment("a-b_c.d~e"))
        assertEquals("Kota123", urlEncodePathSegment("Kota123"))
    }

    @Test
    fun aSpaceBecomesPercentTwentyAndNotAPlus() {
        // The whole reason this function exists rather than URLEncoder.
        assertEquals("kota%20bandung", urlEncodePathSegment("kota bandung"))
    }

    @Test
    fun reservedCharactersAreEscaped() {
        assertEquals("a%2Fb", urlEncodePathSegment("a/b"))
        assertEquals("a%3Fb", urlEncodePathSegment("a?b"))
        assertEquals("a%26b", urlEncodePathSegment("a&b"))
        assertEquals("a%2Bb", urlEncodePathSegment("a+b"))
        assertEquals("a%23b", urlEncodePathSegment("a#b"))
    }

    @Test
    fun nonAsciiIsEncodedAsUtf8Bytes() {
        // Indonesian place names carry apostrophes and, occasionally, accents.
        assertEquals("Jum%27at", urlEncodePathSegment("Jum'at"))
        assertEquals("caf%C3%A9", urlEncodePathSegment("café"))
    }

    @Test
    fun anEmptyStringEncodesToNothing() {
        assertEquals("", urlEncodePathSegment(""))
    }
}
