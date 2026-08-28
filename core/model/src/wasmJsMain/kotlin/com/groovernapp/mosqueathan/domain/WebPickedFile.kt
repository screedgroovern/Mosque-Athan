package com.groovernapp.mosqueathan.domain

import kotlin.js.JsAny

/**
 * A file the user chose in a browser.
 *
 * Lives here, in `core:model`, for the same reason [AndroidPickedFile] does: `:ui:design` creates
 * it from the file input and `:webApp`'s media store unwraps it to upload, and those two modules
 * must not depend on each other.
 *
 * [handle] is the JS `File`. It is deliberately untyped as `JsAny` — nothing in common code can
 * inspect it, which is the point: only the platform that made it knows how to send it.
 *
 * [name] and [isVideo] are lifted out because the *upload* needs them as query parameters, and
 * reading them back through `JsAny` at that point would mean another `js(...)` call in a module
 * that has no business knowing about `File`.
 */
class WebPickedFile(
    val handle: JsAny,
    val name: String,
    val isVideo: Boolean
) : PickedFile
