package com.groovernapp.mosqueathan.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Everything a controller needs to show the display's settings, in the shape the display already
 * serves.
 *
 * This is not a new format. `SettingsServer.getCurrentStateJson()` builds exactly this — the same
 * payload answers `GET /api/get_config` and is pushed to every WebSocket client on any change — and
 * its keys were already the domain classes' property names. Verified against a live device rather
 * than assumed: all seven lists decoded with no unmatched key, and of the config's fields only
 * `hasPin` had no counterpart.
 *
 * Deliberately absent:
 * - **`pinCode`** — the display sends `hasPin`, never the PIN. Keep it that way.
 * - **`connectivityMode`, `remoteTvIp`** — each device's own role and target, never the other's.
 * - **`kemenagSchedules`** — the payload carries them, but the live device had none, so the key
 *   names are unverified. `ignoreUnknownKeys` skips them rather than risking a decode failure on
 *   a guess; add them once a device with downloaded schedules confirms the shape.
 *
 * `jumatTransitionType` and `jumatVideoWaitUntilEnd` used to be missing from the payload, so every
 * controller showed their defaults. That is fixed in the display, and
 * `ConfigPayloadCoverageTest` now fails if any config field goes unserved without a stated reason.
 */
@Serializable
data class MosqueSnapshot(
    val config: MosqueConfig = MosqueConfig(),
    val announcements: List<Announcement> = emptyList(),
    val mediaList: List<Media> = emptyList(),
    val fridayMediaList: List<FridayMedia> = emptyList(),
    val dashboardBackgrounds: List<DashboardBackground> = emptyList(),
    val devices: List<ConnectedDevice> = emptyList(),
    val slideshowSchedules: List<SlideshowSchedule> = emptyList(),
    val standbySchedules: List<StandbySchedule> = emptyList()
)

/**
 * The reader for [MosqueSnapshot].
 *
 * `ignoreUnknownKeys` and every field defaulted are both load-bearing, and for the same reason
 * from two directions: a display running an older build omits keys this one expects, and a newer
 * one sends keys it does not know. Neither should be an error — the two devices in a mosque are
 * not updated at the same moment.
 */
val mosqueJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
