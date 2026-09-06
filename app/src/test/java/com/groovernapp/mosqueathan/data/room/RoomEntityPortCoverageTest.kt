package com.groovernapp.mosqueathan.data.room

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nine entities were transcribed from Realm to Room by hand. A field dropped on the way compiles,
 * and then reads back as its default forever.
 *
 * That is the same shape as the two bugs [com.groovernapp.mosqueathan.server.ConfigPayloadCoverageTest]
 * and [com.groovernapp.mosqueathan.server.SideTablePayloadCoverageTest] were written for, and it is
 * worse here: those lost a value in transit between two devices, this one loses it on disk. A
 * missing column is indistinguishable at runtime from a column whose value happens to equal the
 * default — a takmir's prayer offset reads 0, which is also what an untouched offset reads — so
 * there is no moment at which the absence can be observed. It has to be counted.
 *
 * The Realm side is read from source rather than reflected over, because the Realm compiler plugin
 * adds synthetic members to a `RealmObject` and they would have to be filtered back out by name;
 * the Room side is reflected over, because a `data class` has nothing but its own properties. Room's
 * annotations are `BINARY` retention and invisible to reflection, so anything annotation-shaped —
 * the primary key, the table name — is read from source on both sides.
 *
 * Deliberate differences live in the two allow-lists below. A silent pass is the failure mode this
 * test exists to prevent, so both allow-lists are themselves checked for staleness.
 */
class RoomEntityPortCoverageTest {

    private val realmDir = File("src/main/java/com/groovernapp/mosqueathan/data")
    private val roomDir = File(realmDir, "room")
    private val databaseSource = File(roomDir, "MosqueDatabase.kt")

    /**
     * The one column every Room entity gains and no Realm entity has. Anything *else* appearing on
     * a Room entity but not its Realm counterpart is a column two versions of this app will
     * disagree about, so it fails rather than being waved through.
     */
    private val addedColumns = mapOf(
        "lastModified" to
            "written by whichever device last touched the row; it is what a sync compares, and " +
            "Realm had nowhere to keep it"
    )

    /**
     * Each Room entity, paired with its Realm counterpart by simple name, plus any primary-key
     * change and the reason for it. A key that changes without a reason here is the bug.
     */
    /**
     * Tables Room introduces that Realm never had.
     *
     * Named rather than merely absent from [ported], because "not ported" and "forgotten" look
     * identical to a coverage check otherwise — and forgetting an entity is exactly what this
     * class exists to catch.
     */
    private val newInRoom = mapOf(
        TableStampEntity::class to
            "records when each table last changed, deletions included, which MAX(lastModified) " +
            "over the rows cannot express: delete the newest row and the maximum falls back to " +
            "the one beneath it, so a sync sees no change and restores what was deleted"
    )

    private val ported = listOf(
        Ported(MosqueConfigEntity::class),
        Ported(AnnouncementEntity::class),
        Ported(MediaEntity::class),
        Ported(FridayMediaEntity::class),
        Ported(DashboardBackgroundEntity::class),
        Ported(SlideshowScheduleEntity::class),
        Ported(StandbyScheduleEntity::class),
        Ported(ConnectedDeviceEntity::class),
        Ported(
            KemenagScheduleEntity::class,
            primaryKeyChange =
                "Realm keyed on dateString alone, so two cities could not hold a schedule for the " +
                "same day and changing city overwrote the previous one row at a time; the composite " +
                "key is the correction, and it is why this port is not a pure transcription"
        )
    )

    private data class Ported(val type: KClass<*>, val primaryKeyChange: String? = null)

    private val Ported.name: String get() = type.simpleName!!
    private val Ported.realmFile: File get() = File(realmDir, "$name.kt")
    private val Ported.roomFile: File get() = File(roomDir, "$name.kt")

    // --- vacuous-pass guards -------------------------------------------------------------------

    @Test
    fun theTestCanSeeEverySourceFileItReads() {
        // Without this the whole class passes by finding nothing the moment the layout moves.
        val absent = (ported.flatMap { listOf(it.realmFile, it.roomFile) } + databaseSource)
            .filterNot { it.isFile }
            .map { it.path }
        assertTrue("source files this test reads are missing: $absent", absent.isEmpty())
    }

    @Test
    fun theSourceParsersActuallyFindFieldsAndKeys() {
        // A regex that silently matches nothing would make every comparison below trivially true.
        val complaints = ported.mapNotNull {
            val fields = declaredFields(it.realmFile.readText())
            val keys = realmPrimaryKey(it.realmFile.readText())
            when {
                fields.isEmpty() -> "${it.name}: parsed no fields out of the Realm entity"
                keys.isEmpty() -> "${it.name}: parsed no @PrimaryKey out of the Realm entity"
                roomPrimaryKey(it.roomFile.readText()).isEmpty() ->
                    "${it.name}: parsed no primary key out of the Room entity"
                else -> null
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun everyRealmEntityStillOnDiskIsPaired() {
        // Catches the tenth Realm entity nobody ported, and a rename that would otherwise make this
        // test compare an entity against a file that no longer describes it.
        val onDisk = realmDir.listFiles { f: File -> f.isFile && f.extension == "kt" }
            .orEmpty()
            .filter { it.readText().contains(": RealmObject") }
            .map { it.nameWithoutExtension }
            .toSet()
        val unpaired = onDisk - ported.map { it.name }.toSet()
        assertTrue("Realm entities with no Room counterpart under test: $unpaired", unpaired.isEmpty())
    }

    @Test
    fun everyEntityInTheDatabaseIsPaired() {
        // The other direction: a table added to @Database that this test never looks at.
        val declared = Regex("""(\w+Entity)::class""")
            .findAll(databaseSource.readText())
            .map { it.groupValues[1] }
            .toSet()
        assertTrue("MosqueDatabase declares no entities — the parser missed them", declared.isNotEmpty())
        val untested = declared - ported.map { it.name }.toSet() - newInRoom.keys.mapNotNull { it.simpleName }.toSet()
        assertTrue("entities in MosqueDatabase that this test never checks: $untested", untested.isEmpty())
    }

    // --- the guards ----------------------------------------------------------------------------

    @Test
    fun everyRealmFieldSurvivedThePortWithItsType() {
        val complaints = ported.mapNotNull { entity ->
            val realm = declaredFields(entity.realmFile.readText())
            val room = roomFields(entity.type)
            val missing = realm.filterKeys { it !in room }.keys
            val retyped = realm.filter { (name, type) -> room[name]?.let { it != type } == true }
                .map { (name, type) -> "$name: $type became ${room[name]}" }
            when {
                missing.isNotEmpty() ->
                    "${entity.name} lost $missing — those columns will read back as defaults forever"
                retyped.isNotEmpty() -> "${entity.name} changed a field's type: $retyped"
                else -> null
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun theOnlyColumnRoomAddsIsAnAllowedOne() {
        val complaints = ported.mapNotNull { entity ->
            val realm = declaredFields(entity.realmFile.readText()).keys
            val extra = roomFields(entity.type).keys - realm - addedColumns.keys
            if (extra.isEmpty()) null
            else "${entity.name} has columns Realm never had and nothing declares: $extra"
        }
        assertTrue(
            complaints.joinToString("\n") +
                "\nAdd them to `addedColumns` with a reason, or take them off the entity.",
            complaints.isEmpty()
        )
    }

    @Test
    fun everyRoomEntityCarriesLastModified() {
        val complaints = ported.mapNotNull { entity ->
            when (roomFields(entity.type)["lastModified"]) {
                "Long" -> null
                null -> "${entity.name} has no lastModified, so a sync cannot tell when it changed"
                else -> "${entity.name}.lastModified is not a Long"
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun everyPrimaryKeyIsUnchangedUnlessTheChangeIsDeclared() {
        val complaints = ported.mapNotNull { entity ->
            val realm = realmPrimaryKey(entity.realmFile.readText())
            val room = roomPrimaryKey(entity.roomFile.readText())
            when {
                realm == room -> null
                entity.primaryKeyChange != null -> null
                else ->
                    "${entity.name} changed its primary key from $realm to $room with no reason " +
                        "given. A key change decides which rows survive the Realm-to-Room " +
                        "migration; declare it in `ported` or put it back."
            }
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun noDeclaredKeyChangeDescribesAKeyThatDidNotChange() {
        // A stale entry excuses the next real change, which is the same invisible gap as a missing
        // column — see the withheld-list guard in SideTablePayloadCoverageTest.
        val stale = ported
            .filter { it.primaryKeyChange != null }
            .filter { realmPrimaryKey(it.realmFile.readText()) == roomPrimaryKey(it.roomFile.readText()) }
            .map { it.name }
        assertTrue("these declare a primary-key change that is no longer a change: $stale", stale.isEmpty())
    }

    @Test
    fun noAddedColumnIsAllowedThatNoEntityHas() {
        val everyRoomField = ported.flatMap { roomFields(it.type).keys }.toSet()
        val stale = addedColumns.keys - everyRoomField
        assertTrue("`addedColumns` names columns no entity carries: $stale", stale.isEmpty())
    }

    // --- parsing -------------------------------------------------------------------------------

    /** `name to simple type` for every property declared in a Kotlin entity source. */
    private fun declaredFields(source: String): Map<String, String> =
        Regex("""^\s*(?:override\s+)?va[lr]\s+(\w+)\s*:\s*([\w.]+)""", RegexOption.MULTILINE)
            .findAll(source)
            .associate { it.groupValues[1] to it.groupValues[2].substringAfterLast('.') }

    /** The same shape, taken from the Room `data class` itself rather than from its source. */
    private fun roomFields(type: KClass<*>): Map<String, String> =
        type.memberProperties.associate {
            it.name to it.returnType.toString().removeSuffix("?").substringAfterLast('.')
        }

    private fun realmPrimaryKey(source: String): List<String> =
        Regex("""@PrimaryKey\s+(?:override\s+)?var\s+(\w+)""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toList()

    /** Either `@Entity(primaryKeys = [...])` or the property carrying `@PrimaryKey`. */
    private fun roomPrimaryKey(source: String): List<String> {
        val composite = Regex("""primaryKeys\s*=\s*\[([^]]*)]""").find(source)
        if (composite != null) {
            return Regex(""""(\w+)"""")
                .findAll(composite.groupValues[1])
                .map { it.groupValues[1] }
                .toList()
        }
        return Regex("""@PrimaryKey\s+val\s+(\w+)""")
            .findAll(source)
            .map { it.groupValues[1] }
            .toList()
    }
}
