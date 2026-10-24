package com.groovernapp.mosqueathan.ui.settings.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.groovernapp.mosqueathan.protocol.SyncConflict
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueDangerButton
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueTextButton
import com.groovernapp.mosqueathan.ui.design.initialFocus
import com.groovernapp.mosqueathan.util.Lang
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/**
 * Asked when both the display and this device have changed settings since they last agreed.
 *
 * ### Why it names the sections rather than only the times
 *
 * A takmir remembers *what* they changed, not when. "You changed the Friday schedule; the display
 * changed the running text" is something they can act on; two timestamps alone ask them to reason
 * about clocks. The times are here as the tiebreaker the third option needs, not as the argument.
 *
 * ### Why the third option is offered but not taken automatically
 *
 * Two devices keep two clocks, and a phone that has been switched off can report a "last modified"
 * that is simply wrong. So the newest-write rule decides only when the takmir picks it, and both
 * times are on screen first so an obviously wrong one can be spotted. Never let the clock decide
 * silently.
 *
 * ### Why this is not per-field
 *
 * Choosing per field is the better answer and a much larger change; this chooses per section, which
 * already saves the common case — you edit the Friday schedule at home while someone at the mosque
 * adds a running text, and either wholesale choice throws one of them away. See [SyncConflict] for
 * what per-table resolution can and cannot do.
 */
@Composable
fun SyncConflictDialog(
    server: SyncConflict.Stamps,
    local: SyncConflict.Stamps,
    onChoice: (SyncConflict.Choice) -> Unit,
    onDismiss: () -> Unit
) {
    val differing = SyncConflict.differingTables(server, local)
    val mediaDiffers = differing.any { it in SyncConflict.COMPARED_TABLES - SyncConflict.PUSHABLE_TABLES.toSet() }

    // Everything, buttons included, sits in one scrolling column. The buttons used to be the
    // dialog's own confirm/dismiss slots, which lay a tall column of three beside a fourth: on a
    // phone held upright that wrapped into overlapping rows, and on a landscape phone it ran off the
    // bottom with nothing to scroll. In the body they stack full width at any size and scroll with
    // the text when the window is short.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                Lang.get("Settings differ", "Pengaturan berbeda"),
                color = MosqueTheme.colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
            ) {
                Text(
                    Lang.get(
                        "This device and the display have both been changed. Choose which copy to keep.",
                        "Perangkat ini dan display dua-duanya berubah. Pilih data mana yang dipakai."
                    ),
                    color = MosqueTheme.colors.textSecondary,
                    fontSize = MosqueTheme.typography.bodyCompact
                )

                differing.forEach { table ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            sectionLabel(table),
                            color = MosqueTheme.colors.textPrimary,
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            Lang.get(
                                "Display: ${moment(server.of(table))}   •   This device: ${moment(local.of(table))}",
                                "Display: ${moment(server.of(table))}   •   Perangkat ini: ${moment(local.of(table))}"
                            ),
                            color = MosqueTheme.colors.textMuted,
                            fontSize = MosqueTheme.typography.caption
                        )
                    }
                }

                if (mediaDiffers) {
                    // Said here because the choice below cannot act on these sections the way it
                    // acts on the others: picture files only exist on the display.
                    Text(
                        Lang.get(
                            "Picture files live on the display, so this device's pictures are never sent to it, whichever you choose.",
                            "File gambar ada di display, jadi gambar di perangkat ini tidak pernah dikirim ke display, apa pun pilihannya."
                        ),
                        color = MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.caption
                    )
                }

                Spacer(modifier = Modifier.height(MosqueTheme.spacing.sm))

                // The safest of the three leads and holds initial focus, because a D-pad user who
                // presses Enter without reading should not lose the mosque's own settings.
                MosquePrimaryButton(
                    modifier = Modifier.fillMaxWidth().initialFocus(),
                    onClick = { onChoice(SyncConflict.Choice.TakeServer) }
                ) {
                    Text(
                        Lang.get("Use the display's", "Pakai data display"),
                        textAlign = TextAlign.Center
                    )
                }
                MosqueSecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onChoice(SyncConflict.Choice.NewestWins) }
                ) {
                    Text(
                        Lang.get("Keep whichever is newer", "Pakai yang terbaru"),
                        textAlign = TextAlign.Center
                    )
                }
                // Danger, because this is the one that can overwrite the display the whole
                // congregation is looking at with a phone's copy.
                MosqueDangerButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onChoice(SyncConflict.Choice.TakeLocal) }
                ) {
                    Text(
                        Lang.get("Use this device's", "Pakai data perangkat ini"),
                        textAlign = TextAlign.Center
                    )
                }
                MosqueTextButton(modifier = Modifier.fillMaxWidth(), onClick = onDismiss) {
                    Text(Lang.get("Cancel", "Batal"), textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {},
        containerColor = MosqueTheme.colors.surfaceRaised
    )
}

/** A table name in the takmir's vocabulary, not the schema's. */
private fun sectionLabel(table: String): String = when (table) {
    "mosque_config" -> Lang.get("Prayer times and settings", "Waktu shalat dan pengaturan")
    "announcements" -> Lang.get("Running text", "Teks berjalan")
    "media" -> Lang.get("Slideshow media", "Media slideshow")
    "friday_media" -> Lang.get("Friday media", "Media Jum'at")
    "dashboard_backgrounds" -> Lang.get("Dashboard backgrounds", "Latar dashboard")
    "slideshow_schedules" -> Lang.get("Slideshow schedules", "Jadwal slideshow")
    "standby_schedules" -> Lang.get("Standby schedules", "Jadwal standby")
    else -> table
}

/**
 * A wall-clock moment, short enough to sit on one line beside another.
 *
 * Numeric month rather than a name: this string carries two of them side by side and the point is
 * comparing them at a glance, which a shorter string does better. Zero means the section was never
 * written, which reads as a dash rather than as 1 January 1970.
 */
private fun moment(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    val at = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val day = at.day.toString().padStart(2, '0')
    val month = at.month.number.toString().padStart(2, '0')
    val hour = at.hour.toString().padStart(2, '0')
    val minute = at.minute.toString().padStart(2, '0')
    return "$day/$month $hour:$minute"
}
