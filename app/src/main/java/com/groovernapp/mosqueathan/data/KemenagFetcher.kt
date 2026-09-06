package com.groovernapp.mosqueathan.data

import android.util.Log
import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.parseKemenagMonth
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * Fetches Kemenag prayer schedules from `api.myquran.com`, month by month.
 *
 * Lifted out of `MosqueConfigRepository`, where the interesting half was unreachable by any test:
 * deciding *which* months a duration covers is arithmetic over dates, it is the part that can be
 * wrong by a whole month, and it was interleaved with an HTTP call and a database write. [monthsFor]
 * is now that arithmetic on its own.
 *
 * Storage is the caller's. This returns days; it does not know where they go, which is what lets
 * the Realm and Room repositories share it while both exist.
 */
class KemenagFetcher {

    /** Guards against two overlapping fetches of the same month doing the same work twice. */
    private val ongoing = ConcurrentHashMap<String, Boolean>()

    /**
     * The month of [year]/[month] for [cityId], or empty on any failure.
     *
     * Empty rather than an exception: a schedule that could not be fetched is a schedule the
     * display falls back to adhan2 for, which is a working television rather than a crash.
     */
    suspend fun fetchMonth(cityId: String, year: Int, month: Int): List<KemenagSchedule> {
        if (cityId.isEmpty()) return emptyList()
        val key = "$cityId-$year-$month"
        if (ongoing.putIfAbsent(key, true) != null) return emptyList()

        return try {
            val monthStr = month.toString().padStart(2, '0')
            val url = URL("$API_BASE/$cityId/$year-$monthStr")
            Log.d(TAG, "fetching $url")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            try {
                if (connection.responseCode != 200) {
                    Log.e(TAG, "HTTP ${connection.responseCode} for $year-$monthStr")
                    return emptyList()
                }
                val body = BufferedReader(InputStreamReader(connection.inputStream))
                    .use { it.readText() }
                // Parsing lives in core:model and is tested against a captured response from the
                // real API. This only carries the bytes.
                parseKemenagMonth(cityId, body)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "could not fetch $year-$month", e)
            emptyList()
        } finally {
            ongoing.remove(key)
        }
    }

    companion object {
        private const val TAG = "KemenagFetch"
        private const val API_BASE = "https://api.myquran.com/v3/sholat/jadwal"
        private const val TIMEOUT_MS = 10_000

        /**
         * Every (year, month) a duration covers, starting from [today]'s month.
         *
         * Capped at [MAX_MONTHS], which the original code also did — a mistyped duration must not
         * turn into an unbounded walk hitting a public API once per month forever.
         *
         * `SPECIFIC_DATE` reads the year and month out of an ISO date and ignores the day, because
         * the API is addressed by month. A payload it cannot parse yields the current month alone,
         * which is the same "do the least surprising thing" the old code did by leaving its
         * accumulator untouched.
         */
        fun monthsFor(duration: String, targetDate: String, today: LocalDate): List<Pair<Int, Int>> {
            val end: Pair<Int, Int> = when (duration) {
                "1_WEEK" -> today.plus(7, DateTimeUnit.DAY).yearMonth()
                "2_WEEKS" -> today.plus(14, DateTimeUnit.DAY).yearMonth()
                "3_WEEKS" -> today.plus(21, DateTimeUnit.DAY).yearMonth()
                "1_MONTH" -> today.plus(1, DateTimeUnit.MONTH).yearMonth()
                "2_MONTHS" -> today.plus(2, DateTimeUnit.MONTH).yearMonth()
                "3_MONTHS" -> today.plus(3, DateTimeUnit.MONTH).yearMonth()
                "6_MONTHS" -> today.plus(6, DateTimeUnit.MONTH).yearMonth()
                "1_YEAR" -> today.plus(1, DateTimeUnit.YEAR).yearMonth()
                "SPECIFIC_DATE" -> specificDate(targetDate) ?: today.yearMonth()
                else -> today.yearMonth()
            }

            val months = mutableListOf<Pair<Int, Int>>()
            var year = today.year
            var month = today.month.number
            while ((year < end.first || (year == end.first && month <= end.second)) &&
                months.size < MAX_MONTHS
            ) {
                months += year to month
                month++
                if (month > 12) {
                    month = 1
                    year++
                }
            }
            return months
        }

        const val MAX_MONTHS = 24

        private fun LocalDate.yearMonth(): Pair<Int, Int> = year to month.number

        private fun specificDate(targetDate: String): Pair<Int, Int>? {
            if (targetDate.isEmpty()) return null
            val parts = targetDate.split("-")
            if (parts.size < 2) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            if (month !in 1..12) return null
            return year to month
        }
    }
}
