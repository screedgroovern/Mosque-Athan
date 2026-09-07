package com.groovernapp.mosqueathan.server

/**
 * What `/` and the PWA endpoints serve now that the hand-written panel is gone.
 *
 * The pairing QR encodes the display's bare origin, so scanning it opens `/` — which means `/`
 * could not simply stop working, or the first thing a takmir does after the upgrade is scan a code
 * that leads nowhere. That was step 8's missing prerequisite.
 */
object RetiredPanel {

    /**
     * A 200 that moves the browser to `/panel/`, **not** a 302 — and the difference is not taste.
     *
     * The retired panel registered a service worker that is *still installed* on every phone that
     * opened it, and its `fetch` handler answers navigations to `/` with
     * `event.respondWith(fetch(event.request))`. `fetch` follows redirects, so that hands
     * `respondWith` a response whose `redirected` flag is set — which a navigation request rejects
     * outright, with a TypeError instead of a page. A 302 here would therefore *break* the panel
     * on exactly the devices that used it most, and only until the browser happened to re-check
     * `sw.js`. A 200 carrying a redirect is invisible to that handler.
     *
     * Both a `<meta refresh>` and a script, because the meta tag alone leaves the back button
     * pointing here — `location.replace` does not add a history entry, so Back from the panel goes
     * where the takmir came from. The visible line is for the moment before either fires, and for
     * the case where neither does.
     */
    fun getRedirectHtml(): String = """<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="refresh" content="0; url=/panel/">
<title>MosqueAthan</title>
<style>
  body { margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
         background: #060907; color: #ffd54f; font-family: system-ui, sans-serif; text-align: center; }
  a { color: #ffd54f; }
</style>
</head>
<body>
<p>Membuka panel kontrol&hellip;<br><a href="/panel/">Buka panel kontrol / Open the control panel</a></p>
<script>location.replace('/panel/');</script>
</body>
</html>
"""

    /**
     * The manifest, pointing at the panel that exists.
     *
     * A manifest is read at install time, so this does not move an already-installed shortcut —
     * that is what the redirect above is for. It matters for anyone installing from here on.
     */
    fun getManifest(): String = """{
  "name": "MosqueAthan Settings",
  "short_name": "MosqueAthan",
  "start_url": "/panel/",
  "display": "standalone",
  "background_color": "#060907",
  "theme_color": "#060907",
  "icons": [
    {
      "src": "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23ffd54f'><path d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z'/></svg>",
      "sizes": "192x192",
      "type": "image/svg+xml",
      "purpose": "any maskable"
    }
  ]
}"""

    /**
     * A service worker whose only job is to remove itself.
     *
     * **Serving nothing here would not have retired it.** A registered worker keeps intercepting
     * every navigation to `/` for as long as it lives, and its cache-first branch keeps answering
     * from `mosqueathan-cache-v4` — so a takmir's phone would go on showing a panel this build no
     * longer contains, indefinitely, with no way to tell from the server that it was happening.
     * A 404 on `sw.js` does not unregister anything either; browsers keep the last good copy.
     *
     * So the kill switch has to be a *valid* worker that claims control and then gives it up:
     * `skipWaiting` and `claim` make this copy replace the old one immediately rather than waiting
     * for every tab to close, the caches go, and `unregister` retires the registration. Browsers
     * re-check this script on navigation, which is what makes the switch reachable at all.
     */
    fun getServiceWorkerKillSwitch(): String = """
// The hand-written panel this worker cached no longer exists. Remove ourselves.
self.addEventListener('install', () => self.skipWaiting());

self.addEventListener('activate', event => {
    event.waitUntil((async () => {
        await self.clients.claim();
        const names = await caches.keys();
        await Promise.all(names.map(name => caches.delete(name)));
        await self.registration.unregister();
        // Reload every open tab so it comes from the network rather than from a cache that is
        // now gone; without this the page stays on whatever the old worker last served it.
        const clients = await self.clients.matchAll({ type: 'window' });
        for (const client of clients) {
            client.navigate(client.url);
        }
    })());
});
"""
}
