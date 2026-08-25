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

    // --- footerLines: the primitive the other two are built on --------------------

    @Test
    fun plainTextIsOneLine() {
        assertEquals(listOf("Mohon bersiap."), footerLines("Mohon bersiap."))
    }

    @Test
    fun aJsonArrayBecomesItsElements() {
        assertEquals(listOf("a", "b", "c"), footerLines("""["a","b","c"]"""))
    }

    @Test
    fun blankIsNoLinesAtAll() {
        // Distinct from one empty line: an empty footer should render nothing, not a
        // bullet with nothing either side of it.
        assertEquals(emptyList(), footerLines(""))
        assertEquals(emptyList(), footerLines("   "))
    }

    @Test
    fun malformedJsonSurvivesAsOneVerbatimLine() {
        assertEquals(listOf("[penting] mohon tenang"), footerLines("[penting] mohon tenang"))
        assertEquals(listOf("""["belum selesai"""), footerLines("""["belum selesai"""))
    }

    // --- encodeFooterLines, and the round trip -----------------------------------

    @Test
    fun encodingThenParsingReturnsTheSameLines() {
        listOf(
            listOf("satu"),
            listOf("satu", "dua"),
            listOf("Jum'at \"berkah\"", "koma, di dalam", "[bracket]"),
            emptyList()
        ).forEach { lines ->
            assertEquals(lines, footerLines(encodeFooterLines(lines)))
        }
    }

    @Test
    fun encodingProducesTheArrayShapeThePanelWrites() {
        assertEquals("""["a","b"]""", encodeFooterLines(listOf("a", "b")))
    }

    @Test
    fun aLineStartingWithABracketStillRoundTrips() {
        // Format sniffing keys on a leading '[', so a line that itself starts with one
        // is the case most likely to be mis-read.
        val lines = listOf("[penting] mohon tenang")
        assertEquals(lines, footerLines(encodeFooterLines(lines)))
    }

    // --- joinFooterLines is derived now; prove it did not change ------------------

    @Test
    fun joiningAgreesWithParsingThenJoining() {
        listOf(
            "Mohon bersiap dan hentikan aktivitas sementara.",
            """["Mari rapatkan shaf.","Matikan atau senyapkan ponsel."]""",
            """["Satu baris saja."]""",
            "[]",
            "[penting] mohon tenang",
            """["belum selesai""",
            ""
        ).forEach { raw ->
            assertEquals(footerLines(raw).joinToString("   •   "), joinFooterLines(raw))
        }
    }
}
