package com.groovernapp.mosqueathan.util

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.KemenagScheduleSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Instant

object PrayerCalculator {

    private fun parseKemenagTime(date: kotlinx.datetime.LocalDate, timeStr: String, zone: TimeZone): Instant? {
        if (timeStr.isEmpty()) return null
        return try {
            val parts = timeStr.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toInt()
                val minute = parts[1].trim().toInt()
                val localDateTime = LocalDateTime(date.year, date.month.number, date.day, hour, minute, 0, 0)
                localDateTime.toInstant(zone)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun calculate(
        now: Instant = Clock.System.now(),
        latitude: Double = MosqueConfig.DEFAULT_LATITUDE,
        longitude: Double = MosqueConfig.DEFAULT_LONGITUDE,
        calculationMethodName: String = MosqueConfig.DEFAULT_CALCULATION_METHOD,
        offsetImsak: Int = 0,
        offsetSubuh: Int = 0,
        offsetSyuruq: Int = 0,
        offsetDzuhur: Int = 0,
        offsetAshar: Int = 0,
        offsetMaghrib: Int = 0,
        offsetIsya: Int = 0,
        kemenagCityId: String = "",
        jumatEnabled: Boolean = false,
        offsetJumat: Int = 0,
        kemenagSource: KemenagScheduleSource? = null
    ): PrayerResult {

        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date

        var imsakAdjusted: Instant? = null
        var fajrAdjusted: Instant? = null
        var sunriseAdjusted: Instant? = null
        var dhuhrAdjusted: Instant? = null
        var asrAdjusted: Instant? = null
        
        val isFridayMode = today.dayOfWeek == kotlinx.datetime.DayOfWeek.FRIDAY && jumatEnabled
        val actualOffsetDzuhur = if (isFridayMode) offsetJumat else offsetDzuhur
        var maghribAdjusted: Instant? = null
        var ishaAdjusted: Instant? = null
        var nextFajrAdjusted: Instant? = null
        var tomorrowImsakAdjusted: Instant? = null

        if (calculationMethodName == "KEMENAG" && kemenagCityId.isNotEmpty() && kemenagSource != null) {
            val todaySchedule = kemenagSource.scheduleFor(kemenagCityId, today.toString())
            if (todaySchedule != null) {
                imsakAdjusted = parseKemenagTime(today, todaySchedule.imsak, zone)?.plus(offsetImsak.toLong(), DateTimeUnit.MINUTE)
                fajrAdjusted = parseKemenagTime(today, todaySchedule.subuh, zone)?.plus(offsetSubuh.toLong(), DateTimeUnit.MINUTE)
                sunriseAdjusted = parseKemenagTime(today, todaySchedule.terbit, zone)?.plus(offsetSyuruq.toLong(), DateTimeUnit.MINUTE)
                dhuhrAdjusted = parseKemenagTime(today, todaySchedule.dzuhur, zone)?.plus(actualOffsetDzuhur.toLong(), DateTimeUnit.MINUTE)
                asrAdjusted = parseKemenagTime(today, todaySchedule.ashar, zone)?.plus(offsetAshar.toLong(), DateTimeUnit.MINUTE)
                maghribAdjusted = parseKemenagTime(today, todaySchedule.maghrib, zone)?.plus(offsetMaghrib.toLong(), DateTimeUnit.MINUTE)
                ishaAdjusted = parseKemenagTime(today, todaySchedule.isya, zone)?.plus(offsetIsya.toLong(), DateTimeUnit.MINUTE)
            }
            val tomorrow = today.plus(1, DateTimeUnit.DAY)
            val tomorrowSchedule = kemenagSource.scheduleFor(kemenagCityId, tomorrow.toString())
            if (tomorrowSchedule != null) {
                nextFajrAdjusted = parseKemenagTime(tomorrow, tomorrowSchedule.subuh, zone)?.plus(offsetSubuh.toLong(), DateTimeUnit.MINUTE)
                tomorrowImsakAdjusted = parseKemenagTime(tomorrow, tomorrowSchedule.imsak, zone)?.plus(offsetImsak.toLong(), DateTimeUnit.MINUTE)
            }
        }

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

        val fajrFinal = fajrAdjusted ?: prayerTimes.fajr.plus(offsetSubuh.toLong(), DateTimeUnit.MINUTE)
        val imsakFinal = imsakAdjusted ?: fajrFinal.plus((-10 + offsetImsak).toLong(), DateTimeUnit.MINUTE)
        val sunriseFinal = sunriseAdjusted ?: prayerTimes.sunrise.plus(offsetSyuruq.toLong(), DateTimeUnit.MINUTE)
        val dhuhrFinal = dhuhrAdjusted ?: prayerTimes.dhuhr.plus(actualOffsetDzuhur.toLong(), DateTimeUnit.MINUTE)
        val asrFinal = asrAdjusted ?: prayerTimes.asr.plus(offsetAshar.toLong(), DateTimeUnit.MINUTE)
        val maghribFinal = maghribAdjusted ?: prayerTimes.maghrib.plus(offsetMaghrib.toLong(), DateTimeUnit.MINUTE)
        val ishaFinal = ishaAdjusted ?: prayerTimes.isha.plus(offsetIsya.toLong(), DateTimeUnit.MINUTE)

        val currentPrayer = when {
            now >= ishaFinal -> PrayerType.ISYA
            now >= maghribFinal -> PrayerType.MAGHRIB
            now >= asrFinal -> PrayerType.ASHAR
            now >= dhuhrFinal -> PrayerType.DZUHUR
            now >= fajrFinal -> PrayerType.SUBUH
            else -> PrayerType.ISYA
        }

        val (nextPrayer, nextPrayerInstant) = when {
            now < imsakFinal -> PrayerType.IMSAK to imsakFinal
            now < fajrFinal -> PrayerType.SUBUH to fajrFinal
            now < sunriseFinal -> PrayerType.SYURUQ to sunriseFinal
            now < dhuhrFinal -> PrayerType.DZUHUR to dhuhrFinal
            now < asrFinal -> PrayerType.ASHAR to asrFinal
            now < maghribFinal -> PrayerType.MAGHRIB to maghribFinal
            now < ishaFinal -> PrayerType.ISYA to ishaFinal
            else -> {
                val tomorrow = today.plus(1, DateTimeUnit.DAY)
                val tomorrowFajr = nextFajrAdjusted ?: run {
                    val tomorrowPrayerTimes = PrayerTimes(
                        coordinates = Coordinates(latitude, longitude),
                        dateComponents = DateComponents(
                            tomorrow.year,
                            tomorrow.month.number,
                            tomorrow.day
                        ),
                        calculationParameters = method.parameters
                    )
                    tomorrowPrayerTimes.fajr.plus(offsetSubuh.toLong(), DateTimeUnit.MINUTE)
                }
                val tomorrowImsak = tomorrowImsakAdjusted ?: tomorrowFajr.plus((-10 + offsetImsak).toLong(), DateTimeUnit.MINUTE)
                PrayerType.IMSAK to tomorrowImsak
            }
        }
        return PrayerResult(
            imsak = imsakFinal.toLocalDateTime(zone),
            subuh = fajrFinal.toLocalDateTime(zone),
            syuruq = sunriseFinal.toLocalDateTime(zone),
            dzuhur = dhuhrFinal.toLocalDateTime(zone),
            ashar = asrFinal.toLocalDateTime(zone),
            maghrib = maghribFinal.toLocalDateTime(zone),
            isya = ishaFinal.toLocalDateTime(zone),
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            nextPrayerTime = nextPrayerInstant.toLocalDateTime(zone)
        )
    }

    fun getNextPrayer(
        result: PrayerResult,
        now: Instant,
        latitude: Double,
        longitude: Double,
        methodName: String,
        offsetImsak: Int,
        offsetSubuh: Int,
        kemenagCityId: String,
        kemenagSource: KemenagScheduleSource? = null
    ): Pair<PrayerType, LocalDateTime> {
        val zone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(zone).date
        
        val imsakFinal = result.imsak.toInstant(zone)
        val fajrFinal = result.subuh.toInstant(zone)
        val sunriseFinal = result.syuruq.toInstant(zone)
        val dhuhrFinal = result.dzuhur.toInstant(zone)
        val asrFinal = result.ashar.toInstant(zone)
        val maghribFinal = result.maghrib.toInstant(zone)
        val ishaFinal = result.isya.toInstant(zone)
        
        val (nextPrayer, nextPrayerInstant) = when {
            now < imsakFinal -> PrayerType.IMSAK to imsakFinal
            now < fajrFinal -> PrayerType.SUBUH to fajrFinal
            now < sunriseFinal -> PrayerType.SYURUQ to sunriseFinal
            now < dhuhrFinal -> PrayerType.DZUHUR to dhuhrFinal
            now < asrFinal -> PrayerType.ASHAR to asrFinal
            now < maghribFinal -> PrayerType.MAGHRIB to maghribFinal
            now < ishaFinal -> PrayerType.ISYA to ishaFinal
            else -> {
                val tomorrow = today.plus(1, DateTimeUnit.DAY)
                val method = try {
                    CalculationMethod.valueOf(methodName)
                } catch (_: IllegalArgumentException) {
                    CalculationMethod.SINGAPORE
                }
                var nextFajrAdjusted: Instant? = null
                var tomorrowImsakAdjusted: Instant? = null
                if (methodName == "KEMENAG" && kemenagCityId.isNotEmpty() && kemenagSource != null) {
                    val tomorrowSchedule = kemenagSource.scheduleFor(kemenagCityId, tomorrow.toString())
                    if (tomorrowSchedule != null) {
                        nextFajrAdjusted = parseKemenagTime(tomorrow, tomorrowSchedule.subuh, zone)?.plus(offsetSubuh.toLong(), DateTimeUnit.MINUTE)
                        tomorrowImsakAdjusted = parseKemenagTime(tomorrow, tomorrowSchedule.imsak, zone)?.plus(offsetImsak.toLong(), DateTimeUnit.MINUTE)
                    }
                }
                val tomorrowFajr = nextFajrAdjusted ?: run {
                    val tomorrowPrayerTimes = PrayerTimes(
                        coordinates = Coordinates(latitude, longitude),
                        dateComponents = DateComponents(tomorrow.year, tomorrow.month.number, tomorrow.day),
                        calculationParameters = method.parameters
                    )
                    tomorrowPrayerTimes.fajr.plus(offsetSubuh.toLong(), DateTimeUnit.MINUTE)
                }
                val tomorrowImsakFinal = tomorrowImsakAdjusted ?: tomorrowFajr.plus((-10 + offsetImsak).toLong(), DateTimeUnit.MINUTE)
                PrayerType.IMSAK to tomorrowImsakFinal
            }
        }
        return nextPrayer to nextPrayerInstant.toLocalDateTime(zone)
    }
}