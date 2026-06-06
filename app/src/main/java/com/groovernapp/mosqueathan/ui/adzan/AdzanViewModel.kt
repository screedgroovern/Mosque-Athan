package com.groovernapp.mosqueathan.ui.adzan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.util.PrayerUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class AdzanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdzanUiModel(
            prayerType = PrayerType.SUBUH,
            prayerTime = "--:--",
            currentTime = "--:--:--",
            remainingSeconds = 0
        )
    )
    val uiState = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun start(prayerType: PrayerType, prayerTime: LocalDateTime) {
        val config = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance().getConfig()
        val footerRaw = config.adzanFooterText.ifEmpty { "Mohon bersiap dan hentikan aktivitas sementara." }
        val footer = try {
            if (footerRaw.trim().startsWith("[")) {
                val arr = org.json.JSONArray(footerRaw)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                list.joinToString("   •   ")
            } else {
                footerRaw
            }
        } catch (e: Exception) {
            footerRaw
        }
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = secondsUntil(prayerTime)
            while (remaining > 0) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                _uiState.value = AdzanUiModel(
                    prayerType = prayerType,
                    prayerTime = PrayerUtils.formatTime(prayerTime),
                    currentTime = now.toClockString(),
                    remainingSeconds = remaining,
                    footerText = footer
                )
                delay(1000)
                remaining = secondsUntil(prayerTime)
            }
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            _uiState.value = AdzanUiModel(
                prayerType = prayerType,
                prayerTime = PrayerUtils.formatTime(prayerTime),
                currentTime = now.toClockString(),
                remainingSeconds = 0,
                footerText = footer
            )
        }
    }

    private fun secondsUntil(time: LocalDateTime): Int {
        val now = Clock.System.now().toEpochMilliseconds()
        val then = time.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return (((then - now) + 999) / 1000).toInt().coerceAtLeast(0)
    }

    private fun LocalDateTime.toClockString(): String =
        "%02d:%02d:%02d".format(hour, minute, second)

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
