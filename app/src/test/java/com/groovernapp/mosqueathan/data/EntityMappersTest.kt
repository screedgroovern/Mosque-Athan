package com.groovernapp.mosqueathan.data

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.KemenagSchedule
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mappers carry every field.
 *
 * This is not a formality. Every property of every domain class has a default value —
 * they have to, so the repository can hand back a sensible config when Realm is empty.
 * The consequence is that a mapper which *forgets* a field still compiles, and the
 * setting then reads back as its default no matter what is on disk. That is the same
 * silent-loss failure the hand-written `copy(...)` extension used to cause; moving to a
 * data class removed the boilerplate but not this particular hazard.
 *
 * So each case sets every entity field to a value that is *not* the default, maps it,
 * and compares property-by-property by name. A field missing from a mapper fails here
 * rather than in the mosque.
 */
class EntityMappersTest {

    @Test
    fun mosqueConfigCarriesAllFiftyFiveFields() =
        assertTotal(MosqueConfigEntity(), MosqueConfig::class.java) { it.toDomain() }

    @Test
    fun announcementCarriesEveryField() =
        assertTotal(AnnouncementEntity(), Announcement::class.java) { it.toDomain() }

    @Test
    fun mediaCarriesEveryField() =
        assertTotal(MediaEntity(), Media::class.java) { it.toDomain() }

    @Test
    fun fridayMediaCarriesEveryField() =
        assertTotal(FridayMediaEntity(), FridayMedia::class.java) { it.toDomain() }

    @Test
    fun connectedDeviceCarriesEveryField() =
        assertTotal(ConnectedDeviceEntity(), ConnectedDevice::class.java) { it.toDomain() }

    @Test
    fun dashboardBackgroundCarriesEveryField() =
        assertTotal(DashboardBackgroundEntity(), DashboardBackground::class.java) { it.toDomain() }

    @Test
    fun slideshowScheduleCarriesEveryField() =
        assertTotal(SlideshowScheduleEntity(), SlideshowSchedule::class.java) { it.toDomain() }

    @Test
    fun standbyScheduleCarriesEveryField() =
        assertTotal(StandbyScheduleEntity(), StandbySchedule::class.java) { it.toDomain() }

    @Test
    fun kemenagScheduleCarriesEveryField() =
        assertTotal(KemenagScheduleEntity(), KemenagSchedule::class.java) { it.toDomain() }

    /** The reverse direction, for the one write path that maps a whole object. */
    @Test
    fun kemenagScheduleMapsBackToAnEntityIntact() {
        val domain = KemenagSchedule(
            dateString = "2026-08-23", cityId = "city-1", imsak = "04:20", subuh = "04:30",
            terbit = "05:45", dhuha = "06:10", dzuhur = "11:50", ashar = "15:10",
            maghrib = "17:45", isya = "18:55"
        )
        val roundTripped = domain.toEntity().toDomain()
        assertEquals(domain, roundTripped)
    }

    /**
     * Sets every mutable property of [entity] to a value distinguishable from its
     * default, maps it, and asserts the domain object agrees on every shared name.
     */
    private fun <E : Any, D : Any> assertTotal(entity: E, domainClass: Class<D>, map: (E) -> D) {
        val entityProps = entity::class.memberProperties
            .filterIsInstance<KMutableProperty1<E, Any?>>()
        assertTrue(
            "no mutable properties found on ${entity::class.simpleName} — the reflection " +
                "here is silently testing nothing",
            entityProps.isNotEmpty()
        )

        entityProps.forEach { p -> p.set(entity, distinctValue(p.returnType.toString(), p.get(entity))) }

        val domain = map(entity)
        val domainProps = domainClass.kotlin.memberProperties
            .associateBy { it.name }

        entityProps.forEach { p ->
            val d = domainProps[p.name]
            assertTrue(
                "${domainClass.simpleName} has no property '${p.name}', so the entity " +
                    "field is dropped on the way to the domain model",
                d != null
            )
            @Suppress("UNCHECKED_CAST")
            val actual = (d as KProperty1<D, Any?>).get(domain)
            assertEquals(
                "'${p.name}' did not survive the mapping — it is missing from " +
                    "${domainClass.simpleName}'s mapper, so it will always read as its default",
                p.get(entity),
                actual
            )
        }
    }

    /**
     * A value the field does not already hold, so a dropped field cannot pass by
     * coincidence.
     *
     * [current] is the field's default, and it matters for `Boolean`: a fixed sentinel
     * of `true` would equal the default of every field that already defaults to `true`
     * — `includeDefaultImage` and `jumatVideoWaitUntilEnd` among them — so dropping one
     * from a mapper would still compare equal and the test would pass while the setting
     * silently stopped working. Negating the default removes that blind spot.
     */
    private fun distinctValue(type: String, current: Any?): Any = when {
        type.startsWith("kotlin.String") -> "sentinel-value"
        type.startsWith("kotlin.Int") -> 12345
        type.startsWith("kotlin.Long") -> 9876543210L
        type.startsWith("kotlin.Double") -> 42.4242
        type.startsWith("kotlin.Boolean") -> !(current as Boolean)
        else -> error("no sentinel defined for $type — add one rather than skipping the field")
    }
}
