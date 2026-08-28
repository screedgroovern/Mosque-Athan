package com.groovernapp.mosqueathan.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Plays the Qur'an recitation that precedes the Friday adzan.
 *
 * **This is the one sound the app is meant to make.** `VideoSurface` mutes every video it plays,
 * because the adzan and the sermon come from the building's own sound system and a television
 * talking over them is a fault. Murottal is the opposite: the recitation *is* the audio, played
 * through whatever the display is connected to while the mosque fills up.
 *
 * Owned by [com.groovernapp.mosqueathan.ui.navigation.AppViewModel]'s coordinator rather than
 * driven by its own timer. There is already a clock ticking once a second that knows the date, the
 * prayer times and the Friday window; a second one would be a second opinion about when Friday
 * starts.
 *
 * Everything here is main-thread: [MediaPlayer]'s callbacks arrive on the thread that created it,
 * and the coordinator runs on Main, so no synchronisation is needed and none is implied.
 */
class MurottalPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private var queue: List<String> = emptyList()
    private var index = 0
    private var focusRequest: AudioFocusRequest? = null

    /** True between [start] and [stop], including the gap while one file hands over to the next. */
    var isPlaying: Boolean = false
        private set

    private val audioManager: AudioManager?
        get() = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /**
     * Starts playing [paths] in order, looping back to the first when the last finishes.
     *
     * Looping because the window is defined by the clock, not by the audio: the recitation runs
     * until the adzan, however long that is. A mosque with one short file would otherwise sit in
     * silence for the rest of the wait.
     *
     * Calling this while already playing does nothing — the coordinator calls it every second.
     */
    fun start(paths: List<String>) {
        if (isPlaying) return
        val playable = paths.map { it.trim() }.filter { it.isNotEmpty() }
        if (playable.isEmpty()) return
        if (!requestFocus()) {
            Log.w(TAG, "Audio focus refused; not starting murottal")
            return
        }
        queue = playable
        index = 0
        isPlaying = true
        playCurrent()
    }

    /** Stops and releases everything. Safe to call when not playing; the coordinator does. */
    fun stop() {
        if (!isPlaying && player == null) return
        isPlaying = false
        releasePlayer()
        abandonFocus()
    }

    private fun playCurrent() {
        if (!isPlaying) return
        if (queue.isEmpty()) {
            stop()
            return
        }
        // A whole pass that played nothing means every file is unreadable — stop rather than spin
        // through a list of missing paths once a second for the rest of the morning.
        var attempts = 0
        while (attempts < queue.size) {
            val path = queue[index % queue.size]
            index = (index + 1) % queue.size
            attempts++
            if (openAndPlay(path)) return
        }
        Log.e(TAG, "None of the ${queue.size} configured recitations could be played")
        stop()
    }

    /**
     * @return true if playback actually started.
     *
     * A file that is missing or unplayable is skipped rather than fatal: a mosque with five
     * recitations should not fall silent because one of them was deleted from storage.
     */
    private fun openAndPlay(path: String): Boolean {
        if (!File(path).exists()) {
            Log.w(TAG, "Recitation missing, skipping")
            return false
        }
        return try {
            releasePlayer()
            player = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(path)
                setOnCompletionListener { playCurrent() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Playback error ($what/$extra), skipping to the next recitation")
                    // Returning true claims the error: without it MediaPlayer also fires
                    // onCompletion, and the queue would advance twice for one failure.
                    playCurrent()
                    true
                }
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not play a recitation", e)
            releasePlayer()
            false
        }
    }

    private fun releasePlayer() {
        player?.let { existing ->
            // Cleared first: releasing fires neither callback, but a queued one from the previous
            // file would otherwise advance the queue after we have moved on.
            existing.setOnCompletionListener(null)
            existing.setOnErrorListener(null)
            runCatching { if (existing.isPlaying) existing.stop() }
            runCatching { existing.release() }
        }
        player = null
    }

    private fun requestFocus(): Boolean {
        val manager = audioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                // Anything that takes focus mid-recitation — a system sound, a cast session — means
                // something else should be heard, so give up rather than duck and play under it.
                .setOnAudioFocusChangeListener { change ->
                    if (change != AudioManager.AUDIOFOCUS_GAIN) stop()
                }
                .build()
            focusRequest = request
            manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                { change -> if (change != AudioManager.AUDIOFOCUS_GAIN) stop() },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { runCatching { manager.abandonAudioFocusRequest(it) } }
            focusRequest = null
        }
    }

    private companion object {
        const val TAG = "MurottalPlayer"
    }
}
