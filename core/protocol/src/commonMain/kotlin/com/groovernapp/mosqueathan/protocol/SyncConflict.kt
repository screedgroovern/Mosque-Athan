package com.groovernapp.mosqueathan.protocol

import kotlinx.serialization.Serializable

/**
 * Which side of a sync wins, table by table.
 *
 * Pure, and separated from the sync itself for the same reason `shouldPlayMurottal` is separated
 * from the player: the decision is where the damage happens, and a decision that can only be
 * observed by running a real sync against a real display is a decision nobody can test.
 *
 * ### What this deliberately is not
 *
 * It is not a merge. It chooses a *side per table*, not per field, and it cannot tell an addition
 * from a deletion — a row present here and absent there is equally "I just added it" and "they
 * just removed it", and no timestamp distinguishes those. Answering that needs a third snapshot,
 * the state as of the last successful sync, and that is a later change. Until then [Resolution]
 * says which side supplies each table and the caller keeps rows the other side lacks rather than
 * deleting them; losing a takmir's work is worse than leaving a row to be removed by hand.
 */
object SyncConflict {

    /** The tables two devices can disagree about. Devices and Kemenag rows are excluded — access
     *  control is the display's alone, and Kemenag schedules are a re-fetchable cache. */
    val COMPARED_TABLES: List<String> = listOf(
        "mosque_config",
        "announcements",
        "media",
        "friday_media",
        "dashboard_backgrounds",
        "slideshow_schedules",
        "standby_schedules"
    )

    /** The newest write in each table, as each side reports it. Missing table means never written. */
    @Serializable
    data class Stamps(val byTable: Map<String, Long> = emptyMap()) {
        fun of(table: String): Long = byTable[table] ?: 0L
    }

    /** What the takmir picked in the dialog. */
    enum class Choice {
        /** The display's copy replaces this device's, wholesale. */
        TakeServer,

        /** This device's copy is pushed to the display, wholesale. */
        TakeLocal,

        /**
         * Per table, the side that wrote most recently wins.
         *
         * Two devices, two clocks — so this can be wrong when one of them is wrong, which is why
         * the dialog shows both timestamps rather than only offering the button. It informs; it
         * must never be the only thing the takmir sees.
         */
        NewestWins
    }

    /** Where each table's data should come from once the takmir has chosen. */
    enum class Side { Server, Local }

    @Serializable
    data class Resolution(val byTable: Map<String, Side>) {
        fun sideFor(table: String): Side = byTable[table] ?: Side.Server
    }

    /**
     * Applies [choice] to the two sides' timestamps.
     *
     * A tie under [Choice.NewestWins] goes to the server, because the display is the device the
     * mosque actually sees and the one a second takmir may be standing in front of. Equal
     * timestamps most often mean neither side changed at all, in which case the choice is moot.
     */
    fun resolve(choice: Choice, server: Stamps, local: Stamps): Resolution = Resolution(
        COMPARED_TABLES.associateWith { table ->
            when (choice) {
                Choice.TakeServer -> Side.Server
                Choice.TakeLocal -> Side.Local
                Choice.NewestWins ->
                    if (local.of(table) > server.of(table)) Side.Local else Side.Server
            }
        }
    )

    /**
     * The tables the two sides disagree about, which is what the dialog should name.
     *
     * Different timestamps are the only evidence available here, and they answer "did both sides
     * write?" rather than "is the content different" — two devices can write identical values. That
     * over-reports rather than under-reports, which is the right direction for a prompt about
     * losing work.
     */
    fun differingTables(server: Stamps, local: Stamps): List<String> =
        COMPARED_TABLES.filter { server.of(it) != local.of(it) }

    /**
     * Whether the takmir needs to be asked at all.
     *
     * False when one side has never written anything the other has — a fresh controller pairing
     * with an established display is not a conflict, and asking there is how a dialog earns the
     * reflex that dismisses it later, on the one occasion it mattered.
     */
    fun needsAsking(server: Stamps, local: Stamps): Boolean {
        val differing = differingTables(server, local)
        if (differing.isEmpty()) return false
        val localWroteSomething = COMPARED_TABLES.any { local.of(it) > 0L }
        val serverWroteSomething = COMPARED_TABLES.any { server.of(it) > 0L }
        return localWroteSomething && serverWroteSomething
    }
}
