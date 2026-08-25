package com.groovernapp.mosqueathan.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The readers tolerate exactly what `org.json`'s `optX` tolerated.
 *
 * This is a behaviour-preservation test, not a specification. Replacing `org.json` with
 * strict deserialization would have been a change of behaviour disguised as a refactor:
 * the display's `/api/get_config` omits keys, and a controller syncing from a display on
 * older code will meet fields it does not know. Throwing there turns a cosmetic mismatch
 * into a failed sync on devices that are updated by hand, one at a time.
 *
 * So every case below is one `optX` would have absorbed silently.
 */
class JsonLeniencyTest {

    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    @Test
    fun presentValuesAreRead() {
        val o = obj("""{"s":"text","i":42,"d":-7.25,"b":true}""")
        assertEquals("text", o.optionalString("s"))
        assertEquals(42, o.optionalInt("i"))
        assertEquals(-7.25, o.optionalDouble("d"))
        assertEquals(true, o.optionalBoolean("b"))
    }

    @Test
    fun aMissingKeyGivesTheDefault() {
        val o = obj("""{"other":1}""")
        assertEquals("", o.optionalString("nope"))
        assertEquals(0, o.optionalInt("nope"))
        assertEquals(0.0, o.optionalDouble("nope"))
        assertEquals(false, o.optionalBoolean("nope"))
    }

    @Test
    fun anExplicitNullGivesTheDefault() {
        val o = obj("""{"s":null,"i":null,"d":null,"b":null}""")
        assertEquals("", o.optionalString("s"))
        assertEquals(0, o.optionalInt("i"))
        assertEquals(0.0, o.optionalDouble("d"))
        assertEquals(false, o.optionalBoolean("b"))
    }

    @Test
    fun theWrongTypeGivesTheDefaultRatherThanThrowing() {
        // The case that matters most: a display on older code sending a shape this
        // version does not expect must not fail the whole sync.
        val o = obj("""{"i":"not a number","b":"maybe","d":"abc"}""")
        assertEquals(0, o.optionalInt("i"))
        assertEquals(false, o.optionalBoolean("b"))
        assertEquals(0.0, o.optionalDouble("d"))
    }

    @Test
    fun anObjectOrArrayWhereAScalarWasExpectedGivesTheDefault() {
        val o = obj("""{"a":{"nested":1},"b":[1,2,3]}""")
        assertEquals("", o.optionalString("a"))
        assertEquals(0, o.optionalInt("a"))
        assertEquals("", o.optionalString("b"))
        assertEquals(0, o.optionalInt("b"))
    }

    @Test
    fun numbersReadAsStringsAndBackWhereThatIsWhatOptXDid() {
        // org.json's optString on a number returned its text form; keep that.
        val o = obj("""{"n":42}""")
        assertEquals("42", o.optionalString("n"))
    }

    @Test
    fun anIntegerReadAsADoubleWorks() {
        assertEquals(5.0, obj("""{"n":5}""").optionalDouble("n"))
    }

    @Test
    fun aFractionalValueReadAsAnIntGivesTheDefault() {
        // intOrNull refuses "7.9" rather than truncating. org.json's optInt truncated to
        // 7. This is the one place the two differ, and it is asserted so the difference
        // is recorded rather than discovered: no config field is fractional except
        // latitude and longitude, which are read as doubles.
        assertEquals(0, obj("""{"n":7.9}""").optionalInt("n"))
    }
}
