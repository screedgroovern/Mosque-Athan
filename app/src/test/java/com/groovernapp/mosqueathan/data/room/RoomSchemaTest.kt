package com.groovernapp.mosqueathan.data.room

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What version 1 actually shipped, read back out of the exported schema.
 *
 * The exported JSON is the only record a future migration can be written against, and it records
 * what SQLite was told rather than what the entity source appears to say — which is the point here.
 * `KemenagScheduleEntity` moves its primary key from one column to two, and that change is worth
 * nothing unless the table is genuinely created with a composite key; an `@Entity` argument that
 * did not take effect looks identical in the source either way.
 *
 * **This cannot catch a stale or missing schema file, and it must not be read as if it does.**
 * `:app:copyRoomSchemas` re-copies `schemas/…/1.json` from the KSP output whenever the two differ,
 * so by the time a unit test runs the file always agrees with the entities. Measured, not assumed:
 * deleting `media.lastModified` from the committed JSON and running the tests restored the column
 * and passed. What this class asserts is therefore the *decisions* — nine tables, `lastModified`
 * stored as a non-null INTEGER on every one of them, one composite key and only one — against the
 * schema as Room really recorded them. Change an entity and these fail; edit the JSON and the
 * build simply corrects you.
 */
class RoomSchemaTest {

    private val roomDir = File("src/main/java/com/groovernapp/mosqueathan/data/room")
    private val databaseSource = File(roomDir, "MosqueDatabase.kt")
    private val schemaDir = File("schemas/com.groovernapp.mosqueathan.data.room.MosqueDatabase")

    private val entities: List<KClass<*>> = listOf(
        MosqueConfigEntity::class,
        AnnouncementEntity::class,
        MediaEntity::class,
        FridayMediaEntity::class,
        DashboardBackgroundEntity::class,
        SlideshowScheduleEntity::class,
        StandbyScheduleEntity::class,
        ConnectedDeviceEntity::class,
        KemenagScheduleEntity::class,
        TableStampEntity::class
    )

    /** The one entity whose key is deliberately composite; see `KemenagScheduleEntity`'s KDoc. */
    private val compositeKeys = mapOf(
        "kemenag_schedules" to listOf("dateString", "cityId")
    )

    private val declaredVersion: Int by lazy {
        val match = Regex("""version\s*=\s*(\d+)""").find(databaseSource.readText())
        requireNotNull(match) { "no version = N found in ${databaseSource.path}" }
            .groupValues[1].toInt()
    }

    private val schemaFile: File by lazy { File(schemaDir, "$declaredVersion.json") }

    private val database: JSONObject by lazy {
        JSONObject(schemaFile.readText()).getJSONObject("database")
    }

    private fun tables(): Map<String, JSONObject> {
        val array = database.getJSONArray("entities")
        return (0 until array.length())
            .map { array.getJSONObject(it) }
            .associateBy { it.getString("tableName") }
    }

    private fun columns(table: JSONObject): Map<String, JSONObject> {
        val fields = table.getJSONArray("fields")
        return (0 until fields.length())
            .map { fields.getJSONObject(it) }
            .associateBy { it.getString("columnName") }
    }

    private fun primaryKey(table: JSONObject): List<String> {
        val names = table.getJSONObject("primaryKey").getJSONArray("columnNames")
        return (0 until names.length()).map { names.getString(it) }
    }

    /** `tableName` as written in each entity's `@Entity`, which reflection cannot see. */
    private fun declaredTableName(type: KClass<*>): String? =
        Regex("""tableName\s*=\s*"(\w+)"""")
            .find(File(roomDir, "${type.simpleName}.kt").readText())
            ?.groupValues?.get(1)

    // --- vacuous-pass guards -------------------------------------------------------------------

    @Test
    fun theSchemaOnDiskIsForTheVersionTheDatabaseDeclares() {
        // Every assertion below reads `schemas/<version>.json`, so this is the guard that stops
        // them describing some other version's database — after a bump, silently and in full.
        assertTrue("cannot read ${databaseSource.absolutePath}", databaseSource.isFile)
        assertTrue(
            "MosqueDatabase declares version $declaredVersion but ${schemaFile.path} does not " +
                "exist. Build :app once and commit what Room exports.",
            schemaFile.isFile
        )
        assertEquals(
            "the exported schema is for a different version than @Database declares",
            declaredVersion,
            database.getInt("version")
        )
    }

    @Test
    fun everyEntityDeclaresATableNameThisTestCanRead() {
        val unreadable = entities.filter { declaredTableName(it) == null }.map { it.simpleName }
        assertTrue("no tableName parsed out of: $unreadable", unreadable.isEmpty())
    }

    // --- the guards ----------------------------------------------------------------------------

    @Test
    fun theSchemaHoldsExactlyTheTablesTheEntitiesDeclare() {
        val expected = entities.mapNotNull { declaredTableName(it) }.toSet()
        assertEquals(
            "the exported schema and the entity list disagree about which tables exist",
            expected,
            tables().keys
        )
    }

    @Test
    fun everyTableHasTheColumnsItsEntityDeclares() {
        val tables = tables()
        val complaints = entities.mapNotNull { type ->
            val table = tables[declaredTableName(type)]
            val expected = type.memberProperties.map { it.name }.toSet()
            val actual = table?.let { columns(it).keys }
            when {
                actual == null -> "${type.simpleName}: no table in the schema"
                expected == actual -> null
                else -> "${type.simpleName}: schema is missing ${expected - actual}, " +
                    "and holds ${actual - expected} that the entity does not"
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun everyTableRecordsLastModifiedAsANonNullInteger() {
        // Stored, not just declared: a nullable or TEXT column here would make the sync comparison
        // wrong rather than absent, which is harder to notice than a missing column.
        val complaints = tables().mapNotNull { (name, table) ->
            val column = columns(table)["lastModified"]
                ?: return@mapNotNull "$name has no lastModified column"
            when {
                column.getString("affinity") != "INTEGER" ->
                    "$name.lastModified is ${column.getString("affinity")}, not INTEGER"
                !column.getBoolean("notNull") -> "$name.lastModified is nullable"
                else -> null
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun theKemenagKeyIsRecordedAsComposite() {
        // The whole point of the change: on a single-column key the second city's rows overwrite
        // the first city's one day at a time, and nothing reports it.
        compositeKeys.forEach { (table, expected) ->
            val entity = tables()[table] ?: error("no $table in the exported schema")
            assertEquals("$table's primary key", expected, primaryKey(entity))
        }
    }

    @Test
    fun everyOtherTableKeepsASingleColumnKey() {
        val unexpected = tables()
            .filterKeys { it !in compositeKeys }
            .filterValues { primaryKey(it).size != 1 }
            .keys
        assertTrue(
            "these gained a composite key with nothing declaring it: $unexpected",
            unexpected.isEmpty()
        )
    }

    @Test
    fun noCompositeKeyIsDeclaredForATableThatDoesNotHaveOne() {
        val stale = compositeKeys.keys - tables().keys
        assertTrue("`compositeKeys` names tables that no longer exist: $stale", stale.isEmpty())
    }
}
