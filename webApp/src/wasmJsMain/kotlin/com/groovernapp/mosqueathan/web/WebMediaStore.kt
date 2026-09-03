package com.groovernapp.mosqueathan.web

import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.domain.StoredMedia
import com.groovernapp.mosqueathan.domain.WebPickedFile
import kotlin.js.JsAny

/**
 * Sends a picked file to the display's `POST /upload`.
 *
 * **Every `store*` returns null, and that is correct rather than a stub.** The upload endpoint does
 * the whole job on the display: it writes the file, downscales it, adds the database row itself, and
 * broadcasts the new state. So there is no local row for the caller to add — and the settings
 * ViewModels are written as `store(file)?.let { repository.add(it.path, …) }`, which means null is
 * exactly the "nothing more to do here" signal. The new item appears in the panel a moment later,
 * through the snapshot the display pushes.
 *
 * The cost of fitting the existing interface: **null also means failure**, so a rejected or
 * interrupted upload is indistinguishable from a successful one, and the user sees only that the
 * list did not change. Fixing that means making [MediaStore] suspend — an interface change that
 * touches Android too — and it should be done together with upload progress, since a takmir sending
 * a several-hundred-megabyte video over mosque wifi needs both.
 */
class WebMediaStore(private val host: String) : MediaStore {

    override fun storePresentationMedia(file: PickedFile): StoredMedia? = upload(file, "")

    override fun storeDashboardBackground(file: PickedFile): StoredMedia? =
        upload(file, "&isDashboardBg=true")

    override fun storeFridayMedia(file: PickedFile): StoredMedia? = upload(file, "&isFriday=true")

    override fun storeCustomBackground(file: PickedFile): StoredMedia? =
        upload(file, "&isBackground=true")

    /**
     * Uploads a recitation and lets the display record where it put it.
     *
     * Returns null for a reason specific to audio: the *path* is the setting, and only the display
     * knows the path until it has written the file. So the display appends it to
     * `murottalAudioPaths` itself and broadcasts — the panel learns the path from the snapshot,
     * the same way it learns about an uploaded slide.
     *
     * A recitation is tens of megabytes where a slide is hundreds of kilobytes, so the missing
     * upload progress matters much more here. That gap is named on the class.
     */
    override fun storeMurottalAudio(file: PickedFile): StoredMedia? = upload(file, "&isAudio=true")

    /**
     * Replaces the bytes behind [path], keeping the path the database holds.
     *
     * The display writes the upload beside the original and renames it into place only once the
     * whole body has arrived, so a replace that drops halfway leaves the existing file intact. It
     * also refuses any path it does not already have a row for.
     *
     * Returns false for the same reason the `store*` methods return null: there is nothing further
     * for the caller to do. The path does not change, so no row needs writing, and the display
     * broadcasts once the replacement lands — which is what tells every client to re-read the file.
     * A `false` that means "dispatched, nothing local to do" is the cost of a synchronous interface
     * over an asynchronous upload; see the class comment.
     */
    override fun replaceFile(path: String, file: PickedFile): Boolean {
        val web = file as? WebPickedFile ?: return false
        postFile(
            url = "http://$host:$PORT/upload?filename=${encodeQueryValue(web.name)}" +
                "&isVideo=${web.isVideo}&replacePath=${encodeQueryValue(path)}",
            handle = web.handle
        )
        return false
    }

    /**
     * A controller cannot reach the display's filesystem. Deleting the *row* still works — that
     * goes through `RemoteActionSink` — so the display stops showing the item and the file is left
     * behind. The Android app in REMOTE mode behaves identically, so this is a pre-existing gap in
     * the controller story rather than something the browser introduces.
     */
    override fun delete(path: String): Boolean = false

    override fun deleteCustomBackground(): Boolean = false

    private fun upload(file: PickedFile, extraQuery: String): StoredMedia? {
        val web = file as? WebPickedFile ?: return null
        postFile(
            url = "http://$host:$PORT/upload?filename=${encodeQueryValue(web.name)}" +
                "&isVideo=${web.isVideo}$extraQuery",
            handle = web.handle
        )
        return null
    }

    private companion object {
        const val PORT = 8080
    }
}

/**
 * Percent-encodes a filename for a query parameter.
 *
 * Hand-written rather than reaching for `urlEncodePathSegment` in `core:protocol`: that one is
 * documented as a *path* encoder, and a space in a query value is a different rule. Filenames from
 * a camera roll routinely contain spaces and brackets.
 */
internal fun encodeQueryValue(value: String): String = encodeUriComponent(value)

@Suppress("UNUSED_PARAMETER")
private fun encodeUriComponent(value: String): String = js("encodeURIComponent(value)")

/**
 * Streams the file body with `fetch`.
 *
 * The body is the raw `File`, not a `FormData` part, because the server reads `Content-Length`
 * bytes straight from the socket into a file — it does not parse multipart. Sending FormData would
 * write the MIME boundary into the middle of the image.
 */
@Suppress("UNUSED_PARAMETER")
private fun postFile(url: String, handle: JsAny): Unit = js(
    """{
        fetch(url, { method: 'POST', body: handle })
            .then(function (response) {
                if (!response.ok) { console.error('upload rejected: HTTP ' + response.status); }
            })
            .catch(function (e) { console.error('upload failed: ' + e); });
    }"""
)
