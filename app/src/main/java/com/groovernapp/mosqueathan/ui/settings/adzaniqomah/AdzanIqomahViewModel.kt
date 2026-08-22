package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.data.MosqueConfigRepository
import com.groovernapp.mosqueathan.data.copy
import com.groovernapp.mosqueathan.domain.PrayerType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The Adzan & Iqomah category.
 *
 * Preview events are **not** handled here. Previewing swaps which screen
 * `AppViewModel` is driving, which is navigation rather than settings, so it stays
 * with the caller that already owns that.
 */
class AdzanIqomahViewModel(
    private val repository: MosqueConfigRepository
) : ViewModel() {

    val uiState: StateFlow<AdzanIqomahUiState> =
        repository.getConfigFlow()
            .map { config ->
                AdzanIqomahUiState(
                    adzanLeadMinutes = config.adzanLeadMinutes,
                    iqomahMinutes = IQOMAH_PRAYERS.associateWith { config.iqomahFor(it) },
                    adzanFooterText = config.adzanFooterText,
                    iqomahFooterText = config.iqomahFooterText
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AdzanIqomahUiState()
            )

    fun onEvent(event: AdzanIqomahEvent) {
        when (event) {
            is AdzanIqomahEvent.AdzanLeadMinutesChanged ->
                edit { it.copy(adzanLeadMinutes = event.minutes) }

            is AdzanIqomahEvent.IqomahMinutesChanged ->
                edit { it.withIqomah(event.prayer, event.minutes) }

            is AdzanIqomahEvent.FootersChanged ->
                edit { it.copy(adzanFooterText = event.adzan, iqomahFooterText = event.iqomah) }

            AdzanIqomahEvent.PreviewAdzanRequested,
            AdzanIqomahEvent.PreviewIqomahRequested -> Unit // handled by the caller
        }
    }

    private fun edit(change: (MosqueConfigEntity) -> MosqueConfigEntity) {
        viewModelScope.launch {
            repository.updateConfig(change(repository.getConfig()))
        }
    }
}

private fun MosqueConfigEntity.iqomahFor(prayer: PrayerType): Int = when (prayer) {
    PrayerType.SUBUH -> iqomahSubuh
    PrayerType.DZUHUR -> iqomahDzuhur
    PrayerType.ASHAR -> iqomahAshar
    PrayerType.MAGHRIB -> iqomahMaghrib
    PrayerType.ISYA -> iqomahIsya
    PrayerType.IMSAK, PrayerType.SYURUQ -> 0
}

private fun MosqueConfigEntity.withIqomah(prayer: PrayerType, minutes: Int): MosqueConfigEntity =
    when (prayer) {
        PrayerType.SUBUH -> copy(iqomahSubuh = minutes)
        PrayerType.DZUHUR -> copy(iqomahDzuhur = minutes)
        PrayerType.ASHAR -> copy(iqomahAshar = minutes)
        PrayerType.MAGHRIB -> copy(iqomahMaghrib = minutes)
        PrayerType.ISYA -> copy(iqomahIsya = minutes)
        // Imsak and Syuruq have no iqomah; a stray event must not silently write.
        PrayerType.IMSAK, PrayerType.SYURUQ -> this
    }
