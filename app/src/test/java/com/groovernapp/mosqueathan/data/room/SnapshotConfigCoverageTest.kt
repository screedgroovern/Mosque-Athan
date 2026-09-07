package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.domain.MosqueConfig
import java.io.File
import kotlin.reflect.full.memberProperties
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every config field a sync carries must actually be written, or be withheld with a reason.
 *
 * The same class of bug `ConfigPayloadCoverageTest` guards on the way out, one step further in: a
 * field the *payload* carries and the *store* drops. Both are invisible for the same reason — a
 * field left out reads back as its default, and a default is indistinguishable from a value
 * someone chose. The takmir sees a setting they changed at home simply not take effect on the
 * display, once, with nothing logged.
 *
 * A compiler cannot catch it. `MosqueConfigEntity.copy()` takes every argument optionally, so a
 * `withSnapshot` that forgets a field is a complete, warning-free expression. This reads source
 * for that reason, like [RoomSwitchReadinessTest] and `ActionNameDriftTest`.
 *
 * The Realm implementation is checked too, and not for symmetry's sake: 2.0 keeps Realm on the
 * classpath as the way back if Room misbehaves on a television, and a rollback that silently
 * stopped syncing five fields would be the worst possible moment to discover this.
 */
class SnapshotConfigCoverageTest {

    /**
     * Fields a snapshot must **not** be allowed to write, each with the reason.
     *
     * Note what is *not* here: `customBackgroundPath` and `murottalAudioPaths` are written by both
     * implementations, because which side owns a file path depends on the direction of travel. A
     * display strips them from an incoming push in `SnapshotPush.sanitisedConfig`; a controller
     * receiving the display's snapshot adopts them, which is what lets it show the right
     * thumbnails. Withholding them in the store would break the download direction to protect the
     * push direction.
     */
    private val withheld = mapOf(
        "id" to "the primary key, always \"config\"",
        "pinCode" to
            "never crosses the wire at all — the display sends hasPin — so a snapshot's value is " +
            "always the empty default, and adopting it would unlock the mosque's own settings",
        "connectivityMode" to
            "describes this device's own role; a display that adopted a controller's would " +
            "declare itself a remote and start posting to itself",
        "remoteTvIp" to "describes which display this device controls, not the other's"
    )

    private val roomMappers =
        File("src/main/java/com/groovernapp/mosqueathan/data/room/RoomMappers.kt")

    private val realmRepository =
        File("src/main/java/com/groovernapp/mosqueathan/data/MosqueConfigRepository.kt")

    /** The `withSnapshot` body, so a field assigned elsewhere in the file cannot excuse a gap. */
    private fun roomAssignments(): Set<String> {
        val body = roomMappers.readText().substringAfter("fun MosqueConfigEntity.withSnapshot(")
        return Regex("""^\s{4}(\w+) = incoming\.""", RegexOption.MULTILINE)
            .findAll(body).map { it.groupValues[1] }.toSet()
    }

    /** The Realm `applySnapshot` body only — `updateConfig` above it writes 47 of its own. */
    private fun realmAssignments(): Set<String> {
        val source = realmRepository.readText()
        val body = source
            .substringAfter("override suspend fun applySnapshot(")
            .substringBefore("if (announcements != null)")
        return Regex("""entity\.(\w+) = config\.""").findAll(body).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun theTestCanSeeBothImplementations() {
        // Without this the whole class passes vacuously if either file moves or is renamed.
        assertTrue("cannot read ${roomMappers.absolutePath}", roomMappers.isFile)
        assertTrue("cannot read ${realmRepository.absolutePath}", realmRepository.isFile)
        assertTrue("withSnapshot not found in RoomMappers.kt", roomAssignments().isNotEmpty())
        assertTrue("applySnapshot not found in MosqueConfigRepository.kt", realmAssignments().isNotEmpty())
    }

    @Test
    fun roomWritesEveryConfigFieldASnapshotCarries() {
        val missing = MosqueConfig::class.memberProperties.map { it.name }
            .filter { it !in roomAssignments() && it !in withheld }

        assertTrue(
            "config fields a sync would silently drop on Room: $missing\n" +
                "Add them to MosqueConfigEntity.withSnapshot, or to `withheld` with a reason.",
            missing.isEmpty()
        )
    }

    @Test
    fun realmWritesEveryConfigFieldASnapshotCarries() {
        val missing = MosqueConfig::class.memberProperties.map { it.name }
            .filter { it !in realmAssignments() && it !in withheld }

        assertTrue(
            "config fields a sync would silently drop on Realm: $missing\n" +
                "Add them to MosqueConfigRepository.applySnapshot, or to `withheld` with a reason.",
            missing.isEmpty()
        )
    }

    @Test
    fun neitherImplementationWritesAWithheldField() {
        val both = roomAssignments() + realmAssignments()
        val leaked = withheld.keys.filter { it in both }

        assertTrue(
            "a snapshot must never be allowed to write these, and one of the two stores does: " +
                leaked.map { "$it (${withheld[it]})" },
            leaked.isEmpty()
        )
    }

    @Test
    fun theWithheldListDoesNotNameFieldsThatNoLongerExist() {
        val fields = MosqueConfig::class.memberProperties.map { it.name }.toSet()
        val stale = withheld.keys.filterNot { it in fields }

        // A stale entry is worse than no entry: it would excuse a real gap on a field renamed
        // later, and read as a decision while protecting nothing.
        assertTrue("withheld names fields MosqueConfig no longer has: $stale", stale.isEmpty())
    }

    @Test
    fun theTwoStoresAgreeOnWhatASyncCarries() {
        val room = roomAssignments()
        val realm = realmAssignments()

        // Realm is the way back if Room misbehaves on a television. A rollback that quietly
        // stopped syncing a handful of fields is the failure this pins.
        assertTrue(
            "Room writes fields Realm does not: ${room - realm}",
            (room - realm).isEmpty()
        )
        assertTrue(
            "Realm writes fields Room does not: ${realm - room}",
            (realm - room).isEmpty()
        )
    }
}
