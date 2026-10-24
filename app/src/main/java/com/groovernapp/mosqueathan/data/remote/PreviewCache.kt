package com.groovernapp.mosqueathan.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Pictures from the display, kept on this device so a controller can still show them when the
 * display is not there.
 *
 * Syncing copies the display's *rows* — a list of paths on the display's storage — and never the
 * files, so every thumbnail used to be fetched from the display on demand. Away from the mosque
 * that is a screenful of "failed to load". This keeps a downscaled copy of each picture, filled
 * when a sync completes and topped up whenever one is fetched on demand.
 *
 * **Downscaled, not the original.** The display's pictures run to several megabytes each and a
 * controller only ever draws them as previews. A copy that fits a preview is a few hundred
 * kilobytes, which is what makes copying every picture on every sync reasonable at all. It is not
 * an offline replica of the media and cannot become one: nothing here can be sent back to the
 * display.
 *
 * Stored under `filesDir`, not `cacheDir`: the system may empty a cache directory whenever storage
 * runs low, which is exactly when a phone is most likely to be away from the mosque wifi.
 */
class PreviewCache(context: Context) {

    private val dir = File(context.filesDir, DIR_NAME)

    /** The saved copy of [path], or null if there is none or it cannot be read. */
    fun read(path: String): ImageBitmap? {
        val file = fileFor(path)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    /** The saved copy of [path] as a file, for sending on; null if none was saved. */
    fun cachedFile(path: String): File? = fileFor(path).takeIf { it.exists() }

    /** Fetches [path] from the display at [host], saves a copy, and returns it; null on any failure. */
    suspend fun fetchAndStore(host: String, path: String): ImageBitmap? = withContext(Dispatchers.IO) {
        val bytes = fetchBytes(host, path) ?: return@withContext null
        store(path, bytes)
    }

    /**
     * Replaces the saved copy of every path in [paths] with a fresh one and discards the copies of
     * pictures that are no longer listed. Returns how many were saved.
     *
     * Always re-fetched rather than skipped when present: replacing a picture keeps its path, so a
     * saved copy cannot tell that it has gone stale. A failed fetch leaves the earlier copy alone.
     */
    suspend fun refresh(host: String, paths: List<String>): Int = withContext(Dispatchers.IO) {
        var saved = 0
        for (path in paths) {
            val bytes = fetchBytes(host, path) ?: continue
            if (store(path, bytes) != null) saved++
        }
        prune(keep = paths.toSet())
        saved
    }

    private fun prune(keep: Set<String>) {
        val wanted = keep.map { fileFor(it).name }.toSet()
        dir.listFiles()?.forEach { if (it.name !in wanted) it.delete() }
    }

    private fun store(path: String, bytes: ByteArray): ImageBitmap? {
        return try {
            val bitmap = decodeScaled(bytes) ?: return null
            dir.mkdirs()
            fileFor(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.asImageBitmap()
        } catch (_: Throwable) {
            // OutOfMemoryError on an oversized image must cost one picture, not the sync.
            null
        }
    }

    private fun decodeScaled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun fileFor(path: String): File {
        val digest = MessageDigest.getInstance("SHA-1").digest(path.toByteArray())
        return File(dir, digest.joinToString("") { "%02x".format(it) } + ".jpg")
    }

    private fun fetchBytes(host: String, path: String): ByteArray? {
        return try {
            val encoded = URLEncoder.encode(path, "UTF-8")
            val connection = URL("http://$host:$PORT/media?path=$encoded")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            try {
                if (connection.responseCode !in 200..299) null
                else connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private companion object {
        const val DIR_NAME = "preview-cache"
        const val PORT = 8080
        const val TIMEOUT_MS = 5_000
        const val MAX_EDGE_PX = 1280
        const val JPEG_QUALITY = 85
    }
}
