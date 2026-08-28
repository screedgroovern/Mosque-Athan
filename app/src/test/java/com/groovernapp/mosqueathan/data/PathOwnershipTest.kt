package com.groovernapp.mosqueathan.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The gate on `/upload?replacePath=`.
 *
 * This is the one piece of the server where being wrong is a security problem: the parameter names
 * a file to overwrite and it comes from another device on the mosque wifi. The traversal cases below
 * are the ones that matter — they are strings no owned path equals, which name files the display
 * must never write.
 */
class PathOwnershipTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var media: File
    private lateinit var owned: List<String>

    private fun setUpFiles() {
        val dir = folder.newFolder("presentation")
        media = File(dir, "slide.jpg").apply { writeText("image bytes") }
        owned = listOf(media.absolutePath)
    }

    @Test
    fun theExactOwnedPathIsAccepted() {
        setUpFiles()
        assertTrue(isPathOwned(media.absolutePath, owned))
    }

    @Test
    fun anotherSpellingOfTheSameFileIsAccepted() {
        setUpFiles()
        // Same file, different string. Refusing this would break nothing today, but it would mean
        // the check is comparing strings rather than files — which is the bug the next case exploits.
        val viaDot = media.parentFile.absolutePath + "/./" + media.name
        assertTrue("'$viaDot' names the owned file", isPathOwned(viaDot, owned))
    }

    @Test
    fun aPathWithNoRowIsRefused() {
        setUpFiles()
        val stranger = File(media.parentFile, "not_in_the_database.jpg").absolutePath
        assertFalse(isPathOwned(stranger, owned))
    }

    @Test
    fun traversalOutOfTheMediaDirectoryIsRefused() {
        setUpFiles()
        // The attack: build a string from a genuinely owned path so it looks plausible, then climb
        // out of the directory. As raw strings this equals nothing in `owned`; canonicalised it
        // names a file elsewhere, which is exactly why the comparison must canonicalise.
        val escapes = listOf(
            media.absolutePath + "/../../databases/mosque.realm",
            media.parentFile.absolutePath + "/../shared_prefs/prefs.xml",
            media.parentFile.absolutePath + "/../../../../etc/hosts"
        )
        escapes.forEach { assertFalse("must refuse $it", isPathOwned(it, owned)) }
    }

    @Test
    fun anEmptyOrBlankPathIsRefused() {
        setUpFiles()
        assertFalse(isPathOwned("", owned))
        assertFalse(isPathOwned("   ", owned))
    }

    @Test
    fun nothingIsOwnedWhenTheDisplayHoldsNoMedia() {
        setUpFiles()
        assertFalse(isPathOwned(media.absolutePath, emptyList()))
    }
}
