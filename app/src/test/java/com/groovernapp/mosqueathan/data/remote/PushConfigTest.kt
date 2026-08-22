package com.groovernapp.mosqueathan.data.remote

import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `update_config` payload is what makes a change on a controller device reach
 * the display. If a field is missing from it, the setting appears to save and
 * silently never takes effect on the TV — a failure with no error anywhere.
 *
 * These tests exist because that regression actually happened: three category
 * ViewModels were written to call the repository directly and skipped the push
 * entirely.
 */
class PushConfigTest {

    private fun payloadFor(config: MosqueConfigEntity) =
        JSONObject().apply { pushConfig(config) }

    @Test
    fun theSettingsEveryCategoryEditsAreAllPresent() {
        val payload = payloadFor(MosqueConfigEntity())
        listOf(
            // prayer schedule
            "latitude", "longitude", "calculationMethodName",
            "offsetImsak", "offsetSubuh", "offsetSyuruq", "offsetDzuhur",
            "offsetAshar", "offsetMaghrib", "offsetIsya",
            "kemenagCityId", "kemenagCityName", "kemenagDuration", "kemenagTargetDate",
            // adzan & iqomah
            "adzanLeadMinutes", "adzanFooterText", "iqomahFooterText",
            "iqomahSubuh", "iqomahDzuhur", "iqomahAshar", "iqomahMaghrib", "iqomahIsya",
            // friday
            "jumatEnabled", "offsetJumat"
        ).forEach {
            assertTrue("`$it` missing from update_config", payload.has(it))
        }
    }

    @Test
    fun valuesAreCarriedRatherThanDefaulted() {
        val config = MosqueConfigEntity().apply {
            latitude = -6.2
            iqomahMaghrib = 7
            offsetSubuh = -3
            calculationMethodName = "KEMENAG"
            adzanLeadMinutes = 5
        }
        val payload = payloadFor(config)
        assertEquals(-6.2, payload.getDouble("latitude"), 0.0)
        assertEquals(7, payload.getInt("iqomahMaghrib"))
        assertEquals(-3, payload.getInt("offsetSubuh"))
        assertEquals("KEMENAG", payload.getString("calculationMethodName"))
        assertEquals(5, payload.getInt("adzanLeadMinutes"))
    }

    @Test
    fun thePayloadIsNotEmpty() {
        // Guards the whole mechanism: an empty pushConfig would make every test
        // above pass vacuously if they only checked for absence.
        assertTrue(payloadFor(MosqueConfigEntity()).length() > 30)
    }
}
