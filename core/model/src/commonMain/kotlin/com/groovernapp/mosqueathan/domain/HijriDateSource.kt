package com.groovernapp.mosqueathan.domain

/**
 * Today's date in the Hijri calendar, already formatted for display
 * (e.g. `"10 Rabiul Awal 1448 H"`).
 *
 * An interface rather than an `expect`, because the Android implementation is not a
 * platform API call — it is `ummalqura-calendar`, a JVM library holding the real Umm
 * al-Qura month-length table. That table is data, not arithmetic: the Saudi calendar is
 * set by decree and cannot be computed. An approximation would shift the displayed date
 * by a day, on a screen the congregation reads daily, so the table has to be carried
 * rather than derived.
 *
 * So the port is not "write the iOS version" but "find that table for each platform" —
 * and until then, injecting keeps the display screens buildable everywhere while the
 * real implementation stays where the library is.
 */
fun interface HijriDateSource {
    fun today(): String
}
