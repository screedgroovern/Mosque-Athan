package com.groovernapp.mosqueathan.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Parsing checked against responses actually captured from `api.myquran.com`, not against
 * a shape I assumed it had.
 *
 * That distinction earned its keep: `data.jadwal` is an object *keyed by date* rather
 * than the array a reader would expect, so the field names are data. Code written from a
 * guess would have compiled and returned nothing.
 */
class KemenagResponseTest {

    // Captured 2026-08-26 from /v3/sholat/kabkota/cari/jakarta, verbatim.
    private val citySearchResponse =
        """{"status":true,"message":"success","data":[{"id":"58a2fc6ed39fd083f55d4182bf88826d","lokasi":"KOTA JAKARTA"}]}"""

    // Captured the same day from /v3/sholat/jadwal/58a2.../2026-08, trimmed to three days.
    private val monthResponse = """{"status": true, "message": "success", "data": {"jadwal": {
        "2026-08-01": {"tanggal": "Sabtu, 01/08/2026", "imsak": "04:35", "subuh": "04:45", "terbit": "06:00", "dhuha": "06:29", "dzuhur": "12:03", "ashar": "15:24", "maghrib": "17:58", "isya": "19:09"},
        "2026-08-02": {"tanggal": "Minggu, 02/08/2026", "imsak": "04:35", "subuh": "04:45", "terbit": "06:00", "dhuha": "06:29", "dzuhur": "12:03", "ashar": "15:24", "maghrib": "17:58", "isya": "19:09"},
        "2026-08-03": {"tanggal": "Senin, 03/08/2026", "imsak": "04:35", "subuh": "04:45", "terbit": "06:00", "dhuha": "06:29", "dzuhur": "12:03", "ashar": "15:24", "maghrib": "17:58", "isya": "19:09"}
    }}}"""

    @Test
    fun aRealCitySearchResponseParses() {
        assertEquals(
            listOf("58a2fc6ed39fd083f55d4182bf88826d" to "KOTA JAKARTA"),
            parseKemenagCities(citySearchResponse)
        )
    }

    @Test
    fun aRealMonthResponseParses() {
        val days = parseKemenagMonth("city-1", monthResponse).sortedBy { it.dateString }
        assertEquals(3, days.size)
        assertEquals("2026-08-01", days[0].dateString)
        assertEquals("city-1", days[0].cityId)
        assertEquals("04:35", days[0].imsak)
        assertEquals("04:45", days[0].subuh)
        assertEquals("06:00", days[0].terbit)
        assertEquals("06:29", days[0].dhuha)
        assertEquals("12:03", days[0].dzuhur)
        assertEquals("15:24", days[0].ashar)
        assertEquals("17:58", days[0].maghrib)
        assertEquals("19:09", days[0].isya)
    }

    @Test
    fun theApiSayingNoGivesNothing() {
        // The real refusal, quoted: /v3/sholat/jadwal/<id>/2026/8 — the wrong URL shape.
        val refusal = """{"status":false,"message":"Data tidak ditemukan .."}"""
        assertEquals(emptyList(), parseKemenagMonth("c", refusal))
        assertEquals(emptyList(), parseKemenagCities(refusal))
    }

    @Test
    fun aFailedFetchNeverThrows() {
        // A month that fails to parse must leave yesterday's stored schedule alone — the
        // display still has to show prayer times tomorrow morning.
        listOf("", "not json at all", "{}", """{"status":true}""", """{"status":true,"data":{}}""",
               """{"status":true,"data":{"jadwal":[]}}""").forEach { body ->
            assertEquals(emptyList(), parseKemenagMonth("c", body), "for: $body")
            assertEquals(emptyList(), parseKemenagCities(body), "for: $body")
        }
    }

    @Test
    fun entriesMissingAnIdOrNameAreDropped() {
        // A blank row the takmir could select by accident is worse than a shorter list.
        val partial = """{"status":true,"data":[{"id":"a","lokasi":"KOTA A"},{"id":"","lokasi":"NO ID"},{"id":"c"}]}"""
        assertEquals(listOf("a" to "KOTA A"), parseKemenagCities(partial))
    }

    @Test
    fun aDayMissingFieldsStillYieldsTheDayWithBlanks() {
        // Better a day with gaps than the whole month rejected.
        val sparse = """{"status":true,"data":{"jadwal":{"2026-08-01":{"subuh":"04:45"}}}}"""
        val days = parseKemenagMonth("c", sparse)
        assertEquals(1, days.size)
        assertEquals("04:45", days[0].subuh)
        assertEquals("", days[0].maghrib)
        assertTrue(days[0].dateString.isNotEmpty())
    }
}
