package com.groovernapp.mosqueathan.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The only guard between a request from the mosque wifi and the rest of the APK's assets.
 *
 * Every "must refuse" case below is a way of asking for a file outside the panel directory.
 */
class PanelAssetsTest {

    @Test
    fun theBareRouteServesTheEntryPoint() {
        assertEquals("panel/index.html", panelAssetPath("/panel"))
        assertEquals("panel/index.html", panelAssetPath("/panel/"))
    }

    @Test
    fun ordinaryBundleFilesResolve() {
        assertEquals("panel/mosqueathan.js", panelAssetPath("/panel/mosqueathan.js"))
        assertEquals("panel/abc123.wasm", panelAssetPath("/panel/abc123.wasm"))
        assertEquals(
            "panel/composeResources/font.ttf",
            panelAssetPath("/panel/composeResources/font.ttf")
        )
    }

    @Test
    fun theQueryStringIsNotPartOfTheFilename() {
        // The panel is loaded with a cache-busting parameter during development.
        assertEquals("panel/index.html", panelAssetPath("/panel/index.html?v=12345"))
        assertEquals("panel/index.html", panelAssetPath("/panel?tv=192.168.1.3"))
    }

    @Test
    fun traversalIsRefused() {
        assertNull(panelAssetPath("/panel/../secrets.txt"))
        assertNull(panelAssetPath("/panel/nested/../../secrets.txt"))
        assertNull(panelAssetPath("/panel/.."))
    }

    @Test
    fun encodedTraversalIsRefused() {
        // The reason decoding happens before the check: this is a traversal that only looks like
        // one afterwards, and a check on the raw string would wave it through.
        assertNull(panelAssetPath("/panel/%2e%2e%2fsecrets.txt"))
        assertNull(panelAssetPath("/panel/%2E%2E/secrets.txt"))
    }

    @Test
    fun anAbsoluteOrBackslashPathIsRefused() {
        assertNull(panelAssetPath("/panel//etc/hosts"))
        assertNull(panelAssetPath("/panel/%2fetc%2fhosts"))
        assertNull(panelAssetPath("/panel/nested\\file.js"))
    }
}
