package com.groovernapp.mosqueathan.ui.dashboard

import com.groovernapp.mosqueathan.domain.DisplayDataSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.util.HijriDateFormatter
import com.groovernapp.mosqueathan.util.PrayerCalculator
import com.groovernapp.mosqueathan.util.PrayerResult
import com.groovernapp.mosqueathan.util.PrayerUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class DashboardViewModel(
    private val repository: DisplayDataSource,
    private val kemenagSource: com.groovernapp.mosqueathan.domain.KemenagScheduleSource
) : ViewModel() {

    private var hijriDate = HijriDateFormatter.today()
    private var prayerResult: PrayerResult? = null
    private var lastCalculatedDate: LocalDate? = null
    private var activeAnnouncementsText = "Mari rapatkan shaf dan mohon handphone di-silent."

    private val _uiState =
        MutableStateFlow(
            DashboardUiState.Dashboard(
                currentTime = "--:--:--",
                gregorianDate = "-",
                hijriDate = "-",
                prayerTimes = PrayerTimesUiModel(
                    imsak = "--:--",
                    subuh = "--:--",
                    syuruq = "--:--",
                    dzuhur = "--:--",
                    ashar = "--:--",
                    maghrib = "--:--",
                    isya = "--:--",
                    highlightedPrayer = PrayerType.SUBUH
                ),
                nextPrayer = NextPrayerUiModel(
                    prayerType = PrayerType.SUBUH,
                    prayerTime = "--:--",
                    countdown = "--:--:--"
                ),
                runningText = activeAnnouncementsText
            )
        )

    val uiState = _uiState.asStateFlow()
    private var clockJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getConfigFlow().collect {
                lastCalculatedDate = null
                refreshPrayerSchedule()
            }
        }
        viewModelScope.launch {
            repository.getActiveAnnouncementsFlow().collect { announcements ->
                activeAnnouncementsText = announcements.joinToString(separator = "   •   ") { it.content }
                if (activeAnnouncementsText.isEmpty()) {
                    activeAnnouncementsText = "Mari rapatkan shaf dan mohon handphone di-silent."
                }
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                prayerResult?.let { updateDashboardState(it, now) }
            }
        }
        startClock()
    }

    fun Month.toIndonesian(): String = when (this) {
        Month.JANUARY -> "Januari"
        Month.FEBRUARY -> "Februari"
        Month.MARCH -> "Maret"
        Month.APRIL -> "April"
        Month.MAY -> "Mei"
        Month.JUNE -> "Juni"
        Month.JULY -> "Juli"
        Month.AUGUST -> "Agustus"
        Month.SEPTEMBER -> "September"
        Month.OCTOBER -> "Oktober"
        Month.NOVEMBER -> "November"
        Month.DECEMBER -> "Desember"
    }

    private fun LocalDateTime.toClockString(): String =
        "%02d:%02d:%02d".format(
            hour,
            minute,
            second
        )

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                refreshPrayerSchedule()
                val result =
                    prayerResult ?: run {
                        delay(1000)
                        continue
                    }

                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                updateDashboardState(result, now)
                delay(1000)
            }
        }
    }

    private fun updateDashboardState(
        result: PrayerResult,
        now: LocalDateTime
    ) {
        val config = repository.getConfig()
        val (nextPrayerType, nextPrayerTime) = PrayerCalculator.getNextPrayer(
            kemenagSource = kemenagSource,
            result = result,
            now = Clock.System.now(),
            latitude = config.latitude,
            longitude = config.longitude,
            methodName = config.calculationMethodName,
            offsetImsak = config.offsetImsak,
            offsetSubuh = config.offsetSubuh,
            kemenagCityId = config.kemenagCityId
        )

        val slideshowSchedules = repository.getSlideshowSchedules()
        val slideshowEnabled = config.slideshowEnabled
        var slideshowActive = false
        
        if (isFridayModeActive(config, now)) {
            // Friday mode owns the screen: only the dedicated Friday slideshow may take over,
            // a general slideshow schedule overlapping the Friday window must not.
            slideshowActive = isFridayBeforeAdzan(config, result, now) && config.jumatSlideshowEnabled
        } else if (slideshowEnabled && slideshowSchedules.isNotEmpty()) {
            for (sched in slideshowSchedules) {
                if (com.groovernapp.mosqueathan.util.PrayerUtils.isScheduleActiveNew(
                        now = now,
                        activeDays = sched.activeDays,
                        specificDates = sched.specificDates,
                        startTime = sched.startTime,
                        endTime = sched.endTime,
                        legacyDay = sched.day,
                        legacyIsSpecificDate = sched.isSpecificDate
                    )
                ) {
                    slideshowActive = true
                    break
                }
            }
        }

        val dashboardState =
            DashboardUiState.Dashboard(
                currentTime = now.toClockString(),
                gregorianDate = "${now.day} ${
                    now.month.toIndonesian()
                        .lowercase()
                        .replaceFirstChar {
                            it.uppercase()
                        }
                } ${now.year}",
                hijriDate = hijriDate,
                prayerTimes =
                    PrayerTimesUiModel(
                        imsak = PrayerUtils.formatTime(result.imsak),
                        subuh = PrayerUtils.formatTime(result.subuh),
                        syuruq = PrayerUtils.formatTime(result.syuruq),
                        dzuhur = PrayerUtils.formatTime(result.dzuhur),
                        ashar = PrayerUtils.formatTime(result.ashar),
                        maghrib = PrayerUtils.formatTime(result.maghrib),
                        isya = PrayerUtils.formatTime(result.isya),
                        highlightedPrayer = nextPrayerType
                    ),
                nextPrayer =
                    NextPrayerUiModel(
                        prayerType = nextPrayerType,
                        prayerTime = PrayerUtils.formatTime(nextPrayerTime),
                        countdown = PrayerUtils.countdown(nextPrayerTime)
                    ),
                runningText = activeAnnouncementsText,
                isSlideshowActive = slideshowActive
            )

        _uiState.value = dashboardState
    }

    private fun refreshPrayerSchedule() {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        if (today == lastCalculatedDate) {
            return
        }

        lastCalculatedDate = today
        hijriDate = HijriDateFormatter.today()
        val config = repository.getConfig()
        prayerResult = PrayerCalculator.calculate(
            kemenagSource = kemenagSource,
            latitude = config.latitude,
            longitude = config.longitude,
            calculationMethodName = config.calculationMethodName,
            offsetImsak = config.offsetImsak,
            offsetSubuh = config.offsetSubuh,
            offsetSyuruq = config.offsetSyuruq,
            offsetDzuhur = config.offsetDzuhur,
            offsetAshar = config.offsetAshar,
            offsetMaghrib = config.offsetMaghrib,
            offsetIsya = config.offsetIsya,
            kemenagCityId = config.kemenagCityId,
            jumatEnabled = config.jumatEnabled,
            offsetJumat = config.offsetJumat
        )
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    private fun isFridayBeforeAdzan(config: com.groovernapp.mosqueathan.domain.MosqueConfig, result: PrayerResult, now: LocalDateTime): Boolean {
        if (!config.jumatEnabled) return false
        if (now.dayOfWeek != kotlinx.datetime.DayOfWeek.FRIDAY) return false
        val currentMinutes = now.hour * 60 + now.minute
        val startMins = parseTimeToMinutes(config.jumatStartTime) ?: return false
        val adzanMins = result.dzuhur.hour * 60 + result.dzuhur.minute
        return currentMinutes in startMins until adzanMins
    }

    private fun isFridayModeActive(config: com.groovernapp.mosqueathan.domain.MosqueConfig, now: LocalDateTime): Boolean {
        if (!config.jumatEnabled) return false
        if (now.dayOfWeek != kotlinx.datetime.DayOfWeek.FRIDAY) return false
        val currentMinutes = now.hour * 60 + now.minute
        val startMins = parseTimeToMinutes(config.jumatStartTime) ?: return false
        val endMins = parseTimeToMinutes(config.jumatEndTime) ?: return false
        return currentMinutes >= startMins && currentMinutes < endMins
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
    }
}
