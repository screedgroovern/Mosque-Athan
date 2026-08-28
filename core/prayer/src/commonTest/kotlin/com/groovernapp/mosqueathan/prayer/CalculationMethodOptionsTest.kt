package com.groovernapp.mosqueathan.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.groovernapp.mosqueathan.domain.calculationMethodLabel
import com.groovernapp.mosqueathan.domain.calculationMethodOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The method ids are offered to the user, stored in the config, and then handed to adhan2. A typo
 * in one would not fail anywhere: `PrayerCalculator` catches the unknown name and silently falls
 * back to SINGAPORE, so the mosque would show times for the wrong convention and nothing would
 * say so. That is what this pins.
 */
class CalculationMethodOptionsTest {

    @Test
    fun everyOfferedIdIsAMethodAdhanKnows() {
        val adhanNames = CalculationMethod.entries.map { it.name }.toSet()
        val unknown = calculationMethodOptions
            .map { it.id }
            // KEMENAG is this app's own source — pre-fetched Indonesian schedules, handled before
            // adhan2 is consulted at all — so it is deliberately not an adhan2 method.
            .filter { it != "KEMENAG" }
            .filterNot { it in adhanNames }
        assertTrue(unknown.isEmpty(), "ids adhan2 would reject and silently replace: $unknown")
    }

    @Test
    fun everyOptionHasAReadableNameRatherThanItsIdentifier() {
        calculationMethodOptions.forEach { option ->
            assertNotEquals(
                option.id,
                option.label,
                "${option.id} is still showing its identifier as its label"
            )
            assertTrue(
                option.label.isNotBlank() && !option.label.contains('_'),
                "${option.id} has a label that still reads like an identifier: '${option.label}'"
            )
        }
    }

    @Test
    fun idsAreUnique() {
        val ids = calculationMethodOptions.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate method ids: $ids")
    }

    @Test
    fun anUnknownIdIsShownAsItselfRatherThanHidden() {
        // A config carrying a value this build does not offer must stay visible to the user, so the
        // fallback to SINGAPORE is something they can notice instead of a blank row.
        assertEquals("MOON_SIGHTING_COMMITTEE", calculationMethodLabel("MOON_SIGHTING_COMMITTEE"))
        assertEquals("", calculationMethodLabel(""))
    }
}
