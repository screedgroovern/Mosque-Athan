package com.groovernapp.mosqueathan.prayer

import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.util.PrayerCalculator
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrayerCalculatorTest {

    private val zone = TimeZone.currentSystemDefault()

    @Test
    fun nextPrayerIsDzuhurMidMorning() {
        // June 4 2026, 10:00 — after Subuh/Syuruq, before Dzuhur
        val now = LocalDateTime(2026, 6, 4, 10, 0, 0).toInstant(zone)

        val result = PrayerCalculator.calculate(now)

        assertEquals(PrayerType.DZUHUR, result.nextPrayer)
        assertTrue(
            result.nextPrayerTime.toInstant(zone) > now,
            "next prayer ${result.nextPrayerTime} should be after $now"
        )
    }

    @Test
    fun nextPrayerAfterIshaIsTomorrowImsak() {
        val now = LocalDateTime(2026, 6, 4, 21, 0, 0).toInstant(zone)

        val result = PrayerCalculator.calculate(now)

        assertEquals(PrayerType.IMSAK, result.nextPrayer)
        assertEquals(2026, result.nextPrayerTime.year)
        assertEquals(6, result.nextPrayerTime.month.number)
        assertEquals(5, result.nextPrayerTime.day)
        assertTrue(result.nextPrayerTime.toInstant(zone) > now)
    }

    @Test
    fun imsakDefaultsToTenMinutesBeforeSubuh() {
        val now = LocalDateTime(2026, 6, 5, 10, 0, 0).toInstant(zone)

        val result = PrayerCalculator.calculate(
            now = now,
            calculationMethodName = "SINGAPORE",
            offsetImsak = 0,
            offsetSubuh = 0
        )

        assertEquals(
            result.subuh.toInstant(zone).toEpochMilliseconds() - 10 * 60 * 1000,
            result.imsak.toInstant(zone).toEpochMilliseconds()
        )
    }

    @Test
    fun imsakOffsetStacksOnTopOfSubuhOffset() {
        val now = LocalDateTime(2026, 6, 5, 10, 0, 0).toInstant(zone)

        val result = PrayerCalculator.calculate(
            now = now,
            calculationMethodName = "SINGAPORE",
            offsetImsak = 3,
            offsetSubuh = 2
        )

        // imsak = (subuh incl. its own offset) - 10 + offsetImsak
        assertEquals(
            result.subuh.toInstant(zone).toEpochMilliseconds() - 10 * 60 * 1000 + 3 * 60 * 1000,
            result.imsak.toInstant(zone).toEpochMilliseconds()
        )
    }

    @Test
    fun unknownCalculationMethodFallsBackToSingapore() {
        val now = LocalDateTime(2026, 6, 5, 10, 0, 0).toInstant(zone)

        val fallback = PrayerCalculator.calculate(now, calculationMethodName = "NOT_A_METHOD")
        val singapore = PrayerCalculator.calculate(now, calculationMethodName = "SINGAPORE")

        assertEquals(singapore.subuh, fallback.subuh)
        assertEquals(singapore.maghrib, fallback.maghrib)
    }

    // --- Kemenag path -------------------------------------------------------
    // Previously this needed a live Realm database. With the schedule source
    // injected it is an ordinary unit test.

    private val jakarta = "58a2fc6ed39fd083f55d4182bf88826d"

    private fun schedule(date: String, subuh: String, imsak: String) = KemenagSchedule(
        dateString = date,
        cityId = jakarta,
        imsak = imsak,
        subuh = subuh,
        terbit = "05:54",
        dhuha = "06:24",
        dzuhur = "11:55",
        ashar = "15:16",
        maghrib = "17:48",
        isya = "19:02"
    )

    private val source = KemenagScheduleSource { cityId, date ->
        if (cityId != jakarta) return@KemenagScheduleSource null
        when (date) {
            "2026-06-05" -> schedule(date, subuh = "04:37", imsak = "04:27")
            "2026-06-06" -> schedule(date, subuh = "04:38", imsak = "04:27")
            else -> null
        }
    }

    @Test
    fun kemenagScheduleIsUsedVerbatim() {
        val now = LocalDateTime(2026, 6, 5, 10, 0, 0).toInstant(zone)

        val result = PrayerCalculator.calculate(
            now = now,
            calculationMethodName = "KEMENAG",
            kemenagCityId = jakarta,
            kemenagSource = source
        )

        assertEquals(4 to 27, result.imsak.hour to result.imsak.minute)
        assertEquals(4 to 37, result.subuh.hour to result.subuh.minute)
        assertEquals(11 to 55, result.dzuhur.hour to result.dzuhur.minute)
        assertEquals(15 to 16, result.ashar.hour to result.ashar.minute)
        assertEquals(17 to 48, result.maghrib.hour to result.maghrib.minute)
        assertEquals(19 to 2, result.isya.hour to result.isya.minute)

        assertEquals(PrayerType.DZUHUR, result.nextPrayer)
        assertEquals(11 to 55, result.nextPrayerTime.hour to result.nextPrayerTime.minute)
    }

    @Test
    fun kemenagAfterIshaRollsToTomorrowImsak() {
        val now = LocalDateTime(2026, 6, 5, 21, 0, 0).toInstant(zone)

        val result = PrayerCalculator.calculate(
            now = now,
            calculationMethodName = "KEMENAG",
            kemenagCityId = jakarta,
            kemenagSource = source
        )

        assertEquals(PrayerType.IMSAK, result.nextPrayer)
        assertEquals(6, result.nextPrayerTime.day)
        assertEquals(4 to 27, result.nextPrayerTime.hour to result.nextPrayerTime.minute)
    }

    @Test
    fun kemenagWithoutASourceFallsBackToCalculatedTimes() {
        val now = LocalDateTime(2026, 6, 5, 10, 0, 0).toInstant(zone)

        val withoutSource = PrayerCalculator.calculate(
            now = now,
            calculationMethodName = "KEMENAG",
            kemenagCityId = jakarta,
            kemenagSource = null
        )

        // "KEMENAG" is not an adhan2 method, so it falls back to SINGAPORE rather
        // than throwing or producing empty times.
        val singapore = PrayerCalculator.calculate(now, calculationMethodName = "SINGAPORE")
        assertEquals(singapore.subuh, withoutSource.subuh)
    }

    @Test
    fun fridayUsesJumatOffsetWhenEnabled() {
        // 2026-06-05 is a Friday
        val friday = LocalDateTime(2026, 6, 5, 10, 0, 0).toInstant(zone)

        val plain = PrayerCalculator.calculate(
            now = friday,
            calculationMethodName = "SINGAPORE",
            offsetDzuhur = 0,
            offsetJumat = 7,
            jumatEnabled = false
        )
        val jumat = PrayerCalculator.calculate(
            now = friday,
            calculationMethodName = "SINGAPORE",
            offsetDzuhur = 0,
            offsetJumat = 7,
            jumatEnabled = true
        )

        val delta = jumat.dzuhur.toInstant(zone).toEpochMilliseconds() -
            plain.dzuhur.toInstant(zone).toEpochMilliseconds()
        assertEquals(7L * 60 * 1000, delta)
    }
}
