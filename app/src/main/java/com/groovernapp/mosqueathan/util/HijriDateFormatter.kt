package com.groovernapp.mosqueathan.util

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import com.groovernapp.mosqueathan.domain.HijriDateSource
import java.util.Calendar

/**
 * The Android implementation of [HijriDateSource], backed by `ummalqura-calendar`.
 *
 * Stays in `:app` because that library is JVM-only. Kept as an `object` that also
 * implements the interface, so the handful of existing direct callers still work while
 * the display screens receive it through the graph.
 */
object HijriDateFormatter : HijriDateSource {

    private val hijriMonths = listOf(
        "Muharram",
        "Safar",
        "Rabiul Awal",
        "Rabiul Akhir",
        "Jumadil Awal",
        "Jumadil Akhir",
        "Rajab",
        "Syaban",
        "Ramadhan",
        "Syawal",
        "Dzulqa'dah",
        "Dzulhijjah"
    )

    override fun today(): String {
        val calendar = UmmalquraCalendar()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        return "$day ${hijriMonths[month]} $year H"
    }
}