package com.groovernapp.mosqueathan.domain

/**
 * A file the user has just picked, in whatever form the platform hands it over.
 *
 * Opaque on purpose. On Android it is a content `Uri` — not a path, and not openable as
 * one; it is a grant to read whatever the picker chose. A ViewModel has no business
 * knowing that, and every platform's answer is different, so this carries no members and
 * only the [MediaStore] implementation ever looks inside it.
 *
 * An interface rather than `expect class`, which cannot work here: an `expect class`
 * implies a no-argument constructor, and `android.net.Uri` is abstract. Plain
 * polymorphism also keeps a Beta language feature out of the build for no loss — the
 * implementation pairs with the platform that produced the handle, so its cast back is
 * safe by construction.
 */
interface PickedFile

/**
 * A file copied into the app's own storage, and what the display needs to know about it.
 *
 * [wasLarge] is not metadata for its own sake: an oversized image is downscaled on the
 * way in, and the settings UI tells the takmir that happened rather than leaving them to
 * wonder why their photo looks different.
 */
data class StoredMedia(val path: String, val isVideo: Boolean, val wasLarge: Boolean)

/**
 * Where picked files go, and how they come back out.
 *
 * An interface so `ui:settings` can be built without a file system. The Android
 * implementation copies out of a content `Uri` into app storage and downscales large
 * images; a web controller will do something entirely unlike that.
 *
 * Every store returns null rather than throwing on failure. A file that cannot be read —
 * a revoked permission, a picker returning something undecodable — should leave the
 * previous media in place, not take the settings screen down.
 */
interface MediaStore {

    fun storePresentationMedia(file: PickedFile): StoredMedia?

    fun storeDashboardBackground(file: PickedFile): StoredMedia?

    fun storeFridayMedia(file: PickedFile): StoredMedia?

    fun storeCustomBackground(file: PickedFile): StoredMedia?

    /**
     * Stores a murottal recitation and returns where it landed.
     *
     * Audio is different from the media above in one way that shapes everything: the *path* is the
     * setting. `murottalAudioPaths` is read by the display at playback time, so whatever this
     * returns has to be a path the display can open — which is why a picked file is copied rather
     * than referenced. A controller cannot produce such a path itself, so its implementation
     * uploads instead and returns null, leaving the display to record the path it chose.
     *
     * [StoredMedia.isVideo] is always false here and [StoredMedia.wasLarge] is meaningless — audio
     * is never downscaled. Only [StoredMedia.path] matters.
     */
    fun storeMurottalAudio(file: PickedFile): StoredMedia?

    /** Overwrites the file at [path] with [file], keeping the path the database holds. */
    fun replaceFile(path: String, file: PickedFile): Boolean

    fun delete(path: String): Boolean

    /** Removes the custom background, so the display falls back to the bundled default. */
    fun deleteCustomBackground(): Boolean
}
