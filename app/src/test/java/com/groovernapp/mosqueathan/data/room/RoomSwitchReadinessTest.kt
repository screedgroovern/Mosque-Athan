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
     * Members those consumers call that `RoomConfigRepository` does not have, with what each is
     * for. Nine, and none of them is hard — they are simply not written yet.
     */
    private val stillMissing = mapOf(
        "getActiveAnnouncements" to "the panel's announcement list, read once rather than observed",
        "getConnectedDevices" to "the session-expiry sweep, every 30 seconds",
        "getKemenagSchedule" to "what PrayerCalculator reads through KemenagScheduleSource — so a " +
            "Room-backed source has to exist before the display can switch",
        "getKemenagSchedules" to "the panel's schedule table",
        "registerDevice" to "pairing, on the WebSocket's register action",
        "saveKemenagSchedules" to "storing a fetch the server performed itself",
        "updateDashboardBackgroundPath" to "the /upload replace path",
        "updateDeviceAllowed" to "verify_pin, and the session-expiry sweep",
        "updateDeviceOnlineStatus" to "a controller connecting and disconnecting"
    )

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
            "The gap between RoomConfigRepository and the concrete-typed consumers has changed.\n" +
                "If a member was implemented, remove it from `stillMissing`. If a new call " +
                "appeared, either implement it or document it here — a switch cannot be justified " +
                "against a list nobody updated.",
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
