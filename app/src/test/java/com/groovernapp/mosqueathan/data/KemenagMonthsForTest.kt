package com.groovernapp.mosqueathan.data

import java.io.File
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which months a Kemenag duration covers.
 *
 * ### Why this cannot be noticed at runtime
 *
 * The result is a list of months to fetch, and a wrong list produces *no error anywhere*. Fetch
 * one month too few and the display works perfectly until the day the cache runs out, at which
 * point `PrayerCalculator` falls through to adhan2 — which returns plausible prayer times that are
 * a few minutes from the Kemenag ones the mosque expects. Fetch a month too many and nothing is
 * wrong at all. Nothing on screen, in a log, or in the database distinguishes the two, and the day
 * it matters is weeks after the setting was changed.
 *
 * The year boundary is the specific trap: the walk carries its own `month++ / if (month > 12)`,
 * and a December start is the only input where that arithmetic is exercised. A mosque that sets
 * "3 weeks" on 20 December is asking for exactly the case a naive implementation drops.
 *
 * This is why [KemenagFetcher.monthsFor] was lifted out of the repository: it used to be
 * interleaved with an HTTP call and a database write, so testing the arithmetic meant a network
 * and a database, and it was therefore never tested.
 */
class KemenagMonthsForTest {

    private companion object {
        /** Mid-January, so a month walk cannot cross a year by accident. */
        val MID_JANUARY = LocalDate(2026, 1, 15)

        /** Late December, the only shape in which the walk's rollover runs. */
        val LATE_DECEMBER = LocalDate(2025, 12, 20)

        /** Passed wherever `SPECIFIC_DATE` is not under test, to prove it is ignored. */
        const val IGNORED_TARGET = "2030-06-01"
    }

    private fun months(year: Int, range: IntRange) = range.map { year to it }

    /**
     * Every duration the UI offers, and the months it must cover from [MID_JANUARY].
     *
     * Inclusive of both ends: the walk starts at today's month and stops after the end month, so
     * "1 week" from 15 January is January alone and "3 weeks" reaches into February.
     */
    private val expected: Map<String, List<Pair<Int, Int>>> = mapOf(
        "1_WEEK" to months(2026, 1..1),
        "2_WEEKS" to months(2026, 1..1),
        "3_WEEKS" to months(2026, 1..2),
        "1_MONTH" to months(2026, 1..2),
        "2_MONTHS" to months(2026, 1..3),
        "3_MONTHS" to months(2026, 1..4),
        "6_MONTHS" to months(2026, 1..7),
        "1_YEAR" to months(2026, 1..12) + (2027 to 1),
        "SPECIFIC_DATE" to months(2026, 1..4)
    )

    /** `SPECIFIC_DATE` is the one duration that reads the date; everything else must not. */
    private fun targetFor(duration: String) =
        if (duration == "SPECIFIC_DATE") "2026-04-20" else IGNORED_TARGET

    // --- drift guards --------------------------------------------------------------------------

    private val fetcherSource = File("src/main/java/com/groovernapp/mosqueathan/data/KemenagFetcher.kt")
    private val uiSource = File(
        "../ui/settings/src/commonMain/kotlin/com/groovernapp/mosqueathan/ui/settings/" +
            "prayerschedule/MethodSettingsSegment.kt"
    )

    /** The duration ids `monthsFor`'s `when` names, read out of its own source. */
    private fun handledDurations(): Set<String> {
        val source = fetcherSource.readText()
        val start = source.indexOf("val end: Pair<Int, Int> = when (duration)")
        if (start < 0) return emptySet()
        val body = source.substring(start, source.indexOf("else ->", start).coerceAtLeast(start))
        return Regex(""""([A-Z0-9_]+)"\s*->""").findAll(body).map { it.groupValues[1] }.toSet()
    }

    /** The duration ids the settings UI offers, read out of `durationList`. */
    private fun offeredDurations(): Set<String> {
        if (!uiSource.isFile) return emptySet()
        val source = uiSource.readText()
        val start = source.indexOf("val durationList = listOf(")
        if (start < 0) return emptySet()
        var depth = 0
        var end = source.indexOf('(', start)
        for (i in end until source.length) {
            if (source[i] == '(') depth++
            if (source[i] == ')') depth--
            if (depth == 0) {
                end = i
                break
            }
        }
        val block = source.substring(start, end)
        return Regex(""""([A-Z0-9_]+)"\s*to""").findAll(block).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun theSourcesTheseGuardsReadAreReadable() {
        // Both halves: an unreadable file or a moved marker would make every set below empty, and
        // three of the four comparisons would then pass by comparing nothing to nothing.
        assertTrue("cannot read ${fetcherSource.absolutePath}", fetcherSource.isFile)
        assertTrue("cannot read ${uiSource.absolutePath}", uiSource.isFile)
        assertTrue("no durations parsed out of monthsFor's when", handledDurations().isNotEmpty())
        assertTrue("no durations parsed out of the UI's durationList", offeredDurations().isNotEmpty())
    }

    @Test
    fun everyDurationTheUiOffersIsHandled() {
        // A duration the UI offers and the `when` does not falls to `else`, which is the current
        // month — so a takmir asks for a year of schedules, gets January, and is told nothing.
        val unhandled = offeredDurations() - handledDurations()
        assertTrue("offered by the settings UI and not handled by monthsFor: $unhandled", unhandled.isEmpty())
    }

    @Test
    fun everyHandledDurationIsTested() {
        val untested = handledDurations() - expected.keys
        assertTrue("handled by monthsFor and not asserted here: $untested", untested.isEmpty())
    }

    @Test
    fun noTestNamesADurationThatIsNotHandled() {
        val unknown = expected.keys - handledDurations()
        assertTrue("asserted here and not handled by monthsFor: $unknown", unknown.isEmpty())
    }

    // --- the arithmetic ------------------------------------------------------------------------

    @Test
    fun everyDurationCoversExactlyTheMonthsItShould() {
        expected.forEach { (duration, months) ->
            assertEquals(
                duration,
                months,
                KemenagFetcher.monthsFor(duration, targetFor(duration), MID_JANUARY)
            )
        }
    }

    @Test
    fun aDurationTheFetcherDoesNotKnowIsTheCurrentMonthAlone() {
        // The `else`. Deliberately the least surprising thing rather than an exception: a display
        // whose duration string was mistyped should still fetch the month it is in.
        listOf("", "1_DECADE", "SPECIFIC-DATE", "1_week").forEach { duration ->
            assertEquals(
                duration,
                listOf(2026 to 1),
                KemenagFetcher.monthsFor(duration, IGNORED_TARGET, MID_JANUARY)
            )
        }
    }

    @Test
    fun aDecemberStartRollsIntoTheNextYear() {
        // The one input that exercises `month++ / if (month > 12) { month = 1; year++ }`. An
        // off-by-one here fetches December 2025 and then walks to month 13, which the API answers
        // for with nothing — a mosque with no January schedule and no error.
        assertEquals(
            listOf(2025 to 12, 2026 to 1),
            KemenagFetcher.monthsFor("3_WEEKS", IGNORED_TARGET, LATE_DECEMBER)
        )
        assertEquals(
            listOf(2025 to 12, 2026 to 1),
            KemenagFetcher.monthsFor("1_MONTH", IGNORED_TARGET, LocalDate(2025, 12, 31))
        )
        assertEquals(
            months(2025, 12..12) + months(2026, 1..12),
            KemenagFetcher.monthsFor("1_YEAR", IGNORED_TARGET, LATE_DECEMBER)
        )
    }

    @Test
    fun aWeekThatStaysInsideTheMonthAsksForOneMonth() {
        // The other side of the boundary, so `everyDurationCoversExactlyTheMonthsItShould` cannot
        // be satisfied by an implementation that always adds the next month.
        assertEquals(
            listOf(2026 to 3),
            KemenagFetcher.monthsFor("1_WEEK", IGNORED_TARGET, LocalDate(2026, 3, 1))
        )
        assertEquals(
            listOf(2026 to 3, 2026 to 4),
            KemenagFetcher.monthsFor("1_WEEK", IGNORED_TARGET, LocalDate(2026, 3, 30))
        )
    }

    @Test
    fun aSpecificDateReadsItsYearAndMonthAndIgnoresItsDay() {
        // The API is addressed by month, so the day is not part of the question. Two dates in the
        // same month must produce the same walk, or a takmir picking the 1st and the 28th would
        // get different schedules.
        assertEquals(
            KemenagFetcher.monthsFor("SPECIFIC_DATE", "2026-04-01", MID_JANUARY),
            KemenagFetcher.monthsFor("SPECIFIC_DATE", "2026-04-28", MID_JANUARY)
        )
        assertEquals(
            months(2026, 1..12) + months(2027, 1..6),
            KemenagFetcher.monthsFor("SPECIFIC_DATE", "2027-06-30", MID_JANUARY)
        )
    }

    @Test
    fun aSpecificDateThatCannotBeParsedIsTheCurrentMonthAlone() {
        // Nothing validates `kemenagTargetDate` on the way in — it arrives from a date picker on
        // the display, from JSON over the WebSocket, and from a snapshot written by an older build.
        // The old code left its accumulator untouched, which had the same effect for a different
        // reason; this makes it the stated behaviour rather than a side effect.
        listOf(
            "",
            "besok",
            "2026",
            "-",
            "2026-",
            "2026-13-01",
            "2026-00-01",
            "20xx-04-01",
            "2026/04/01"
        ).forEach { target ->
            assertEquals(
                "target date: '$target'",
                listOf(2026 to 1),
                KemenagFetcher.monthsFor("SPECIFIC_DATE", target, MID_JANUARY)
            )
        }
    }

    @Test
    fun theWalkStopsAtTwentyFourMonths() {
        // A mistyped or far-future target must not turn into an unbounded walk hitting a public
        // API once per month. The cap is the only thing between "2099" in a date picker and 888
        // requests to api.myquran.com.
        val walk = KemenagFetcher.monthsFor("SPECIFIC_DATE", "2099-01-01", MID_JANUARY)
        assertEquals(KemenagFetcher.MAX_MONTHS, walk.size)
        assertEquals(2026 to 1, walk.first())
        assertEquals(2027 to 12, walk.last())
    }

    @Test
    fun theWalkIsContiguousAndEveryMonthIsARealMonth() {
        // A property over every case rather than a value: the walk is fed to the API as
        // `year-month`, so a 0 or a 13 is a request that answers with nothing.
        val walks = expected.keys.map { it to KemenagFetcher.monthsFor(it, targetFor(it), MID_JANUARY) } +
            expected.keys.map { it to KemenagFetcher.monthsFor(it, targetFor(it), LATE_DECEMBER) } +
            listOf("cap" to KemenagFetcher.monthsFor("SPECIFIC_DATE", "2099-01-01", MID_JANUARY))

        walks.forEach { (label, walk) ->
            assertTrue("$label produced nothing", walk.isNotEmpty())
            walk.forEach { (_, month) ->
                assertTrue("$label: month $month is not a month", month in 1..12)
            }
            walk.zipWithNext().forEach { (before, after) ->
                val step = (after.first * 12 + after.second) - (before.first * 12 + before.second)
                assertEquals("$label: $before then $after is not one month later", 1, step)
            }
        }
    }

    @Test
    fun aSpecificDateAlreadyPastAsksForNothing() {
        // Recorded, not endorsed: the walk's condition is false immediately, so the fetch silently
        // does nothing and the display keeps whatever schedule it had. The old Realm code behaved
        // the same way. It is a defect of the *feature* — the UI lets a past date be chosen and
        // says nothing — and it is pinned here so a change to it has to be deliberate.
        assertEquals(
            emptyList<Pair<Int, Int>>(),
            KemenagFetcher.monthsFor("SPECIFIC_DATE", "2025-01-01", MID_JANUARY)
        )
    }
}
