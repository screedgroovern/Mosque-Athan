package com.groovernapp.mosqueathan.domain

/**
 * A prayer-time calculation method the user can pick, and the name to show for it.
 *
 * [id] is what is stored in [MosqueConfig] and handed to adhan2's `CalculationMethod.valueOf`, so
 * it must not change. [label] is what a person reads.
 *
 * Showing the id was a real defect rather than a cosmetic one: the settings screen listed
 * `MUSLIM_WORLD_LEAGUE` where the hand-written web panel had always said
 * "Muslim World League (MWL)", and on a phone the identifier filled an entire row. That is exactly
 * the drift this migration exists to make impossible — the television and the browser render the
 * same composable, so fixing it here fixes both.
 *
 * The labels are proper nouns — institutions and places — so they read the same in English and
 * Indonesian and are deliberately not passed through `Lang`.
 */
data class CalculationMethodOption(val id: String, val label: String)

/**
 * The methods offered, in the order the settings screen lays them out. The order is unchanged from
 * when the ids were the labels, so no button moves.
 */
val calculationMethodOptions: List<CalculationMethodOption> = listOf(
    CalculationMethodOption("SINGAPORE", "MUIS (Singapore)"),
    CalculationMethodOption("UMM_AL_QURA", "Umm al-Qura (Makkah)"),
    CalculationMethodOption("MUSLIM_WORLD_LEAGUE", "Muslim World League (MWL)"),
    CalculationMethodOption("EGYPTIAN", "Egyptian General Authority"),
    CalculationMethodOption("KARACHI", "University of Islamic Sciences (Karachi)"),
    CalculationMethodOption("DUBAI", "Dubai Standard"),
    CalculationMethodOption("KUWAIT", "Kuwait Standard"),
    CalculationMethodOption("QATAR", "Qatar Standard"),
    CalculationMethodOption("KEMENAG", "Kemenag RI (Indonesia)")
)

/**
 * The readable name for a stored method id. An unknown id is returned as-is rather than hidden:
 * `PrayerCalculator` falls back to SINGAPORE for a name adhan2 does not know, and a user looking at
 * a stale value needs to see what it actually says.
 */
fun calculationMethodLabel(id: String): String =
    calculationMethodOptions.firstOrNull { it.id == id }?.label ?: id
