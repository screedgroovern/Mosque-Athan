package com.groovernapp.mosqueathan.data.remote

import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
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

    private fun payloadFor(config: MosqueConfig) =
        buildJsonObject { pushConfig(config) }

    @Test
    fun theSettingsEveryCategoryEditsAreAllPresent() {
        val payload = payloadFor(MosqueConfig())
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
            assertTrue("`$it` missing from update_config", payload.containsKey(it))
        }
    }

    @Test
    fun everyFridaySettingIsPresent() {
        // The Friday category edits sixteen config fields and nothing else, so this
        // list is the whole contract between the Friday pane and the display. It is
        // spelled out rather than covered by the summary above because the pane was
        // the last one migrated, and a missing field here would look exactly like a
        // working save.
        val payload = payloadFor(MosqueConfig())
        listOf(
            "jumatEnabled", "jumatStartTime", "jumatEndTime", "offsetJumat",
            "jumatStandbyOnly", "jumatWarningText",
            "murottalEnabled", "murottalStartDelay", "murottalAudioPaths",
            "jumatSlideshowEnabled", "includeGlobalSlideshow", "selectedGlobalSlideIds",
            "jumatTransitionType", "jumatSlideIntervalSeconds", "jumatSlideIntervalUnit",
            "jumatVideoWaitUntilEnd"
        ).forEach {
            assertTrue("`$it` missing from update_config", payload.containsKey(it))
        }
    }

    @Test
    fun fridayValuesAreCarriedRatherThanDefaulted() {
        val config = MosqueConfig(jumatEnabled = true, offsetJumat = -4, jumatStandbyOnly = true, murottalStartDelay = 12, jumatSlideIntervalUnit = "MINUTES", selectedGlobalSlideIds = "id-1,id-2")
        val payload = payloadFor(config)
        assertTrue(payload.boolAt("jumatEnabled"))
        assertEquals(-4, payload.intAt("offsetJumat"))
        assertTrue(payload.boolAt("jumatStandbyOnly"))
        assertEquals(12, payload.intAt("murottalStartDelay"))
        assertEquals("MINUTES", payload.stringAt("jumatSlideIntervalUnit"))
        assertEquals("id-1,id-2", payload.stringAt("selectedGlobalSlideIds"))
    }

    @Test
    fun valuesAreCarriedRatherThanDefaulted() {
        val config = MosqueConfig(latitude = -6.2, iqomahMaghrib = 7, offsetSubuh = -3, calculationMethodName = "KEMENAG", adzanLeadMinutes = 5)
        val payload = payloadFor(config)
        assertEquals(-6.2, payload.doubleAt("latitude"), 0.0)
        assertEquals(7, payload.intAt("iqomahMaghrib"))
        assertEquals(-3, payload.intAt("offsetSubuh"))
        assertEquals("KEMENAG", payload.stringAt("calculationMethodName"))
        assertEquals(5, payload.intAt("adzanLeadMinutes"))
    }

    @Test
    fun thePayloadIsNotEmpty() {
        // Guards the whole mechanism: an empty pushConfig would make every test
        // above pass vacuously if they only checked for absence.
        assertTrue(payloadFor(MosqueConfig()).size > 30)
    }

    // Small readers so the assertions above stay as readable as they were with
    // org.json. kotlinx.serialization exposes values as JsonPrimitive rather than
    // typed getters.
    private fun kotlinx.serialization.json.JsonObject.stringAt(key: String) =
        getValue(key).jsonPrimitive.content

    private fun kotlinx.serialization.json.JsonObject.intAt(key: String) =
        getValue(key).jsonPrimitive.int

    private fun kotlinx.serialization.json.JsonObject.doubleAt(key: String) =
        getValue(key).jsonPrimitive.double

    private fun kotlinx.serialization.json.JsonObject.boolAt(key: String) =
        getValue(key).jsonPrimitive.boolean
}
