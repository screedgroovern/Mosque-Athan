package com.groovernapp.mosqueathan.data.media

import android.content.Context
import android.net.Uri
import com.groovernapp.mosqueathan.domain.AndroidPickedFile
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.domain.PictureKind
import com.groovernapp.mosqueathan.domain.PictureToSend
import com.groovernapp.mosqueathan.domain.StoredMedia
import android.provider.OpenableColumns
import android.util.Log
import com.groovernapp.mosqueathan.util.ImageUtils
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.File
import java.util.UUID

private val PickedFile.uri: Uri
    get() = (this as AndroidPickedFile).uri

/**
 * Copies picked media into the app's own storage.
 *
 * Extracted so that ViewModels stop taking an Android `Context` as a method
 * parameter. Six settings actions did — adding slideshow media, replacing a media
 * file, the custom background, and the three dashboard-background operations — which
 * meant every one of them had to be routed back through the screen.
 *
 * This is also the seam for step 6: picking and copying files is the part of the
 * settings flow that genuinely cannot be shared, so this class is what becomes
 * `expect`/`actual`.
 *
 * Images are downscaled in place after copying; a phone camera photo is several
 * megabytes and would be decoded in full on every slideshow tick otherwise.
 */
class MediaFileStore(
    private val context: Context,
    private val previews: com.groovernapp.mosqueathan.data.remote.PreviewCache,
    private val repository: com.groovernapp.mosqueathan.domain.ConfigRepository
) : MediaStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** What happened to an upload to the display, for the screen to say. Nothing else reports it. */
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private val _uploadsFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits once the display has accepted an upload, so a controller can re-read its pictures now. */
    val uploadsFinished: SharedFlow<Unit> = _uploadsFinished.asSharedFlow()

    override suspend fun cacheForOffline(host: String, filePaths: List<String>): Int =
        previews.refresh(host, filePaths)

    /**
     * Sends the saved previews to a display that lacks them, one after another so they arrive in the
     * order they were listed. A picture with no saved preview is skipped, not failed: this device
     * never held the original, so there is nothing else to send.
     */
    override suspend fun sendPicturesToDisplay(host: String, pictures: List<PictureToSend>): Int =
        withContext(Dispatchers.IO) {
            var accepted = 0
            for (picture in pictures) {
                val file = previews.cachedFile(picture.path) ?: continue
                val kind = when (picture.kind) {
                    PictureKind.Slideshow -> ""
                    PictureKind.Friday -> "&isFriday=true"
                    PictureKind.Dashboard -> "&isDashboardBg=true"
                }
                val name = URLEncoder.encode("${UUID.randomUUID()}.jpg", "UTF-8")
                val code = try {
                    post(host, "filename=$name&isVideo=false$kind", file)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not send a picture to $host", e)
                    -1
                }
                if (code in 200..299) accepted++
            }
            accepted
        }

    override fun storePresentationMedia(file: PickedFile): StoredMedia? = store(file, PRESENTATION_DIR, "")

    override fun storeDashboardBackground(file: PickedFile): StoredMedia? =
        store(file, DASHBOARD_DIR, "isDashboardBg=true")

    /** Friday slides live in their own directory: they are a separate library, not a subset. */
    override fun storeFridayMedia(file: PickedFile): StoredMedia? =
        store(file, FRIDAY_DIR, "isFriday=true")

    /**
     * Where a picked picture goes depends on which device this is.
     *
     * On the display it is copied into this device's storage and the caller adds the row. On a
     * controller it is **uploaded to the display** and this returns null, because the display adds
     * the row itself — the same arrangement the browser panel uses. Copying it locally instead left
     * the picture on the phone with a row the display never heard of: it showed "failed to load" on
     * the controller and did not appear on the display at all.
     */
    private fun store(file: PickedFile, dirName: String, uploadQuery: String): StoredMedia? {
        val host = displayHost() ?: return copy(file.uri, dirName)
        upload(host, file.uri, uploadQuery)
        return null
    }

    /** The display this device controls, or null when it *is* the display (or has none to reach). */
    private fun displayHost(): String? {
        val config = repository.getConfig()
        return config.remoteTvIp.trim().takeIf { config.connectivityMode == REMOTE_MODE && it.isNotEmpty() }
    }

    /**
     * Copies a recitation in, keeping its real extension.
     *
     * Not [copy]: that one decides between `.jpg` and `.mp4` and downscales images, both wrong
     * here. The extension is preserved because a media player picks its decoder from the file, and
     * a `.mp3` renamed to `.jpg` is a decoder failure waiting for a Friday.
     *
     * The file is copied rather than referenced. The picker's content URI used to *be* the setting,
     * which tied a stored preference to a permission grant — and to the device that granted it.
     */
    override fun storeMurottalAudio(file: PickedFile): StoredMedia? {
        val uri = file.uri
        // A controller cannot produce a path the display can open, so it uploads and lets the
        // display record the path it chose.
        displayHost()?.let { host ->
            upload(host, uri, "isAudio=true")
            return null
        }
        return try {
            val dir = File(context.filesDir, MUROTTAL_DIR).apply { if (!exists()) mkdirs() }
            val target = File(dir, "${System.currentTimeMillis()}_${audioFileName(uri)}")
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
                true
            } ?: false
            if (copied) StoredMedia(target.absolutePath, isVideo = false, wasLarge = false) else null
        } catch (e: Exception) {
            Log.e(TAG, "Could not store murottal audio", e)
            null
        }
    }

    /**
     * The chosen file's own name, sanitised.
     *
     * Falls back to `.mp3` because that is what murottal recordings are in practice, and a name
     * with no extension at all is worse than a wrong guess: some players refuse it outright.
     */
    private fun audioFileName(uri: Uri): String {
        val display = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (_: Exception) {
            null
        }
        val name = (display ?: "murottal.mp3").replace(Regex("[/\\\\]"), "_")
        return if (name.contains('.')) name else "$name.mp3"
    }

    /**
     * Deletes a file this store owns.
     *
     * The repository removes the row; nothing else removes the bytes. Friday slides
     * can be videos, so leaving them behind fills a television's internal storage
     * with files no screen will ever show again.
     */
    override fun delete(path: String): Boolean = try {
        val file = File(path)
        !file.exists() || file.delete()
    } catch (e: Exception) {
        Log.e(TAG, "Could not delete $path", e)
        false
    }

    /** Overwrites an existing entry's file, keeping its path so the database row stands. */
    override fun replaceFile(path: String, file: PickedFile): Boolean {
        val uri = file.uri
        // The path names a file on the display, so the bytes go there. False: dispatched, and the
        // display's own change is what the controller will see on its next look.
        displayHost()?.let { host ->
            upload(host, uri, "replacePath=${URLEncoder.encode(path, "UTF-8")}")
            return false
        }
        val target = File(path)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return false
            if (!target.isVideoByExtension()) ImageUtils.downscaleImageInPlace(target)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not replace $path", e)
            false
        }
    }

    override fun storeCustomBackground(file: PickedFile): StoredMedia? {
        val uri = file.uri
        displayHost()?.let { host ->
            upload(host, uri, "isBackground=true")
            return null
        }
        val target = File(context.filesDir, CUSTOM_BACKGROUND)
        return try {
            val large = isLarge(uri)
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return null
            ImageUtils.downscaleImageInPlace(target)
            StoredMedia(target.absolutePath, isVideo = false, wasLarge = large)
        } catch (e: Exception) {
            Log.e(TAG, "Could not store the custom background", e)
            null
        }
    }

    /**
     * Removes the custom background, so the display falls back to the bundled default.
     *
     * Here rather than in a ViewModel because this class already knows the filename and
     * the directory; a reset that reached for `context.filesDir` itself would be a
     * second place that had to agree about both.
     */
    override fun deleteCustomBackground(): Boolean =
        delete(File(context.filesDir, CUSTOM_BACKGROUND).absolutePath)

    private fun copy(uri: Uri, dirName: String): StoredMedia? {
        return try {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
            val large = !isVideo && isLarge(uri)

            val dir = File(context.filesDir, dirName).apply { if (!exists()) mkdirs() }
            val name = "${dirName}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}" +
                if (isVideo) ".mp4" else ".jpg"
            val target = File(dir, name)

            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
                true
            } ?: false
            if (!copied) return null

            if (!isVideo) ImageUtils.downscaleImageInPlace(target)
            StoredMedia(target.absolutePath, isVideo, large)
        } catch (e: Exception) {
            Log.e(TAG, "Could not store $uri", e)
            null
        }
    }

    /**
     * Sends a picked file to the display's `POST /upload`, off the main thread.
     *
     * Copied to a temporary file first, and that is the point rather than a detour: the endpoint
     * reads exactly `Content-Length` bytes, so the length has to be known before the first byte
     * goes out, and a content `Uri` often cannot say. It also lets an image be downscaled on the
     * phone first, so a camera photo does not cross the wifi at full size only to be shrunk on the
     * other end.
     *
     * Fire-and-forget, because [MediaStore] is not suspending; the outcome comes back through
     * [notices] and [uploadsFinished].
     */
    private fun upload(host: String, uri: Uri, extraQuery: String) {
        scope.launch {
            val temp = File(context.cacheDir, "upload_${System.nanoTime()}.tmp")
            try {
                val resolver = context.contentResolver
                val isVideo = resolver.getType(uri)?.startsWith("video") == true
                val isAudio = extraQuery.contains("isAudio=true")
                resolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { input.copyTo(it) }
                } ?: throw java.io.IOException("the picked file could not be read")
                if (!isVideo && !isAudio) ImageUtils.downscaleImageInPlace(temp)

                val name = if (isAudio) audioFileName(uri) else pickedFileName(uri)
                val query = "filename=${URLEncoder.encode(name, "UTF-8")}&isVideo=$isVideo" +
                    if (extraQuery.isEmpty()) "" else "&$extraQuery"
                val code = post(host, query, temp)
                if (code in 200..299) {
                    _notices.emit(Lang.get("Sent to the display.", "Terkirim ke display."))
                    _uploadsFinished.tryEmit(Unit)
                } else {
                    _notices.emit(
                        Lang.get("The display refused the file (HTTP $code).", "Display menolak file (HTTP $code).")
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload to the display failed", e)
                _notices.emit(
                    Lang.get(
                        "Could not send the file to the display: ${e.message}",
                        "Gagal mengirim file ke display: ${e.message}"
                    )
                )
            } finally {
                temp.delete()
            }
        }
    }

    private fun post(host: String, query: String, file: File): Int {
        val connection = URL("http://$host:$PORT/upload?$query").openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            // Long: a video over mosque wifi is minutes, and the display answers only at the end.
            connection.readTimeout = UPLOAD_TIMEOUT_MS
            connection.setFixedLengthStreamingMode(file.length())
            file.inputStream().use { input -> connection.outputStream.use { input.copyTo(it) } }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }

    private fun pickedFileName(uri: Uri): String {
        val display = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (_: Exception) {
            null
        }
        return (display ?: "upload.jpg").replace(Regex("[/\\\\]"), "_")
    }

    /** Above this, downscaling is worth telling the user about. */
    private fun isLarge(uri: Uri): Boolean = sizeOf(uri) > LARGE_BYTES

    private fun sizeOf(uri: Uri): Long = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && column != -1) cursor.getLong(column) else 0L
        } ?: 0L
    } catch (_: Exception) {
        0L
    }

    private fun File.isVideoByExtension() = extension.equals("mp4", ignoreCase = true)

    private companion object {
        const val TAG = "MediaFileStore"
        const val PRESENTATION_DIR = "presentation"
        const val DASHBOARD_DIR = "dashboard"
        const val FRIDAY_DIR = "friday_media"
        const val MUROTTAL_DIR = "murottal"
        const val CUSTOM_BACKGROUND = "custom_background.jpg"
        const val LARGE_BYTES = 2L * 1024 * 1024
        const val REMOTE_MODE = "REMOTE"
        const val PORT = 8080
        const val CONNECT_TIMEOUT_MS = 5_000
        const val UPLOAD_TIMEOUT_MS = 10 * 60 * 1000
    }
}
