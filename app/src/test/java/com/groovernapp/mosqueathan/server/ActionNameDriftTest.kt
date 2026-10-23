package com.groovernapp.mosqueathan.server

import com.groovernapp.mosqueathan.protocol.RemoteActions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * No side of the WebSocket protocol may spell an action name by hand.
 *
 * The hand-written panel's JavaScript and the server's `when` used to be two separate
 * files in two languages describing one protocol, and nothing connected them. That let the
 * rename button send `register_device` while the server only handled `register` — the
 * message was dropped, and the panel reported success anyway. The JavaScript is gone; the
 * panel is now the shared Compose UI, which sends from Kotlin like every other controller.
 *
 * Every side now references [RemoteActions]. This test is what keeps them there, because
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

    @Test
    fun theServerSourceIsWhereThisTestThinksItIs() {
        // Guards against the whole test passing vacuously from a wrong working directory.
        assertTrue(
            "expected the server sources at ${serverDir.absolutePath}",
            File(serverDir, "SettingsServer.kt").isFile
        )
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

    @Test
    fun everyControllerSendsConstantsOnly() {
        // The other sides of the protocol. A device in REMOTE mode POSTs these to the
        // display, and two of them named actions the server did not have: the running-text
        // reorder and — worse — the access PIN. Both failed in silence. The browser panel
        // sends from `:webApp` and `:ui:settings`, which replaced the JavaScript that used to
        // be checked here, so those are walked too.
        val roots = listOf(
            File("src/main/java/com/groovernapp/mosqueathan"),
            File("../ui"),
            File("../webApp"),
            File("../core")
        )
        roots.forEach { assertTrue("expected sources at ${it.absolutePath}", it.isDirectory) }
        val offenders = mutableListOf<String>()
        var scanned = 0
        roots.forEach { root ->
            root.walkTopDown()
                .onEnter { it.name != "build" && it.name != "node_modules" }
                .filter { it.extension == "kt" }
                .forEach { file ->
                    scanned++
                    val source = stripComments(file.readText())
                    Regex("""\.send\("([a-z_]+)"""").findAll(source).forEach {
                        offenders += "${file.name}: '${it.groupValues[1]}'"
                    }
                }
        }
        assertTrue("expected to scan many sources, saw $scanned", scanned > 100)
        assertEquals(
            "these outgoing actions are spelled by hand; reference RemoteActions so a name " +
                "the server does not handle cannot compile",
            emptyList<String>(),
            offenders
        )
    }

    /** Removes `//` and block comments so prose about an action name cannot match. */
    private fun stripComments(source: String): String =
        source
            .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .lines().joinToString("\n") { it.substringBefore("//") }
}
