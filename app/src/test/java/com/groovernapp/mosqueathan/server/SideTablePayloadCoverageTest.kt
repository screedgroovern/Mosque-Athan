package com.groovernapp.mosqueathan.server

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.ConnectedDevice
import com.groovernapp.mosqueathan.domain.DashboardBackground
import com.groovernapp.mosqueathan.domain.FridayMedia
import com.groovernapp.mosqueathan.domain.Media
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The same guard as [ConfigPayloadCoverageTest], for the seven side tables — and it exists because
 * that test could not have caught these.
 *
 * `ConfigPayloadCoverageTest` scans the whole file for `put("name")`, which is enough for one flat
 * config object and useless here: `isUsed` is served for dashboard backgrounds, so `Media.isUsed`
 * looked served while `mediaArray` never carried it. Two fields were missing on that reasoning —
 * `Media.isUsed`, which the settings UI shows as the "Use" checkbox on every slide, and
 * `Announcement.position`, which a controller then reordered using positions that all read 0.
 *
 * So this reads each array's own `apply { }` block rather than the file. Source again, for the
 * reason given in [ConfigPayloadCoverageTest]: an absent key is indistinguishable at runtime from
 * one whose value happens to equal the default.
 */
class SideTablePayloadCoverageTest {

    private val serverSource =
        File("src/main/java/com/groovernapp/mosqueathan/server/SettingsServer.kt")

    /**
     * The builder variable in `getCurrentStateJson()`, the class it serialises, and any property
     * deliberately left out. Adding to a withheld list is a decision; omitting a key is the bug.
     */
    private val tables = listOf(
        Table("announcementsArray", Announcement::class),
        Table("mediaArray", Media::class),
        Table("fridayMediaArray", FridayMedia::class),
        Table("dashboardBgArray", DashboardBackground::class),
        Table("slideshowArray", SlideshowSchedule::class),
        Table("standbyArray", StandbySchedule::class),
        Table(
            "devicesArray", ConnectedDevice::class,
            withheld = mapOf(
                "authenticatedAt" to
                    "the display's own session bookkeeping — it is what expires a paired device, " +
                    "and no controller reads it"
            )
        )
    )

    private data class Table(
        val builder: String,
        val type: KClass<*>,
        val withheld: Map<String, String> = emptyMap()
    )

    @Test
    fun theTestCanSeeTheServerSource() {
        assertTrue("cannot read ${serverSource.absolutePath}", serverSource.isFile)
    }

    @Test
    fun everyTableIsActuallyFoundInTheSource() {
        // Without this the whole test passes vacuously the moment a variable is renamed.
        val source = serverSource.readText()
        val absent = tables.filterNot { source.contains("val ${it.builder} = JSONArray()") }
        assertTrue("these builders are no longer in getCurrentStateJson(): $absent", absent.isEmpty())
    }

    @Test
    fun noWithheldListNamesAFieldThatNoLongerExists() {
        // A stale entry silently excuses a field that has since been renamed, which is the same
        // invisible gap this test exists to catch.
        val stale = tables.flatMap { table ->
            val fields = table.type.memberProperties.map { it.name }.toSet()
            table.withheld.keys.filterNot { it in fields }
                .map { "${table.type.simpleName}.$it" }
        }
        assertTrue("withheld names properties that no longer exist: $stale", stale.isEmpty())
    }

    @Test
    fun everySideTableFieldIsEitherServedOrDeliberatelyWithheld() {
        val source = serverSource.readText()
        val complaints = tables.mapNotNull { table ->
            val served = keysServedBy(source, table.builder)
            val missing = table.type.memberProperties
                .map { it.name }
                .filter { it !in served && it !in table.withheld }
            if (missing.isEmpty()) null
            else "${table.type.simpleName} -> ${table.builder} never sends $missing"
        }

        assertTrue(
            "side-table fields no controller can read:\n" + complaints.joinToString("\n") +
                "\nAdd them to getCurrentStateJson(), or withhold them with a reason.",
            complaints.isEmpty()
        )
    }

    /**
     * The keys inside `<builder>.put(JSONObject().apply { … })`.
     *
     * Brace-counted rather than matched with a regex, because the block contains its own braces —
     * a `?:`, an `if`, a string template — and a lazy `.*?` would stop at the first of them and
     * report keys as missing that are served two lines later.
     */
    private fun keysServedBy(source: String, builder: String): Set<String> {
        val marker = "$builder.put(JSONObject().apply {"
        val start = source.indexOf(marker)
        if (start < 0) return emptySet()
        var depth = 0
        var i = start + marker.length - 1
        val end: Int = run {
            while (i < source.length) {
                when (source[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return@run i
                    }
                }
                i++
            }
            source.length
        }
        return Regex("""put\("([a-zA-Z]+)"""")
            .findAll(source.substring(start, end))
            .map { it.groupValues[1] }
            .toSet()
    }
}
