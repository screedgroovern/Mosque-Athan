package com.groovernapp.mosqueathan.ui.iqomah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.PrayerType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class IqomahViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        IqomahUiModel(
            prayerType = PrayerType.SUBUH,
            iqomahMinutes = 0,
            remainingSeconds = 0,
            currentTime = "--:--:--"
        )
    )
    val uiState = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun start(prayerType: PrayerType) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val minutes = com.groovernapp.mosqueathan.data.MosqueConfigRepository.getInstance().getIqomahMinutes(prayerType)
            var remaining = minutes * 60
            while (remaining >= 0) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                _uiState.value = IqomahUiModel(
                    prayerType = prayerType,
                    iqomahMinutes = minutes,
                    remainingSeconds = remaining,
                    currentTime = now.toClockString()
                )
                if (remaining == 0) break
                delay(1000)
                remaining--
            }
        }
    }

    private fun LocalDateTime.toClockString(): String =
        "%02d:%02d:%02d".format(hour, minute, second)

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
