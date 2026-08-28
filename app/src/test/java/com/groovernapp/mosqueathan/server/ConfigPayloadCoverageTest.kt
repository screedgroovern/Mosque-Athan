package com.groovernapp.mosqueathan.server

import com.groovernapp.mosqueathan.domain.MosqueConfig
import java.io.File
import kotlin.reflect.full.memberProperties
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every config field must reach a controller, or be deliberately withheld.
 *
 * `jumatTransitionType` and `jumatVideoWaitUntilEnd` were neither: the display simply never put
 * them in `getCurrentStateJson()`, so the web panel and the hand-written panel both showed their
 * defaults while the display held something else. Nothing failed — a missing key is indistinguishable
 * from a key whose value happens to equal the default, which is exactly why this needs a test rather
 * than a careful reader.
 *
 * This reads source, like [ActionNameDriftTest], for the same reason: the failure is textual and
 * there is no runtime moment at which an absent key can be observed.
 */
class ConfigPayloadCoverageTest {

    /**
     * Fields the display must **not** send, each for a stated reason. Adding to this list is a
     * decision; leaving a field out of the payload by accident is the bug.
     */
    private val withheld = mapOf(
        "id" to "the Realm primary key, meaningless to another device",
        "pinCode" to "never leaves the display; it sends hasPin instead",
        "connectivityMode" to "describes this device's own role, not the other's",
        "remoteTvIp" to "describes which display this device controls, not the other's"
    )

    private val serverSource =
        File("src/main/java/com/groovernapp/mosqueathan/server/SettingsServer.kt")

    @Test
    fun theTestCanSeeTheServerSource() {
        // Guards against the whole test passing vacuously if the layout ever moves.
        assertTrue("cannot read ${serverSource.absolutePath}", serverSource.isFile)
    }

    @Test
    fun everyConfigFieldIsEitherServedOrDeliberatelyWithheld() {
        val source = serverSource.readText()
        val served = Regex("""put\("([a-zA-Z]+)"""").findAll(source).map { it.groupValues[1] }.toSet()

        val fields = MosqueConfig::class.memberProperties.map { it.name }
        val missing = fields.filter { it !in served && it !in withheld }

        assertTrue(
            "config fields the display never sends, so no controller can read them: $missing\n" +
                "Add them to getCurrentStateJson(), or add them to `withheld` with a reason.",
            missing.isEmpty()
        )
    }

    @Test
    fun theWithheldListDoesNotNameFieldsThatNoLongerExist() {
        val fields = MosqueConfig::class.memberProperties.map { it.name }.toSet()
        val stale = withheld.keys.filterNot { it in fields }
        assertTrue("withheld names no longer on MosqueConfig: $stale", stale.isEmpty())
    }

    @Test
    fun thePinIsStillNotServed() {
        // Stated separately from the withheld list so removing an entry there cannot quietly
        // start leaking the PIN.
        val source = serverSource.readText()
        assertTrue(
            "the display must never put the PIN in the state payload",
            !source.contains("""put("pinCode"""")
        )
    }
}
