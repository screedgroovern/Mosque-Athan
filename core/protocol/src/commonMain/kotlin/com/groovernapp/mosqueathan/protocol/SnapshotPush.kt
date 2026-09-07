package com.groovernapp.mosqueathan.protocol

import com.groovernapp.mosqueathan.domain.Announcement
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.domain.SlideshowSchedule
import com.groovernapp.mosqueathan.domain.StandbySchedule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.encodeToJsonElement

/**
 * A controller's own copy of the tables it is asking the display to take.
 *
 * The mirror image of [com.groovernapp.mosqueathan.domain.MosqueSnapshot], which travels the other
 * way — and the direction that never existed until now. Sync was one GET of `/api/get_config`
 * followed by overwriting this device; a takmir who edited at home and walked to the mosque had no
 * way to hand those edits over except by retyping them at the television.
 *
 * **Null means "leave that table alone", and that is what lets one message serve every choice.**
 * `TakeServer` sends a push with all four null (or sends nothing at all), `TakeLocal` fills all
 * four, `NewestWins` fills the ones this device wrote more recently. An empty *list* is not the
 * same as null and must not be conflated: it means "this table is empty here, empty it there too",
 * which is how a running text deleted at home actually disappears from the display.
 *
 * Media is absent by design — see [SyncConflict.PUSHABLE_TABLES].
 */
@Serializable
data class SnapshotPush(
    val config: MosqueConfig? = null,
    val announcements: List<Announcement>? = null,
    val slideshowSchedules: List<SlideshowSchedule>? = null,
    val standbySchedules: List<StandbySchedule>? = null
) {
    /** True when there is nothing to send, so a caller can skip the round trip entirely. */
    val isEmpty: Boolean
        get() = config == null &&
            announcements == null &&
            slideshowSchedules == null &&
            standbySchedules == null

    /**
     * The incoming config with the fields the *receiver* owns taken back from [current].
     *
     * Only the file-bearing pair, and only in this direction. `customBackgroundPath` and
     * `murottalAudioPaths` name files on the sender's own storage, and a path on a takmir's phone
     * is not a path on the television — a display that adopted them would fall back to its default
     * background and fall silent at the Friday recitation, having discarded the paths to files it
     * actually holds. Both already reach the display by upload, which writes the path the display
     * chose, so taking the sender's list would undo an upload that had just succeeded.
     *
     * The other four withheld fields — `id`, `pinCode`, `connectivityMode`, `remoteTvIp` — are the
     * store's business in both directions and are handled there, because they are wrong to adopt
     * whichever way the snapshot travels. These two are wrong to adopt only on the way *in*.
     *
     * Null in, null out: a push that names no config asks for none.
     */
    fun sanitisedConfig(current: MosqueConfig): MosqueConfig? = config?.copy(
        customBackgroundPath = current.customBackgroundPath,
        murottalAudioPaths = current.murottalAudioPaths
    )

    companion object {
        /**
         * Builds a push carrying exactly [tables], taking each table's rows from the lambdas.
         *
         * The table names come from [SyncConflict.PUSHABLE_TABLES] rather than being spelled here,
         * so a name cannot be right in one file and wrong in the other — the same reason the action
         * names became constants.
         *
         * The rows are supplied lazily because most calls send a subset: reading four tables out of
         * a database to send one is work a takmir waits for.
         */
        fun of(
            tables: Collection<String>,
            config: () -> MosqueConfig,
            announcements: () -> List<Announcement>,
            slideshowSchedules: () -> List<SlideshowSchedule>,
            standbySchedules: () -> List<StandbySchedule>
        ): SnapshotPush = SnapshotPush(
            config = if (TABLE_CONFIG in tables) config() else null,
            announcements = if (TABLE_ANNOUNCEMENTS in tables) announcements() else null,
            slideshowSchedules = if (TABLE_SLIDESHOW in tables) slideshowSchedules() else null,
            standbySchedules = if (TABLE_STANDBY in tables) standbySchedules() else null
        )

        const val TABLE_CONFIG = "mosque_config"
        const val TABLE_ANNOUNCEMENTS = "announcements"
        const val TABLE_SLIDESHOW = "slideshow_schedules"
        const val TABLE_STANDBY = "standby_schedules"
    }
}

/**
 * Writes [push] into an `apply_snapshot` message.
 *
 * `kotlinx.serialization` rather than field-by-field `put` calls, unlike [pushConfig] beside it.
 * That is not inconsistency: `update_config` is a *partial* write whose absent keys mean "keep what
 * you have", so its field list is a contract the server reads key by key. A snapshot is a whole
 * object in both directions, so the class *is* the contract — and a config field added later
 * crosses on its own here, where `pushConfig` would silently keep sending 38 of 39.
 */
fun JsonObjectBuilder.putSnapshotPush(push: SnapshotPush) {
    val encoded = snapshotPushJson.encodeToJsonElement(push)
    (encoded as kotlinx.serialization.json.JsonObject).forEach { (key, value) -> put(key, value) }
}

/**
 * Reads a push out of a whole `apply_snapshot` message.
 *
 * The message carries `action` and the sender's `deviceId` alongside the tables, so this decodes
 * the envelope and the payload in one pass rather than digging the tables out first —
 * `ignoreUnknownKeys` is what makes that work, and it is the same tolerance a snapshot travelling
 * the other way relies on.
 */
fun parseSnapshotPush(message: String): SnapshotPush =
    snapshotPushJson.decodeFromString(message)

/**
 * The codec for a push, in both directions.
 *
 * `explicitNulls = false` is what makes "leave that table alone" cost nothing on the wire, and it
 * also means an older display that has never heard of a table simply does not see the key. Note it
 * is the *opposite* default from `mosqueJson`, which keeps every key present so a display's
 * snapshot is complete; there an absent key is the danger, here it is the message.
 */
val snapshotPushJson = kotlinx.serialization.json.Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}
