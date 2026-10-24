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

    /**
     * Keeps a copy of the display's pictures on this device, so they still show when it is away.
     *
     * A sync copies the display's rows, which only *name* files on the display. A controller that
     * stopped there showed "failed to load" for every picture the moment it left the mosque's wifi.
     * [host] is the display, [filePaths] are the pictures it holds, and the result is how many were
     * saved. The copies are previews, not the media: nothing here can be sent back.
     *
     * Nothing to do by default: a display holds the files itself, and a browser stays connected.
     */
    suspend fun cacheForOffline(host: String, filePaths: List<String>): Int = 0

    /**
     * Copies pictures this device holds to a display that does not have them, and returns how many
     * were accepted.
     *
     * What is sent is the saved preview of each picture ([cacheForOffline]), not an original: the
     * originals only ever existed on the display they came from. So a picture arrives smaller than
     * it left, and one that was never saved here cannot be sent at all. The display adds the rows
     * itself. Nothing to do by default: only an Android controller holds copies to send.
     */
    suspend fun sendPicturesToDisplay(host: String, pictures: List<PictureToSend>): Int = 0
}

/** Which of the display's picture lists a picture belongs to. */
enum class PictureKind { Slideshow, Friday, Dashboard }

/** A picture to copy to a display: [path] is how this device knows it, [kind] where it goes there. */
data class PictureToSend(val path: String, val kind: PictureKind)
