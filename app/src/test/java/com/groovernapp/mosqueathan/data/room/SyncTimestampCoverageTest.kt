package com.groovernapp.mosqueathan.data.room

import java.io.File
import kotlin.reflect.KClass
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `tableTimestamps()` is what a sync compares, and it names its tables in a hand-written SQL string.
 *
 * Room's compiler checks that each name is a table that exists. Nothing checks the other direction:
 * a table added later and not added to the UNION is simply never asked when it last changed, so a
 * controller and the display agree they are in sync while one of them holds newer rows. There is no
 * error, no empty result, and nothing on screen — the query returns seven perfectly good rows, as
 * it did before.
 *
 * So this is the same shape as
 * [com.groovernapp.mosqueathan.server.ConfigPayloadCoverageTest]: every table is either compared or
 * deliberately left out for a stated reason, and the withheld list is checked for staleness so a
 * renamed table cannot inherit an excuse written for something else.
 */
class SyncTimestampCoverageTest {

    private val roomDir = File("src/main/java/com/groovernapp/mosqueathan/data/room")
    private val daoSource = File(roomDir, "MosqueDao.kt")

    private val entities: List<KClass<*>> = listOf(
        MosqueConfigEntity::class,
        AnnouncementEntity::class,
        MediaEntity::class,
        FridayMediaEntity::class,
        DashboardBackgroundEntity::class,
        SlideshowScheduleEntity::class,
        StandbyScheduleEntity::class,
        ConnectedDeviceEntity::class,
        KemenagScheduleEntity::class
    )

    /**
     * Tables two devices do not need to agree about. Both reasons are stated on the DAO itself;
     * they are repeated here because leaving a table out of the comparison has to be a decision.
     */
    private val withheld = mapOf(
        "connected_devices" to "access control is server-authoritative and never syncs",
        "kemenag_schedules" to "a cache the display re-fetches, not state a controller edits"
    )

    private fun declaredTableName(type: KClass<*>): String? =
        Regex("""tableName\s*=\s*"(\w+)"""")
            .find(File(roomDir, "${type.simpleName}.kt").readText())
            ?.groupValues?.get(1)

    /** The tables named in the `tableTimestamps()` query, taken from its own body. */
    private fun comparedTables(): Set<String> {
        val source = daoSource.readText()
        val marker = source.indexOf("fun tableTimestamps()")
        if (marker < 0) return emptySet()
        val query = source.lastIndexOf("\"\"\"", marker).let { close ->
            val open = source.lastIndexOf("\"\"\"", close - 1)
            if (open < 0 || close < 0) return emptySet()
            source.substring(open, close)
        }
        return Regex("""FROM\s+(\w+)""").findAll(query).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun theTestCanSeeTheDaoAndItsQuery() {
        // Both halves, because a readable file whose query no longer parses would pass every
        // comparison below by comparing against nothing.
        assertTrue("cannot read ${daoSource.absolutePath}", daoSource.isFile)
        assertTrue(
            "no tables parsed out of tableTimestamps() — the query or its name has moved",
            comparedTables().isNotEmpty()
        )
    }

    @Test
    fun everyTableIsEitherComparedOrDeliberatelyWithheld() {
        val compared = comparedTables()
        val missing = entities
            .mapNotNull { declaredTableName(it) }
            .filter { it !in compared && it !in withheld }
        assertTrue(
            "tables no sync will ever notice a change in: $missing\n" +
                "Add them to tableTimestamps(), or withhold them with a reason.",
            missing.isEmpty()
        )
    }

    @Test
    fun theQueryDoesNotNameATableThatNoLongerExists() {
        val known = entities.mapNotNull { declaredTableName(it) }.toSet()
        val unknown = comparedTables() - known
        assertTrue("tableTimestamps() reads tables that are not entities: $unknown", unknown.isEmpty())
    }

    @Test
    fun theWithheldListDoesNotNameATableThatNoLongerExists() {
        val known = entities.mapNotNull { declaredTableName(it) }.toSet()
        val stale = withheld.keys - known
        assertTrue("`withheld` names tables that no longer exist: $stale", stale.isEmpty())
    }

    @Test
    fun noWithheldTableIsAlsoCompared() {
        // Contradiction rather than a gap, and it would leave the withheld reason describing
        // behaviour the query does not have.
        val both = withheld.keys.intersect(comparedTables())
        assertTrue("these are withheld and compared at the same time: $both", both.isEmpty())
    }
}
