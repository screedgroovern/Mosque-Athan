package com.groovernapp.mosqueathan.data.media

import android.content.Context
import android.net.Uri
import com.groovernapp.mosqueathan.domain.AndroidPickedFile
import com.groovernapp.mosqueathan.domain.MediaStore
import com.groovernapp.mosqueathan.domain.PickedFile
import com.groovernapp.mosqueathan.domain.StoredMedia
import android.provider.OpenableColumns
import android.util.Log
import com.groovernapp.mosqueathan.util.ImageUtils
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
class MediaFileStore(private val context: Context) : MediaStore {

    override fun storePresentationMedia(file: PickedFile): StoredMedia? = copy(file.uri, PRESENTATION_DIR)

    override fun storeDashboardBackground(file: PickedFile): StoredMedia? = copy(file.uri, DASHBOARD_DIR)

    /** Friday slides live in their own directory: they are a separate library, not a subset. */
    override fun storeFridayMedia(file: PickedFile): StoredMedia? = copy(file.uri, FRIDAY_DIR)

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
        const val CUSTOM_BACKGROUND = "custom_background.jpg"
        const val LARGE_BYTES = 2L * 1024 * 1024
    }
}
