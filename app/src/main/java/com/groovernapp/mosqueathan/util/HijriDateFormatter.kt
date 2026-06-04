package com.groovernapp.mosqueathan.util

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar
import java.util.Calendar

object HijriDateFormatter {

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

    fun today(): String {
        val calendar = UmmalquraCalendar()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        return "$day ${hijriMonths[month]} $year H"
    }
}