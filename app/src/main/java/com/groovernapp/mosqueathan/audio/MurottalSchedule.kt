package com.groovernapp.mosqueathan.audio

import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime

/**
 * Whether the pre-adzan recitation should be sounding at [now].
 *
 * Pure, and separated from `AppViewModel` for one reason: being wrong here means a television
 * reciting Qur'an over the khutbah in front of a full mosque. That is not a defect anyone wants to
 * discover on a Friday, and it is not observable from a unit test unless the decision can be called
 * directly.
 *
 * @param dzuhur the calculated Friday adzan, which is the hard end of the window. Null while the
 *   schedule has not been calculated yet — treated as "do not play", because a recitation with no
 *   known stopping point is the failure this whole function exists to prevent.
 * @param forcedSilence the takmir having pressed silence, which means silence including this.
 */
internal fun shouldPlayMurottal(
    config: MosqueConfig,
    now: LocalDateTime,
    dzuhur: LocalDateTime?,
    forcedSilence: Boolean
): Boolean {
    if (!config.murottalEnabled) return false
    if (config.murottalAudioPaths.isBlank()) return false
    if (!config.jumatEnabled) return false
    if (now.dayOfWeek != DayOfWeek.FRIDAY) return false
    if (forcedSilence) return false

    val windowStart = minutesOfDay(config.jumatStartTime) ?: return false
    val windowEnd = minutesOfDay(config.jumatEndTime) ?: return false
    val nowMinutes = now.hour * 60 + now.minute
    if (nowMinutes < windowStart || nowMinutes >= windowEnd) return false

    // The delay is measured from the start of the Friday window, matching the field's own label
    // ("minutes after Start Time") rather than from the adzan.
    if (nowMinutes < windowStart + config.murottalStartDelay) return false

    // Read from the calculated schedule, not from jumatEndTime: that one is when the Friday
    // *layout* stops, long after the recitation should have.
    val adzan = dzuhur ?: return false
    return nowMinutes < adzan.hour * 60 + adzan.minute
}

/** `"HH:mm"` as minutes past midnight, or null if it is not that. */
internal fun minutesOfDay(timeStr: String): Int? {
    val parts = timeStr.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}
