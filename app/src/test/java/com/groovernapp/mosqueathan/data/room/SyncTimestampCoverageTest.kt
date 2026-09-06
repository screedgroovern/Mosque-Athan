package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.protocol.SyncConflict
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every table a sync compares must actually be stamped when it changes.
 *
 * This class used to parse the `UNION ALL` that `tableTimestamps()` was built from, because the
 * newest write per table was computed as `MAX(lastModified)` over the rows. That query is gone: a
 * maximum cannot see a deletion — remove the newest running text and the maximum falls back to the
 * row beneath it, so two devices comparing notes conclude nothing happened and the deleted row
 * comes back from the other side. There is a `table_stamps` table now, written by the repository.
 *
 * So the invariant moved with it. It is no longer "is this table in the query" but "does anything
 * ever stamp this table", and the failure it guards is the same either way: a section of settings
 * that silently never participates in a sync. Nothing throws, the dialog simply never mentions it,
 * and whichever side loses that section loses it every time.
 */
class SyncTimestampCoverageTest {

    private val repositorySource =
        File("src/main/java/com/groovernapp/mosqueathan/data/room/RoomConfigRepository.kt")

    private val daoSource =
        File("src/main/java/com/groovernapp/mosqueathan/data/room/MosqueDao.kt")

    @Test
    fun theTestCanSeeTheSourcesItReads() {
        // Without this the whole class passes the moment a file is renamed.
        assertTrue("cannot read ${repositorySource.absolutePath}", repositorySource.isFile)
        assertTrue("cannot read ${daoSource.absolutePath}", daoSource.isFile)
        assertTrue("SyncConflict compares nothing", SyncConflict.COMPARED_TABLES.isNotEmpty())
    }

    @Test
    fun theStampsTableIsWhatIsRead() {
        // The point of the change: if this ever goes back to a maximum over the rows, deletions
        // stop being visible to a sync and no other test would notice.
        val dao = daoSource.readText()
        assertTrue(
            "tableTimestamps() no longer reads table_stamps; a MAX() over rows cannot see a deletion",
            Regex("""FROM\s+table_stamps""").containsMatchIn(dao)
        )
        assertTrue(
            "nothing writes a table stamp",
            dao.contains("suspend fun stampTable(")
        )
    }

    @Test
    fun everyComparedTableIsStampedSomewhere() {
        val source = repositorySource.readText()
        // The repository names tables through constants, so resolve each to the literal it holds
        // rather than searching for the table name directly — otherwise a renamed constant would
        // read as a missing stamp and a renamed *table* would read as fine.
        val constants = Regex("""const val (TABLE_\w+) = "(\w+)"""")
            .findAll(source)
            .associate { it.groupValues[2] to it.groupValues[1] }

        val stamped = Regex("""touch\((TABLE_\w+)\)""").findAll(source).map { it.groupValues[1] }.toSet() +
            // resetToDefaults stamps the whole compared set in one loop
            if (Regex("""COMPARED_TABLES\.forEach\s*\{\s*touch\(""").containsMatchIn(source)) {
                constants.values.toSet()
            } else {
                emptySet()
            }

        val missing = SyncConflict.COMPARED_TABLES.filter { table ->
            val constant = constants[table]
            constant == null || constant !in stamped
        }
        assertTrue(
            "tables no sync will ever notice a change in: $missing\n" +
                "Each needs a touch(TABLE_…) on every path that writes or deletes one of its rows.",
            missing.isEmpty()
        )
    }

    @Test
    fun theRepositoryTakesItsTableListFromSyncConflict() {
        // Two lists that must agree is how a table quietly stops being part of a sync.
        assertTrue(
            "RoomConfigRepository declares its own compared-table list instead of using SyncConflict's",
            repositorySource.readText().contains("SyncConflict.COMPARED_TABLES")
        )
    }
}
