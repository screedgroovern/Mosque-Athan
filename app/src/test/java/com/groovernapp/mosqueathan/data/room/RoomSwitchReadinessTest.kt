package com.groovernapp.mosqueathan.data.room

import java.io.File
import kotlin.reflect.full.memberFunctions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What still stands between the two implementations, as a list that cannot go stale in either
 * direction.
 *
 * ### Why this cannot be noticed at runtime
 *
 * `RoomConfigRepository` satisfies `ConfigRepository`, so "it compiles" says the display and the
 * settings UI could switch to it today. It says nothing about `SettingsServer` and
 * `RealmKemenagScheduleSource`, which take the **concrete** `MosqueConfigRepository` — deliberately,
 * because the server is Android-only and never moving — and therefore call members that are not on
 * the interface at all. Nine of them do not exist on the Room class.
 *
 * The failure this prevents is not a crash: it is a Koin binding swapped in a one-line change,
 * `:app` failing to compile, and the reason being nine unrelated-looking errors in a 1,900-line
 * file. Or worse, the switch being made for the display only, leaving the server writing to Realm
 * while the display reads Room — two stores, one mosque, no error anywhere.
 *
 * Both directions fail, which is the point: a new concrete-typed call on the server appears here,
 * and closing a gap here fails too, so the list shrinks when the work is done rather than
 * describing a state that has passed.
 */
class RoomSwitchReadinessTest {

    /** The consumers that take `MosqueConfigRepository` rather than the interface. */
    private val concreteConsumers = listOf(
        File("src/main/java/com/groovernapp/mosqueathan/server/SettingsServer.kt"),
        File("src/main/java/com/groovernapp/mosqueathan/data/RealmKemenagScheduleSource.kt")
    )

    /**
     * Every other file that names the concrete repository, which this test used to miss entirely.
     *
     * It listed two consumers by hand, and `MainActivity` was not one of them — so when the store
     * was switched, `MainActivity` went on injecting `MosqueConfigRepository` for the server while
     * `AppViewModel` took `DisplayDataSource` for the display. The app ran **both** stores at once:
     * a setting changed through the panel landed in Realm, and the television calculated from Room
     * and showed prayer times from the configuration as it had been before. Nothing failed and each
     * half looked correct on its own.
     *
     * A hand-picked list of consumers cannot catch that. This searches the whole source tree
     * instead, so a new concrete reference anywhere is a failure rather than a discovery made on a
     * television.
     */
    private val allowedToNameTheConcreteClass = setOf(
        // Its own file, and the DI module that chooses between the two stores.
        "MosqueConfigRepository.kt",
        "AppModule.kt",
        // The Realm-only adapter, which exists to be swapped for the Room one.
        "RealmKemenagScheduleSource.kt",
        // The migration reads the old entities directly; that is its whole job.
        "RealmToRoomMigration.kt"
    )

    /**
     * Comments removed before matching, as in [ActionNameDriftTest] and for the same reason.
     *
     * This reads source, and every file that *explains* the switch names the class it moved away
     * from — including this test's own subject. Matching those would make the guard fire on its
     * own documentation, which is how a source-reading test earns the reputation of crying wolf.
     */
    private fun stripComments(source: String): String {
        val noBlocks = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).replace(source, "")
        return Regex("//[^\n]*").replace(noBlocks, "")
    }

    @Test
    fun nothingElseNamesTheConcreteRepository() {
        val root = File("src/main/java")
        assertTrue("cannot read ${root.absolutePath}", root.isDirectory)
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.name !in allowedToNameTheConcreteClass }
            .filter { "MosqueConfigRepository" in stripComments(it.readText()) }
            .map { it.name }
            .toList()
        assertTrue(
            "these name MosqueConfigRepository directly, so switching the store leaves them on " +
                "Realm while the rest moves: $offenders\nReach for ServerDataSource, " +
                "ConfigRepository or DisplayDataSource instead — whichever is the least authority " +
                "that file needs.",
            offenders.isEmpty()
        )
    }

    /**
     * Members those consumers call that `RoomConfigRepository` does not have.
     *
     * **Empty, and that is the assertion.** It held nine entries — device registration, presence,
     * the raw Kemenag rows and the panel's announcement list — which were the whole reason the
     * store could not be swapped: `SettingsServer` and `RealmKemenagScheduleSource` asked for the
     * concrete Realm class, so a Room repository could satisfy every settings member and still be
     * unusable. `ServerDataSource` and `RoomKemenagScheduleSource` closed it.
     *
     * Leaving the map here rather than deleting the test is the point: a new call on a concrete
     * repository type reopens the gap silently, and the next person to try switching would find
     * out by the display failing rather than by a test. An entry added here needs a reason beside
     * it, exactly as the nine did.
     */
    private val stillMissing = emptyMap<String, String>()

    private val roomMembers: Set<String> by lazy {
        RoomConfigRepository::class.memberFunctions.map { it.name }.toSet()
    }

    /** Every `repository.member(` call in the concrete consumers. */
    private fun concreteCalls(): Set<String> = concreteConsumers
        .flatMap { file ->
            Regex("""\brepository\.([a-zA-Z][a-zA-Z0-9]*)""")
                .findAll(file.readText())
                .map { it.groupValues[1] }
        }
        .toSet()

    @Test
    fun theSourcesThisReadsAreReadable() {
        // Without this an unreadable or renamed file empties `concreteCalls()`, and the comparison
        // below then reports every documented gap as closed — the exact opposite of the truth.
        val unreadable = concreteConsumers.filterNot { it.isFile }.map { it.path }
        assertTrue("cannot read: $unreadable", unreadable.isEmpty())
        assertTrue("no repository calls parsed out of the consumers", concreteCalls().size > 20)
        assertTrue("no members found on RoomConfigRepository", roomMembers.size > 50)
    }

    @Test
    fun theGapIsExactlyWhatIsDocumented() {
        assertEquals(
            "A consumer calls something RoomConfigRepository does not have, so the store can no " +
                "longer be switched. Either implement it, or reach for it through an interface " +
                "both stores satisfy — and if it genuinely has to stay concrete, add it to " +
                "`stillMissing` with a reason.",
            stillMissing.keys.sorted(),
            (concreteCalls() - roomMembers).sorted()
        )
    }

    @Test
    fun everyOtherConcreteCallIsAlreadySatisfied() {
        // The other half of the same fact, stated so that a reader does not have to invert the
        // assertion above: everything not on the list is present, so the list is the whole job.
        val satisfied = concreteCalls() - stillMissing.keys
        val absent = satisfied.filterNot { it in roomMembers }
        assertTrue("present in neither the Room class nor the documented gap: $absent", absent.isEmpty())
    }
}
