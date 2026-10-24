package com.groovernapp.mosqueathan.protocol

/**
 * Every action name the WebSocket protocol carries.
 *
 * These exist because each name used to be typed twice — once in
 * `SettingsServer.handleWebSocketMessage`'s `when`, and once in the hand-written panel's
 * JavaScript, which was itself a Kotlin raw string. Nothing connected the two, so a mismatch
 * was invisible until a button quietly stopped working. That is not hypothetical: the
 * panel's rename button sent `register_device` for an unknown length of time while the
 * server only ever handled `register`.
 *
 * Referencing a constant from both sides makes that drift a compile error rather than a
 * silent no-op. The JavaScript panel has since been deleted and every sender is Kotlin, but
 * the constants stay: the values are the exact strings the protocol already used, so a
 * controller built from an older commit still talks to a newer server.
 *
 * Three schedule actions used to be assembled in JavaScript as `'update_' + type +
 * '_schedule_days'`. They remain here for the server.
 */
object RemoteActions {

    // Pairing and access
    const val DELETE_DEVICE = "delete_device"
    const val PING = "ping"
    const val REGISTER = "register"
    const val SET_MASTER = "set_master"
    const val VERIFY_PIN = "verify_pin"

    // Schedules
    const val ADD_SLIDESHOW_SCHEDULE = "add_slideshow_schedule"
    const val ADD_STANDBY_SCHEDULE = "add_standby_schedule"
    const val DELETE_SLIDESHOW_SCHEDULE = "delete_slideshow_schedule"
    const val DELETE_STANDBY_SCHEDULE = "delete_standby_schedule"
    const val SAVE_KEMENAG_SCHEDULES = "save_kemenag_schedules"
    // The display fetches, because it has both the API access and the storage. The old
    // panel fetched in the browser and posted the result back one day at a time, which is
    // up to 180 messages for one button press.
    const val FETCH_KEMENAG_SCHEDULES = "fetch_kemenag_schedules"
    const val UPDATE_SLIDESHOW_SCHEDULE_DATES = "update_slideshow_schedule_dates"
    const val UPDATE_SLIDESHOW_SCHEDULE_DAYS = "update_slideshow_schedule_days"
    const val UPDATE_SLIDESHOW_SCHEDULE_TIME = "update_slideshow_schedule_time"
    const val UPDATE_STANDBY_SCHEDULE_DATES = "update_standby_schedule_dates"
    const val UPDATE_STANDBY_SCHEDULE_DAYS = "update_standby_schedule_days"
    const val UPDATE_STANDBY_SCHEDULE_TIME = "update_standby_schedule_time"

    // Kemenag
    const val SEARCH_KEMENAG_CITIES = "search_kemenag_cities"

    // Media and backgrounds
    const val CLEAR_BACKGROUND = "clear_background"
    const val DELETE_DASHBOARD_BACKGROUND = "delete_dashboard_background"
    const val DELETE_FRIDAY_MEDIA = "delete_friday_media"
    const val DELETE_MEDIA = "delete_media"
    const val PREVIEW_FRIDAY_SLIDESHOW = "preview_friday_slideshow"
    const val PREVIEW_SLIDESHOW = "preview_slideshow"
    const val REORDER_DASHBOARD_BACKGROUNDS = "reorder_dashboard_backgrounds"
    // Newer than the rest, and the only two the hand-written panel never had: the display
    // stored Media.isUsed and a slide order that no controller could change.
    const val REORDER_MEDIA = "reorder_media"
    const val REORDER_FRIDAY_MEDIA = "reorder_friday_media"
    const val UPDATE_DASHBOARD_BACKGROUND_PATH = "update_dashboard_background_path"
    const val UPDATE_DASHBOARD_BACKGROUND_USED = "update_dashboard_background_used"
    const val UPDATE_FRIDAY_MEDIA_ITEM_PATH = "update_friday_media_item_path"
    const val UPDATE_MEDIA_ITEM_PATH = "update_media_item_path"
    const val UPDATE_MEDIA_ITEM_USED = "update_media_item_used"
    const val UPDATE_SLIDESHOW_ENABLED = "update_slideshow_enabled"
    const val UPLOAD_BACKGROUND = "upload_background"
    const val UPLOAD_DASHBOARD_BACKGROUND = "upload_dashboard_background"
    const val UPLOAD_FRIDAY_MEDIA = "upload_friday_media"
    const val UPLOAD_MEDIA = "upload_media"

    // Announcements
    const val ADD_ANNOUNCEMENT = "add_announcement"
    const val DELETE_ANNOUNCEMENT = "delete_announcement"
    const val REORDER_ANNOUNCEMENTS = "reorder_announcements"
    const val UPDATE_ANNOUNCEMENT = "update_announcement"

    // Preview
    const val PREVIEW_ADZAN = "preview_adzan"
    const val PREVIEW_IQOMAH = "preview_iqomah"

    /** Ends whichever preview the display is showing and returns it to the screen it left. */
    const val EXIT_PREVIEW = "exit_preview"

    // Everything else
    const val RESET = "reset"
    const val SET_PIN = "set_pin"
    const val UPDATE_CONFIG = "update_config"

    /**
     * Replaces whole tables with a controller's copy, in one message and one transaction.
     *
     * The push direction, which did not exist: every other action changes one thing, and `sync`
     * only ever *pulled* — so a takmir who edited at home had no way to make the display take their
     * copy. Only the tables present in the payload are touched, which is what lets one action serve
     * "keep mine", "keep theirs" and "keep whichever is newer, per table" without three code paths.
     *
     * Media rows are deliberately not among them. They name files, and a file on a phone is not a
     * file on the display — pushing the rows would leave a television showing "failed to load
     * image" for every slide. Media crosses through the upload endpoint, which already works.
     */
    const val APPLY_SNAPSHOT = "apply_snapshot"
    const val UPDATE_DASHBOARD_SETTINGS = "update_dashboard_settings"
    const val UPDATE_SHOW_SETTINGS_ON_TV = "update_show_settings_on_tv"
    const val UPDATE_STANDBY = "update_standby"
    const val UPDATE_THEME = "update_theme"

    /** Actions a client may send before it has been granted access. */
    val UNAUTHENTICATED = setOf(REGISTER, VERIFY_PIN, PING)
}
