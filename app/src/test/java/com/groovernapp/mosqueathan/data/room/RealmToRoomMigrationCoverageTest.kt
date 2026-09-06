package com.groovernapp.mosqueathan.data.room

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one place a lost column costs a real mosque its settings.
 *
 * `RealmToRoomMigration` copies a 1.x device's data across field by field, and **every parameter of
 * every Room entity has a default**. So a field left out of the config's fifty-six named arguments
 * compiles, runs, reports `Migrated`, and writes the default over what the takmir had configured —
 * a prayer offset of 0 where they had set −2, and nothing anywhere says so. This runs once per
 * device, so there is no second chance and no moment at which the loss is visible: the display
 * simply shows slightly wrong prayer times from then on.
 *
 * This is the same guard `EntityMappersTest` applies to the Realm-to-domain mapper, for the same
 * reason and against the same failure — the compiler cannot help while defaults exist, and the
 * defaults exist so that Room can construct a row.
 *
 * The seven side tables are copied *positionally*, which adds a second failure the compiler cannot
 * see: `MediaEntity(id, filePath, isVideo, isUsed, sortOrder, now)` has two adjacent `Boolean`s, so
 * swapping `isVideo` and `isUsed` type-checks perfectly and turns every video into a still. Order is
 * therefore asserted, not just completeness.
 */
class RealmToRoomMigrationCoverageTest {

    private val roomDir = File("src/main/java/com/groovernapp/mosqueathan/data/room")
    private val migrationSource = File(roomDir, "RealmToRoomMigration.kt")

    private val migrated: List<KClass<*>> = listOf(
        MosqueConfigEntity::class,
        AnnouncementEntity::class,
        MediaEntity::class,
        FridayMediaEntity::class,
        DashboardBackgroundEntity::class,
        SlideshowScheduleEntity::class,
        StandbyScheduleEntity::class,
        KemenagScheduleEntity::class
    )

    /** Tables deliberately left behind. Not carrying one over has to be a decision. */
    private val notMigrated = mapOf(
        ConnectedDeviceEntity::class to
            "access-control bookkeeping the display rebuilds as devices register; the live display " +
            "holds eighteen rows, most for browsers closed months ago"
    )

    private val source: String by lazy { migrationSource.readText() }

    private fun parameters(type: KClass<*>): List<String> =
        type.primaryConstructor!!.parameters.map { it.name!! }

    // --- vacuous-pass guards -------------------------------------------------------------------

    @Test
    fun theTestCanSeeTheMigrationSource() {
        assertTrue("cannot read ${migrationSource.absolutePath}", migrationSource.isFile)
    }

    @Test
    fun everyMigratedEntityIsActuallyConstructedInTheSource() {
        // Without this, renaming an entity makes the whole class pass by finding no call to check.
        val absent = migrated.filter { assignedParameters(it) == null }.map { it.simpleName }
        assertTrue("no constructor call found in the migration for: $absent", absent.isEmpty())
    }

    @Test
    fun noEntityIsBothMigratedAndDeclaredLeftBehind() {
        val both = migrated.intersect(notMigrated.keys).map { it.simpleName }
        assertTrue("listed as migrated and as left behind at once: $both", both.isEmpty())
    }

    @Test
    fun everyEntityInTheDatabaseIsEitherMigratedOrDeclaredLeftBehind() {
        // The list that matters is the database's, not this file's — a tenth table added later is
        // otherwise silently not carried across.
        val declared = Regex("""(\w+Entity)::class""")
            .findAll(File(roomDir, "MosqueDatabase.kt").readText())
            .map { it.groupValues[1] }
            .toSet()
        assertTrue("MosqueDatabase declares no entities — the parser missed them", declared.isNotEmpty())
        val accounted = (migrated + notMigrated.keys).map { it.simpleName }.toSet()
        assertTrue("entities nothing decides about: ${declared - accounted}", (declared - accounted).isEmpty())
    }

    @Test
    fun nothingIsDeclaredLeftBehindThatTheMigrationActuallyCopies() {
        // A stale reason is worse than none: it states as a decision something that is no longer
        // true, and the next reader trusts it.
        val copied = notMigrated.keys.filter { assignedParameters(it) != null }.map { it.simpleName }
        assertTrue("declared as left behind, but the migration constructs them: $copied", copied.isEmpty())
    }

    // --- the guards ----------------------------------------------------------------------------

    @Test
    fun everyColumnIsAssignedFromTheOldDatabase() {
        val complaints = migrated.mapNotNull { type ->
            val assigned = assignedParameters(type)?.toSet() ?: return@mapNotNull null
            val missing = parameters(type) - assigned
            if (missing.isEmpty()) null
            else "${type.simpleName} never assigns $missing — those read back as defaults on every " +
                "device that upgrades, and the old value is gone"
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun everyEntityIsCopiedWithNamedArguments() {
        // This began as a check that positional copies were in the right order, and every copy has
        // since been converted to named arguments — which turned it into a test that skipped
        // everything and passed on nothing. Requiring the names instead removes the class of bug
        // rather than detecting it: `MediaEntity(it.id, it.filePath, it.isVideo, it.isUsed, …)` has
        // two adjacent Booleans, and swapping them type-checks and turns every video into a still.
        val complaints = migrated.mapNotNull { type ->
            if (assignedParameters(type) == null) return@mapNotNull null
            if (isNamed(type)) null
            else "${type.simpleName} is copied positionally; use named arguments, because two " +
                "fields of the same type will otherwise swap without the compiler noticing"
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    @Test
    fun everyMigratedRowGetsAMigrationTimestamp() {
        // lastModified left at its 0L default would make every migrated row look older than
        // anything a controller holds, so the first sync after an upgrade would overwrite it.
        val complaints = migrated.mapNotNull { type ->
            val assigned = assignedParameters(type) ?: return@mapNotNull null
            if ("lastModified" in assigned) null
            else "${type.simpleName} does not set lastModified"
        }
        assertTrue(complaints.joinToString("\n"), complaints.isEmpty())
    }

    // --- parsing -------------------------------------------------------------------------------

    /** True when the migration constructs this entity with named arguments. */
    private fun isNamed(type: KClass<*>): Boolean =
        rawArguments(type)?.firstOrNull()?.let { Regex("""^\w+\s*=[^=]""").containsMatchIn(it.trim()) } == true

    /**
     * The entity's parameter names in the order the migration assigns them.
     *
     * `now` is the migration's single timestamp and stands for `lastModified`; everything else is
     * either `name = …` or a bare `it.name` read off the Realm object, and the two databases use
     * the same names throughout.
     */
    private fun assignedParameters(type: KClass<*>): List<String>? =
        rawArguments(type)?.map { argument ->
            val named = Regex("""^(\w+)\s*=[^=]""").find(argument.trim())
            when {
                named != null -> named.groupValues[1]
                argument.trim() == "now" -> "lastModified"
                else -> argument.trim().substringAfterLast('.')
            }
        }

    /** The top-level arguments of the entity's constructor call, unparsed. */
    private fun rawArguments(type: KClass<*>): List<String>? {
        val marker = "${type.simpleName}("
        val start = source.indexOf(marker)
        if (start < 0) return null
        var depth = 0
        var i = start + marker.length - 1
        val arguments = mutableListOf<String>()
        val current = StringBuilder()
        while (i < source.length) {
            val c = source[i]
            when {
                c == '(' || c == '[' || c == '{' -> {
                    depth++
                    if (depth > 1) current.append(c)
                }
                c == ')' || c == ']' || c == '}' -> {
                    depth--
                    if (depth == 0) {
                        if (current.isNotBlank()) arguments += current.toString()
                        return arguments
                    }
                    current.append(c)
                }
                c == ',' && depth == 1 -> {
                    arguments += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        return null
    }
}
