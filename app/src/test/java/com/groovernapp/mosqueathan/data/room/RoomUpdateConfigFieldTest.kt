package com.groovernapp.mosqueathan.data.room

import com.groovernapp.mosqueathan.domain.MosqueConfig
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `updateConfig(config)` copies the fields it owns and leaves the ten that belong elsewhere.
 *
 * ### Why this cannot be noticed at runtime
 *
 * Every caller of `updateConfig(config)` builds its argument from `getConfig()` and changes one
 * thing — so in the ordinary case a total write is *indistinguishable* from a partial one, because
 * the other fields already hold the values it would write. It goes wrong only when the caller's
 * copy is stale, and the two callers whose copy is always stale are exactly the ones that matter:
 * `SettingsServer`'s remote-sync handler, which assembles a config from JSON that deliberately
 * omits the PIN and the theme, and a controller in `REMOTE` mode, which never had them.
 *
 * The failure is therefore: a takmir changes the Friday warning text from a phone, and the
 * display's remote-control PIN becomes empty, or its slideshow transition reverts to FADE. No
 * error, no log, and the field that changed is not the field anyone was looking at.
 *
 * ### The list
 *
 * Ten fields, verified against `MosqueConfigRepository.updateConfig` and the note in
 * `EntityMappers.kt`. Two of them — `connectivityMode` and `remoteTvIp` — the Realm
 * implementation *does* copy while this one does not. That divergence is deliberate here (they
 * describe *this* device, and the web controller already has to carry its own values across a
 * snapshot) and it is reported rather than asserted as equivalent, because a reader comparing the
 * two implementations should find it written down.
 *
 * The comparison is over every property of the entity, reflectively, so a config field added later
 * lands in one column or the other rather than in neither.
 */
class RoomUpdateConfigFieldTest {

    private companion object {
        const val WRITE_AT = 4_242L

        /** Distinguishes "the stored row" from "the incoming object" in every generated value. */
        const val STORED = 1
        const val INCOMING = 2
    }

    /** A field the whole-object write must not touch, and what does write it instead. */
    private class Owner(
        val setter: String,
        val invoke: (suspend (RoomConfigRepository) -> Unit)?
    )

    private val theme: suspend (RoomConfigRepository) -> Unit = {
        it.updateThemeSettings("SLIDE", 99, videoWaitUntilEnd = true, includeDefaultImage = true)
    }

    private val ownedElsewhere: Map<String, Owner> = mapOf(
        "id" to Owner("nothing — it is the primary key", null),
        "lastModified" to Owner("every write stamps it; see RoomWriteStampTest", null),
        "customBackgroundPath" to Owner("updateCustomBackground") {
            it.updateCustomBackground("/nonexistent/chosen.jpg")
        },
        "transitionType" to Owner("updateThemeSettings", theme),
        "slideIntervalSeconds" to Owner("updateThemeSettings", theme),
        "videoWaitUntilEnd" to Owner("updateThemeSettings", theme),
        "includeDefaultImage" to Owner("updateThemeSettings", theme),
        "showSettingsOnTv" to Owner("updateShowSettingsOnTv") { it.updateShowSettingsOnTv(true) },
        "pinCode" to Owner("updatePinCode") { it.updatePinCode("1234", "d1") },
        "connectivityMode" to Owner("updateConnectivityMode") { it.updateConnectivityMode("REMOTE") },
        "remoteTvIp" to Owner("updateRemoteTvIp") { it.updateRemoteTvIp("192.168.1.9") }
    )

    // --- two instances that differ in every field ----------------------------------------------

    /**
     * A value for [parameter] derived from its name and [seed].
     *
     * Generated rather than hand-written because the point is to differ from the *other* instance
     * in every single field: a hand-written pair that happened to agree on one field would make
     * "preserved" and "copied" indistinguishable there, and that is precisely the field a
     * regression would hide in.
     */
    private fun valueFor(parameter: KParameter, seed: Int): Any {
        val name = requireNotNull(parameter.name)
        return when (parameter.type.classifier) {
            String::class -> "$name-$seed"
            Int::class -> seed * 1_000 + name.length
            Long::class -> (seed * 1_000 + name.length).toLong()
            Double::class -> seed + name.length / 100.0
            Boolean::class -> seed % 2 == 0
            else -> error("no marker value for ${parameter.type} (${parameter.name})")
        }
    }

    private fun <T : Any> instanceOf(type: KClass<T>, seed: Int): T {
        val constructor = requireNotNull(type.primaryConstructor) { "${type.simpleName} has no primary constructor" }
        return constructor.callBy(constructor.parameters.associateWith { valueFor(it, seed) })
    }

    private fun valueOf(instance: Any, name: String): Any? {
        @Suppress("UNCHECKED_CAST")
        val property = instance::class.memberProperties
            .firstOrNull { it.name == name } as KProperty1<Any, Any?>?
        return requireNotNull(property) { "no property $name on ${instance::class.simpleName}" }
            .get(instance)
    }

    private val storedRow = instanceOf(MosqueConfigEntity::class, STORED)
    private val incoming = instanceOf(MosqueConfig::class, INCOMING)

    private val entityFields = MosqueConfigEntity::class.memberProperties.map { it.name }

    private fun repositoryOver(row: MosqueConfigEntity): Pair<FakeMosqueDao, RoomConfigRepository> {
        val dao = FakeMosqueDao().apply {
            seed(
                config = row,
                devices = listOf(ConnectedDeviceEntity("d1", "hp", false, true, 1, 1, false, 1L))
            )
        }
        return dao to roomRepository(dao) { WRITE_AT }
    }

    // --- vacuous-pass guards -------------------------------------------------------------------

    @Test
    fun theTwoInstancesDifferInEveryFieldTheTestCompares() {
        // Without this the whole class can pass vacuously: if the stored row and the incoming
        // object agreed on a field, "left alone" and "copied over" produce the same value there.
        val same = entityFields
            .filter { name -> MosqueConfig::class.memberProperties.any { it.name == name } }
            .filter { name -> valueOf(storedRow, name) == valueOf(incoming, name) }
        assertTrue("stored row and incoming config agree on: $same", same.isEmpty())
    }

    @Test
    fun lastModifiedIsTheOnlyEntityFieldWithNoDomainCounterpart() {
        // It is storage bookkeeping and deliberately absent from the wire model — see
        // MosqueSnapshot. Anything *else* appearing here would mean a setting exists on disk that
        // no controller can see, which is the class of bug ConfigPayloadCoverageTest was written
        // for; pinning it keeps that discovery from happening in this test's noise instead.
        val domainFields = MosqueConfig::class.memberProperties.map { it.name }.toSet()
        assertEquals(listOf("lastModified"), entityFields.filterNot { it in domainFields })
    }

    @Test
    fun everyEntityFieldIsEitherCopiedOrOwnedElsewhere() {
        // The reflective half. A config field added to the entity and forgotten in
        // `updateConfig` reads back as whatever the row already held — which on a fresh install
        // is its default, so the setting appears to save and then does not.
        val copied = entityFields.filterNot { it in ownedElsewhere }
        assertTrue("no fields left to copy; the classification is broken", copied.size > 40)
        val unmatched = copied.filter { name ->
            MosqueConfig::class.memberProperties.none { it.name == name }
        }
        assertTrue(
            "these entity fields have no same-named property on MosqueConfig, so this test " +
                "cannot tell whether updateConfig copies them: $unmatched",
            unmatched.isEmpty()
        )
    }

    @Test
    fun theOwnedElsewhereListDoesNotNameAFieldThatNoLongerExists() {
        val stale = ownedElsewhere.keys - entityFields.toSet()
        assertTrue("`ownedElsewhere` names fields that no longer exist: $stale", stale.isEmpty())
    }

    // --- the guards ----------------------------------------------------------------------------

    @Test
    fun theWholeObjectWriteCopiesEveryFieldItOwns() {
        val (dao, repository) = repositoryOver(storedRow)
        runBlocking { repository.updateConfig(incoming) }
        val after = requireNotNull(dao.configRow)

        val wrong = entityFields
            .filterNot { it in ownedElsewhere }
            .mapNotNull { name ->
                val expected = valueOf(incoming, name)
                val actual = valueOf(after, name)
                if (expected == actual) null else "$name: expected $expected, stored $actual"
            }
        assertTrue(
            "updateConfig(config) did not persist these:\n" + wrong.joinToString("\n"),
            wrong.isEmpty()
        )
    }

    @Test
    fun theWholeObjectWriteLeavesTheFieldsOwnedElsewhereAlone() {
        val (dao, repository) = repositoryOver(storedRow)
        runBlocking { repository.updateConfig(incoming) }
        val after = requireNotNull(dao.configRow)

        val clobbered = ownedElsewhere.keys
            .filterNot { it == "lastModified" }
            .mapNotNull { name ->
                val kept = valueOf(storedRow, name)
                val actual = valueOf(after, name)
                if (kept == actual) {
                    null
                } else {
                    "$name: was $kept, became $actual — it belongs to " +
                        "${ownedElsewhere.getValue(name).setter}"
                }
            }
        assertTrue(
            "updateConfig(config) overwrote fields it does not own:\n" +
                clobbered.joinToString("\n"),
            clobbered.isEmpty()
        )
        // The one field that must change, so "leaves alone" cannot be read as "wrote nothing".
        assertEquals(WRITE_AT, after.lastModified)
    }

    @Test
    fun theFieldByFieldWriteAlsoLeavesThemAlone() {
        // The other overload, used by the remote sync and the panel's own update path. It takes
        // loose values and none of them are these, so the same clobbering would be possible.
        val (dao, repository) = repositoryOver(storedRow)
        runBlocking {
            repository.updateConfig(
                latitude = -6.2,
                longitude = 106.8,
                iqomahSubuh = 1,
                iqomahDzuhur = 2,
                iqomahAshar = 3,
                iqomahMaghrib = 4,
                iqomahIsya = 5,
                calculationMethodName = "EGYPTIAN",
                adzanLeadMinutes = 6
            )
        }
        val after = requireNotNull(dao.configRow)

        val clobbered = ownedElsewhere.keys
            .filterNot { it == "lastModified" }
            .filter { valueOf(storedRow, it) != valueOf(after, it) }
        assertTrue("the field-by-field write overwrote: $clobbered", clobbered.isEmpty())
        assertEquals(-6.2, after.latitude, 0.0)
        assertEquals("EGYPTIAN", after.calculationMethodName)
    }

    @Test
    fun theFieldByFieldWriteTreatsNullAsLeaveItAlone() {
        // Five nullable parameters, and the reason they are nullable: the panel sends a subset.
        // A null read as "" or as false would blank the adzan footer the moment a controller
        // changed the coordinates.
        val (dao, repository) = repositoryOver(storedRow)
        runBlocking {
            repository.updateConfig(
                latitude = 1.0,
                longitude = 2.0,
                iqomahSubuh = 1,
                iqomahDzuhur = 2,
                iqomahAshar = 3,
                iqomahMaghrib = 4,
                iqomahIsya = 5,
                calculationMethodName = "EGYPTIAN",
                adzanLeadMinutes = 6,
                adzanFooterText = null,
                iqomahFooterText = null,
                dashboardRandomize = null,
                dashboardTransitionType = null,
                dashboardTransitionInterval = null
            )
        }
        val after = requireNotNull(dao.configRow)

        assertEquals(storedRow.adzanFooterText, after.adzanFooterText)
        assertEquals(storedRow.iqomahFooterText, after.iqomahFooterText)
        assertEquals(storedRow.dashboardRandomize, after.dashboardRandomize)
        assertEquals(storedRow.dashboardTransitionType, after.dashboardTransitionType)
        assertEquals(storedRow.dashboardTransitionInterval, after.dashboardTransitionInterval)
    }

    @Test
    fun everyFieldOwnedElsewhereHasASetterThatDoesChangeIt() {
        // The other half of the claim. "Not copied" is only correct if something else writes it —
        // otherwise the field would be unreachable, which is a different bug with the same
        // assertions.
        val unmoved = ownedElsewhere.entries.mapNotNull { (name, owner) ->
            val invoke = owner.invoke ?: return@mapNotNull null
            val (dao, repository) = repositoryOver(storedRow)
            runBlocking { invoke(repository) }
            val after = requireNotNull(dao.configRow)
            if (valueOf(storedRow, name) == valueOf(after, name)) {
                "$name was not changed by ${owner.setter}"
            } else {
                null
            }
        }
        assertTrue(unmoved.joinToString("\n"), unmoved.isEmpty())
        assertNotEquals(
            "no owner in the list is exercised, so this test asserts nothing",
            0,
            ownedElsewhere.count { it.value.invoke != null }
        )
    }
}
