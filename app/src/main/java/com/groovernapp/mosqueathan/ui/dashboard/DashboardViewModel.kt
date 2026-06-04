package com.groovernapp.mosqueathan.ui.dashboard

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

class DashboardViewModel : ViewModel() {

    private val hijriDate = HijriDateFormatter.today()
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

    private val repository = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance()
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
        val dashboardState =
            DashboardUiState.Dashboard(
                currentTime = now.toClockString(),
                gregorianDate = "${now.day} ${
                    now.month.name
                        .lowercase()
                        .replaceFirstChar {
                            it.uppercase()
                        }
                } ${now.year}",
                hijriDate = hijriDate,
                prayerTimes =
                    PrayerTimesUiModel(
                        subuh = PrayerUtils.formatTime(result.subuh),
                        syuruq = PrayerUtils.formatTime(result.syuruq),
                        dzuhur = PrayerUtils.formatTime(result.dzuhur),
                        ashar = PrayerUtils.formatTime(result.ashar),
                        maghrib = PrayerUtils.formatTime(result.maghrib),
                        isya = PrayerUtils.formatTime(result.isya),
                        highlightedPrayer = result.nextPrayer
                    ),
                nextPrayer =
                    NextPrayerUiModel(
                        prayerType = result.nextPrayer,
                        prayerTime = PrayerUtils.formatTime(result.nextPrayerTime),
                        countdown = PrayerUtils.countdown(result.nextPrayerTime)
                    ),
                runningText = activeAnnouncementsText
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
        val config = repository.getConfig()
        prayerResult = PrayerCalculator.calculate(
            latitude = config.latitude,
            longitude = config.longitude,
            calculationMethodName = config.calculationMethodName
        )
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
    }
}
