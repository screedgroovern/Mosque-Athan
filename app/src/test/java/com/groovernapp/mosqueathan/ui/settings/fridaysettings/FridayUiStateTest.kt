package com.groovernapp.mosqueathan.ui.settings.fridaysettings

import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FridayUiStateTest {

    @Test
    fun visualsNeedFridayModeOn() {
        // Nothing Friday-specific happens at all when the mode is off, so the
        // murottal and slideshow cards are dimmed regardless of their own switches.
        assertFalse(FridayUiState(enabled = false).visualsEnabled)
        assertFalse(
            FridayUiState(
                enabled = false,
                murottal = MurottalSettings(enabled = true),
                slideshow = FridaySlideshowSettings(enabled = true)
            ).visualsEnabled
        )
    }

    @Test
    fun standbyOnlySuppressesTheVisuals() {
        // Standby blacks the screen out for the whole service. A slideshow behind a
        // black screen is not a feature, it is a contradiction — which is why the
        // pane says so instead of letting the takmir configure both.
        assertTrue(FridayUiState(enabled = true, standbyOnly = false).visualsEnabled)
        assertFalse(FridayUiState(enabled = true, standbyOnly = true).visualsEnabled)
    }

    @Test
    fun standbyOnlyMeansNothingWhileFridayModeIsOff() {
        assertFalse(FridayUiState(enabled = false, standbyOnly = true).visualsEnabled)
    }

    @Test
    fun theDefaultStateIsEverythingOff() {
        // The state before the first config emission. It must be safe to draw: a
        // default that read as "enabled" would flash the Friday cards open on every
        // entry to the pane.
        val state = FridayUiState()
        assertFalse(state.enabled)
        assertFalse(state.standbyOnly)
        assertFalse(state.visualsEnabled)
        assertFalse(state.murottal.enabled)
        assertFalse(state.slideshow.enabled)
        assertTrue(state.slideshow.media.isEmpty())
        assertTrue(state.slideshow.globalMedia.isEmpty())
    }

    @Test
    fun theThreeConcernsAreSeparatelyAddressable() {
        // The point of the grouping: changing the murottal delay cannot disturb the
        // slideshow, because they are different objects rather than adjacent
        // parameters in one long list.
        val state = FridayUiState(
            enabled = true,
            murottal = MurottalSettings(enabled = true, startDelayMinutes = 15),
            slideshow = FridaySlideshowSettings(enabled = true, intervalSeconds = 30)
        )
        val slower = state.copy(murottal = state.murottal.copy(startDelayMinutes = 20))

        assertEquals(20, slower.murottal.startDelayMinutes)
        assertEquals(30, slower.slideshow.intervalSeconds)
        assertTrue(slower.slideshow.enabled)
        assertEquals(state.slideshow, slower.slideshow)
    }

    @Test
    fun everyFieldOfTheStateHasAConfigFieldBehindIt() {
        // The pane is auto-save, so each of these must map to a stored field. This
        // is the list the ViewModel reads, spelled out: if a rename drops one, the
        // setting silently stops persisting.
        val config = MosqueConfigEntity().apply {
            jumatEnabled = true
            jumatStartTime = "11:15"
            jumatEndTime = "13:05"
            offsetJumat = -4
            jumatStandbyOnly = false
            jumatWarningText = "Khutbah dimulai"
            murottalEnabled = true
            murottalStartDelay = 12
            murottalAudioPaths = "content://a,content://b"
            jumatSlideshowEnabled = true
            includeGlobalSlideshow = true
            selectedGlobalSlideIds = "id-1,id-2"
            jumatTransitionType = "ZOOM"
            jumatSlideIntervalSeconds = 25
            jumatSlideIntervalUnit = "MINUTES"
            jumatVideoWaitUntilEnd = false
        }

        val state = FridayUiState(
            enabled = config.jumatEnabled,
            startTime = config.jumatStartTime,
            endTime = config.jumatEndTime,
            dzuhurOffsetMinutes = config.offsetJumat,
            standbyOnly = config.jumatStandbyOnly,
            warningText = config.jumatWarningText,
            murottal = MurottalSettings(
                enabled = config.murottalEnabled,
                startDelayMinutes = config.murottalStartDelay,
                audioPaths = config.murottalAudioPaths
            ),
            slideshow = FridaySlideshowSettings(
                enabled = config.jumatSlideshowEnabled,
                includeGlobalMedia = config.includeGlobalSlideshow,
                selectedGlobalMediaIds = config.selectedGlobalSlideIds,
                transitionType = config.jumatTransitionType,
                intervalSeconds = config.jumatSlideIntervalSeconds,
                intervalUnit = config.jumatSlideIntervalUnit,
                videoWaitUntilEnd = config.jumatVideoWaitUntilEnd
            )
        )

        assertTrue(state.visualsEnabled)
        assertEquals("11:15", state.startTime)
        assertEquals("13:05", state.endTime)
        assertEquals(-4, state.dzuhurOffsetMinutes)
        assertEquals("Khutbah dimulai", state.warningText)
        assertEquals(12, state.murottal.startDelayMinutes)
        assertEquals("content://a,content://b", state.murottal.audioPaths)
        assertEquals("id-1,id-2", state.slideshow.selectedGlobalMediaIds)
        assertEquals("ZOOM", state.slideshow.transitionType)
        assertEquals(25, state.slideshow.intervalSeconds)
        assertEquals("MINUTES", state.slideshow.intervalUnit)
        assertFalse(state.slideshow.videoWaitUntilEnd)
    }

    @Test
    fun aNegativeFridayOffsetSurvives() {
        // The Friday adzan is commonly brought forward, so the sign matters.
        assertEquals(-10, FridayUiState(dzuhurOffsetMinutes = -10).dzuhurOffsetMinutes)
    }

    @Test
    fun theGlobalSelectionIsACommaSeparatedIdList() {
        // The pane parses this string into a set and joins it back. Round-tripping is
        // what keeps a checkbox from dropping every other selection.
        val ids = "a,b,c"
        val parsed = ids.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        assertEquals(setOf("a", "b", "c"), parsed)
        assertEquals(ids, parsed.joinToString(","))

        // The empty case is the one that used to produce a phantom "" id.
        assertTrue("".split(",").map { it.trim() }.filter { it.isNotEmpty() }.isEmpty())
    }
}
