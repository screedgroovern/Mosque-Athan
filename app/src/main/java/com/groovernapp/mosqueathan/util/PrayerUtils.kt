package com.groovernapp.mosqueathan.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

object PrayerUtils {

    fun formatTime(
        dateTime: LocalDateTime
    ) = "%02d:%02d".format(dateTime.hour, dateTime.minute)

    fun countdown(
        target: LocalDateTime
    ): String {
        val zone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val targetMillis =target.toInstant(zone).toEpochMilliseconds()
        val diff = (targetMillis - nowMillis).coerceAtLeast(0)
        val totalSeconds = diff / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}