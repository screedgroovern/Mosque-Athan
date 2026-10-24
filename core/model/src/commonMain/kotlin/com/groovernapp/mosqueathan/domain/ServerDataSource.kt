package com.groovernapp.mosqueathan.domain

/**
 * Everything a controller writes, plus the eight members only the server needs.
 *
 * The third and largest of the three interfaces that name how much authority a consumer has:
 * [DisplayDataSource] is the nine reads the display screens use, [ConfigRepository] adds what the
 * settings UI writes, and this adds device registration, presence and the raw Kemenag rows — none
 * of which any UI touches, because they are things a *server* does when a client connects.
 *
 * ### Why this exists now when it deliberately did not before
 *
 * `SettingsServer` used to take the concrete `MosqueConfigRepository`, and this file's absence was
 * a stated decision: the server is Android-only and never being ported, so abstracting it bought
 * nothing. That reasoning has expired. The concrete class is Realm, and these eight members were
 * the last thing pinning the app to it — a Room-backed repository could satisfy `ConfigRepository`
 * completely and still not be usable, because the server asked for a type rather than a capability.
 * So the interface is not for portability; it is what lets the store be replaced at all.
 *
 * Nothing here is on `ConfigRepository` itself. A settings pane has no business registering a
 * device or marking one online, and putting these where the panes can see them would be the same
 * mistake `DisplayDataSource` exists to prevent, one level up.
 */
interface ServerDataSource : ConfigRepository {

    /**
     * The announcements the display is showing, active ones only.
     *
     * Named for what it returns rather than for the table. Note the consequence, which is
     * pre-existing and worth knowing before relying on it: the state payload is built from this,
     * so a controller never sees a switched-off announcement at all — the off state is invisible
     * outside the display's own settings UI.
     */
    fun getActiveAnnouncements(): List<Announcement>

    /** Every paired device, however long ago it was last seen. */
    fun getConnectedDevices(): List<ConnectedDevice>

    /** One city's cached Kemenag days, for the state payload. */
    fun getKemenagSchedules(cityId: String): List<KemenagSchedule>

    /**
     * Records a device that has just connected, creating it if this is the first time.
     *
     * The first device ever to register becomes master, and a device is allowed when it is that
     * first one or when no master exists yet — which is how a mosque with no PIN set stays usable
     * without anyone approving anything.
     */
    suspend fun registerDevice(id: String, name: String): ConnectedDevice

    /** Stores days fetched by a client rather than by the display itself. */
    suspend fun saveKemenagSchedules(schedules: List<KemenagSchedule>)

    /** Points an existing background row at a different file, after an upload replaced it. */
    suspend fun updateDashboardBackgroundPath(id: String, filePath: String)

    /**
     * Marks every picture table that names [filePath] as changed.
     *
     * For an upload that replaced a file's bytes and left its path alone: no row is written, so
     * nothing else would move the table's stamp, and a controller deciding whether to re-read the
     * display's pictures goes by that stamp. Without it a replaced picture looks unchanged forever.
     */
    suspend fun touchMediaFile(filePath: String)

    /** Approves or refuses a device; approving also restarts its session clock. */
    suspend fun updateDeviceAllowed(id: String, allowed: Boolean)

    /** Marks a device present or gone as its socket opens and closes. */
    suspend fun updateDeviceOnlineStatus(id: String, online: Boolean)
}
