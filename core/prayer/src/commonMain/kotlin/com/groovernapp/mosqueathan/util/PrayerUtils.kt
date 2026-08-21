package com.groovernapp.mosqueathan.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.number
import kotlin.time.Clock

object PrayerUtils {

    /** `yyyy-MM-dd`, locale-independent, replacing a platform format call. */
    private fun isoDate(at: LocalDateTime): String =
        "${at.year.pad(4)}-${at.month.number.pad(2)}-${at.day.pad(2)}"

    fun formatTime(
        dateTime: LocalDateTime
    ) = "${dateTime.hour.pad(2)}:${dateTime.minute.pad(2)}"

    fun countdown(
        target: LocalDateTime
    ): String {
        val zone = TimeZone.currentSystemDefault()
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val targetMillis = target.toInstant(zone).toEpochMilliseconds()
        val diff = (targetMillis - nowMillis).coerceAtLeast(0)
        val totalSeconds = diff / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return "${hours.pad(2)}:${minutes.pad(2)}:${seconds.pad(2)}"
    }

    fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    fun isScheduleActive(
        now: LocalDateTime,
        day: String,
        isSpecificDate: Boolean,
        startTime: String,
        endTime: String
    ): Boolean {
        if (day.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) return false

        if (isSpecificDate) {
            val todayStr = isoDate(now)
            if (day.trim() != todayStr) {
                return false
            }
        } else {
            val currentDayName = now.dayOfWeek.name // MONDAY, TUESDAY...
            if (!day.equals(currentDayName, ignoreCase = true)) {
                return false
            }
        }

        val currentMinutes = now.hour * 60 + now.minute
        val startMinutes = parseTimeToMinutes(startTime) ?: return false
        val endMinutes = parseTimeToMinutes(endTime) ?: return false

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    /**
     * New overload for multi-day schedules using activeDays (comma-separated day names)
     * and specificDates (comma-separated date strings). If activeDays is non-empty, it checks
     * day-of-week membership; if specificDates is non-empty, it checks date membership.
     * Falls back to legacy isScheduleActive if both new fields are empty.
     */
    fun isScheduleActiveNew(
        now: LocalDateTime,
        activeDays: String,
        specificDates: String,
        startTime: String,
        endTime: String,
        // Legacy fallback fields
        legacyDay: String = "",
        legacyIsSpecificDate: Boolean = false
    ): Boolean {
        if (startTime.isEmpty() || endTime.isEmpty()) return false

        val currentMinutes = now.hour * 60 + now.minute
        val startMinutes = parseTimeToMinutes(startTime) ?: return false
        val endMinutes = parseTimeToMinutes(endTime) ?: return false

        val timeActive = if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
        if (!timeActive) return false

        // Check specificDates (comma-separated date strings like "2026-06-18")
        if (specificDates.isNotEmpty()) {
            val todayStr = isoDate(now)
            val dates = specificDates.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            return dates.any { it == todayStr }
        }

        // Check activeDays (comma-separated day names like "MONDAY,WEDNESDAY")
        if (activeDays.isNotEmpty()) {
            val currentDayName = now.dayOfWeek.name
            val days = activeDays.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            return days.any { it == currentDayName }
        }

        // Fall back to legacy single-day field
        if (legacyDay.isNotEmpty()) {
            return isScheduleActive(now, legacyDay, legacyIsSpecificDate, startTime, endTime)
        }

        return false
    }
}