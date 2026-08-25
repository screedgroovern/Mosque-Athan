package com.groovernapp.mosqueathan.util

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The footer parser behaves as `org.json.JSONArray` did.
 *
 * This is a reimplementation, not a move: the old code walked a `JSONArray`, the new one
 * uses kotlinx.serialization. Both formats are live on installed devices, so the cases
 * below are the ones a real `MosqueConfigEntity` can actually hold.
 */
class FooterTextTest {

    @Test
    fun plainTextIsReturnedUnchanged() {
        assertEquals(
            "Mohon bersiap dan hentikan aktivitas sementara.",
            joinFooterLines("Mohon bersiap dan hentikan aktivitas sementara.")
        )
    }

    @Test
    fun aJsonArrayIsJoinedWithTheBulletSeparator() {
        assertEquals(
            "Mari rapatkan shaf.   •   Matikan atau senyapkan ponsel.",
            joinFooterLines("""["Mari rapatkan shaf.","Matikan atau senyapkan ponsel."]""")
        )
    }

    @Test
    fun aSingleElementArrayHasNoSeparator() {
        assertEquals("Satu baris saja.", joinFooterLines("""["Satu baris saja."]"""))
    }

    @Test
    fun anEmptyArrayBecomesAnEmptyFooter() {
        assertEquals("", joinFooterLines("[]"))
    }

    @Test
    fun textThatMerelyStartsWithABracketIsReturnedVerbatim() {
        // A takmir typing a stray bracket must see their own words on the wall, not an
        // empty footer during adzan. This is why the parser swallows its exception.
        assertEquals("[penting] mohon tenang", joinFooterLines("[penting] mohon tenang"))
    }

    @Test
    fun anUnterminatedArrayIsReturnedVerbatim() {
        assertEquals("""["belum selesai""", joinFooterLines("""["belum selesai"""))
    }

    @Test
    fun leadingWhitespaceStillCountsAsAnArray() {
        assertEquals("a   •   b", joinFooterLines("""  ["a","b"]"""))
    }

    @Test
    fun anEmptyStringStaysEmpty() {
        assertEquals("", joinFooterLines(""))
    }

    @Test
    fun unicodeAndEscapesSurviveTheRoundTrip() {
        assertEquals("""Jum'at "berkah"""", joinFooterLines("""["Jum'at \"berkah\""]"""))
    }
}
