package com.groovernapp.mosqueathan.util

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlinx.datetime.Instant

object PrayerCalculator {
    fun calculate(
        now: Instant = Clock.System.now(),
        latitude: Double = MosqueConfig.DEFAULT_LATITUDE,
        longitude: Double = MosqueConfig.DEFAULT_LONGITUDE,
        calculationMethodName: String = MosqueConfig.DEFAULT_CALCULATION_METHOD
    ): PrayerResult {

        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date

        val method = try {
            CalculationMethod.valueOf(calculationMethodName)
        } catch (_: IllegalArgumentException) {
            CalculationMethod.SINGAPORE
        }

        val prayerTimes = PrayerTimes(
            coordinates = Coordinates(latitude, longitude),
            dateComponents = DateComponents(
                today.year,
                today.month.number,
                today.day
            ),
            calculationParameters = method.parameters
        )

        val currentPrayer = when {
            now >= prayerTimes.isha -> PrayerType.ISYA
            now >= prayerTimes.maghrib -> PrayerType.MAGHRIB
            now >= prayerTimes.asr -> PrayerType.ASHAR
            now >= prayerTimes.dhuhr -> PrayerType.DZUHUR
            now >= prayerTimes.fajr -> PrayerType.SUBUH
            else -> PrayerType.ISYA
        }

        val (nextPrayer, nextPrayerInstant) = when {
            now < prayerTimes.fajr -> PrayerType.SUBUH to prayerTimes.fajr
            now < prayerTimes.dhuhr -> PrayerType.DZUHUR to prayerTimes.dhuhr
            now < prayerTimes.asr -> PrayerType.ASHAR to prayerTimes.asr
            now < prayerTimes.maghrib -> PrayerType.MAGHRIB to prayerTimes.maghrib
            now < prayerTimes.isha -> PrayerType.ISYA to prayerTimes.isha
            else -> {
                val tomorrow = today.plus(1, DateTimeUnit.DAY)
                val tomorrowPrayerTimes = PrayerTimes(
                    coordinates = Coordinates(latitude, longitude),
                    dateComponents = DateComponents(
                        tomorrow.year,
                        tomorrow.month.number,
                        tomorrow.day
                    ),
                    calculationParameters = method.parameters
                )
                PrayerType.SUBUH to tomorrowPrayerTimes.fajr
            }
        }

        return PrayerResult(
            subuh = prayerTimes.fajr.toLocalDateTime(zone),
            syuruq = prayerTimes.sunrise.toLocalDateTime(zone),
            dzuhur = prayerTimes.dhuhr.toLocalDateTime(zone),
            ashar = prayerTimes.asr.toLocalDateTime(zone),
            maghrib = prayerTimes.maghrib.toLocalDateTime(zone),
            isya = prayerTimes.isha.toLocalDateTime(zone),
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            nextPrayerTime = nextPrayerInstant.toLocalDateTime(zone)
        )
    }
}