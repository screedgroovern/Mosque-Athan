@file:JsModule("@js-joda/timezone")

package com.groovernapp.mosqueathan.web

/**
 * Pulls the js-joda timezone database into the bundle.
 *
 * kotlinx-datetime is implemented on js-joda in the browser and depends on this package
 * being *loaded*, not merely declared — the database registers itself as a side effect of
 * the import. Referencing [jsJodaTz] from `main` is what forces webpack to keep it.
 *
 * Without it `TimeZone.currentSystemDefault()` throws, which is not an obvious failure:
 * it surfaces as the settings screen refusing to compose, because `PrayerType.displayName`
 * asks for today's date to decide whether to say "Jum'at".
 */
@JsName("default")
external val jsJodaTz: JsAny
