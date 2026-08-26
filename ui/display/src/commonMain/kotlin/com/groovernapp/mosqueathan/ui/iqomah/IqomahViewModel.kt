package com.groovernapp.mosqueathan.ui.iqomah

import com.groovernapp.mosqueathan.util.pad
import com.groovernapp.mosqueathan.domain.DisplayDataSource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.domain.PrayerType
import com.groovernapp.mosqueathan.util.joinFooterLines
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class IqomahViewModel(
    private val repository: DisplayDataSource
) : ViewModel() {

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

    fun start(prayerType: PrayerType, customDurationSeconds: Int? = null) {
        val config = repository.getConfig()
        val footer = joinFooterLines(config.iqomahFooterText.ifEmpty { "Mari merapatkan dan meluruskan shaf." })
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val minutes = repository.getIqomahMinutes(prayerType)
            var remaining = customDurationSeconds ?: (minutes * 60)
            while (remaining >= 0) {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                _uiState.value = IqomahUiModel(
                    prayerType = prayerType,
                    iqomahMinutes = minutes,
                    remainingSeconds = remaining,
                    currentTime = now.toClockString(),
                    footerText = footer
                )
                if (remaining == 0) break
                delay(1000)
                remaining--
            }
        }
    }

    private fun LocalDateTime.toClockString(): String =
        "${hour.pad(2)}:${minute.pad(2)}:${second.pad(2)}"

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
