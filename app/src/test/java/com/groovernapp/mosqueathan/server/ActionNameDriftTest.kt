package com.groovernapp.mosqueathan.server

import com.groovernapp.mosqueathan.protocol.RemoteActions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Neither side of the WebSocket protocol may spell an action name by hand.
 *
 * The panel's JavaScript and the server's `when` are two separate files in two
 * languages describing one protocol, and nothing used to connect them. That let the
 * rename button send `register_device` while the server only handled `register` — the
 * message was dropped, and the panel reported success anyway.
 *
 * Both sides now reference [RemoteActions]. This test is what keeps them there, because
 * writing a literal is easier than finding the constant and compiles perfectly well.
 *
 * It reads source rather than behaviour, which is unusual and worth justifying: the
 * failure being guarded against is *textual*, and there is no runtime moment at which a
 * name that nothing handles can be observed — that is exactly what made the original bug
 * invisible. Comments are stripped before matching, so prose mentioning an action name
 * cannot make this pass or fail.
 */
class ActionNameDriftTest {

    private val serverDir = File("src/main/java/com/groovernapp/mosqueathan/server")

    /**
     * Assembled at runtime from a schedule type — `'update_' + type + '_schedule_days'`
     * — so these cannot reference a constant from the JavaScript side.
     */
    private val dynamicPrefixes = setOf("delete_", "update_")

    @Test
    fun theServerSourceIsWhereThisTestThinksItIs() {
        // Guards against the whole test passing vacuously from a wrong working directory.
        assertTrue(
            "expected the server sources at ${serverDir.absolutePath}",
            File(serverDir, "SettingsServer.kt").isFile
        )
        assertTrue(serverDir.listFiles { f -> f.name.startsWith("WebPageJs") }!!.isNotEmpty())
    }

    @Test
    fun theServerDispatchesOnConstantsOnly() {
        val source = stripComments(File(serverDir, "SettingsServer.kt").readText())
        val literals = Regex("""^\s*"([a-z_]+)" -> """, RegexOption.MULTILINE)
            .findAll(source).map { it.groupValues[1] }.toList()
        assertEquals(
            "these `when` branches still match a string literal; use RemoteActions instead",
            emptyList<String>(),
            literals
        )
    }

    @Test
    fun thePanelSendsConstantsOnly() {
        val offenders = mutableListOf<String>()
        serverDir.listFiles { f -> f.name.startsWith("WebPageJs") }!!.forEach { file ->
            val source = stripComments(file.readText())
            Regex("""action: *'([a-z_]+)'""").findAll(source).forEach { match ->
                val name = match.groupValues[1]
                if (dynamicPrefixes.none { name == it }) {
                    offenders += "${file.name}: '$name'"
                }
            }
        }
        assertEquals(
            "the panel spells these action names by hand; reference RemoteActions so the " +
                "server and the panel cannot disagree",
            emptyList<String>(),
            offenders
        )
    }

    @Test
    fun everyActionTheServerHandlesHasAConstant() {
        val source = stripComments(File(serverDir, "SettingsServer.kt").readText())
        val referenced = Regex("""RemoteActions\.([A-Z_]+)""").findAll(source)
            .map { it.groupValues[1] }.toSet()
        val declared = RemoteActions::class.java.declaredFields
            .map { it.name }.filter { it.all { c -> c.isUpperCase() || c == '_' } }.toSet()
        val unknown = referenced - declared - setOf("UNAUTHENTICATED")
        assertEquals("the server references constants that do not exist", emptySet<String>(), unknown)
        assertTrue("the server should dispatch on many actions, saw ${referenced.size}", referenced.size > 40)
    }

    @Test
    fun theUnauthenticatedSetIsTheOneUsedByTheGate() {
        // If someone adds a fourth pre-auth action they must add it to the set, not to a
        // second condition beside it.
        assertEquals(
            setOf(RemoteActions.REGISTER, RemoteActions.VERIFY_PIN, RemoteActions.PING),
            RemoteActions.UNAUTHENTICATED
        )
        val source = stripComments(File(serverDir, "SettingsServer.kt").readText())
        assertTrue(
            "the access gate should test RemoteActions.UNAUTHENTICATED",
            source.contains("action !in RemoteActions.UNAUTHENTICATED")
        )
    }

    /** Removes `//` and block comments so prose about an action name cannot match. */
    private fun stripComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .lines().joinToString("\n") { it.substringBefore("//") }
}
