package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueIconButton
import com.groovernapp.mosqueathan.ui.design.component.MosqueSecondaryButton
import com.groovernapp.mosqueathan.util.Lang

/**
 * The displays this device has connected to, each with whether it is on right now.
 *
 * A takmir who looks after more than one mosque, or a display and a spare, switches between them
 * here instead of retyping an address. The dot is a probe made while this pane is open, not a
 * promise: a display that was on a moment ago may be off by the time a button is pressed, and
 * connecting says so if it is.
 *
 * **Two layouts, chosen by window class.** Upright on a phone there is no room for the name, a
 * Connect button and a delete button on one line without squeezing the name to a few letters, so
 * the buttons drop to their own line. Anywhere wider they share the row.
 */
@Composable
fun KnownServersSection(
    servers: List<ServerEntry>,
    currentHost: String,
    onConnect: (String) -> Unit,
    onForget: (String) -> Unit
) {
    if (servers.isEmpty()) return
    val isCompact = LocalWindowClass.current.isCompact

    Column(
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = Lang.get("Saved displays", "Display tersimpan"),
            color = MosqueTheme.colors.accent,
            fontSize = MosqueTheme.typography.body,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = Lang.get(
                "Displays this device has connected to. The dot shows whether each one is on right now.",
                "Display yang pernah terhubung dengan perangkat ini. Titik menunjukkan apakah display sedang menyala."
            ),
            color = MosqueTheme.colors.textSecondary,
            fontSize = MosqueTheme.typography.labelSmall
        )

        Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
            servers.forEach { entry ->
                ServerRow(
                    entry = entry,
                    isCurrent = entry.server.host == currentHost,
                    isCompact = isCompact,
                    onConnect = { onConnect(entry.server.host) },
                    onForget = { onForget(entry.server.host) }
                )
            }
        }
    }
}

@Composable
private fun ServerRow(
    entry: ServerEntry,
    isCurrent: Boolean,
    isCompact: Boolean,
    onConnect: () -> Unit,
    onForget: () -> Unit
) {
    val server = entry.server
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(MosqueTheme.radius.md))
        .background(MosqueTheme.colors.fillSubtle)
        .padding(horizontal = MosqueTheme.spacing.lg, vertical = MosqueTheme.spacing.md)

    val identity: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            StatusDot(entry.online)
            Column {
                Text(
                    text = server.name.ifBlank { server.host },
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.bodyCompact,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = listOfNotNull(
                        server.host.takeIf { server.name.isNotBlank() },
                        statusLabel(entry.online),
                        Lang.get("Current", "Saat ini").takeIf { isCurrent }
                    ).joinToString("  •  "),
                    color = if (isCurrent) MosqueTheme.colors.accent else MosqueTheme.colors.textMuted,
                    fontSize = MosqueTheme.typography.caption
                )
            }
        }
    }
    val actions: @Composable () -> Unit = {
        MosqueSecondaryButton(
            onClick = onConnect,
            shape = RoundedCornerShape(MosqueTheme.radius.sm),
            modifier = Modifier.heightIn(min = MosqueTheme.size.controlMd)
        ) {
            Text(
                text = Lang.get("Connect", "Hubungkan"),
                fontWeight = FontWeight.Bold,
                fontSize = MosqueTheme.typography.label
            )
        }
        MosqueIconButton(onClick = onForget, modifier = Modifier.size(MosqueTheme.size.controlMd)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = Lang.get("Forget this display", "Lupakan display ini"),
                tint = MosqueTheme.colors.danger
            )
        }
    }

    if (isCompact) {
        Column(modifier = rowModifier, verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
            identity(Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) { actions() }
        }
    } else {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            identity(Modifier.weight(1f))
            actions()
        }
    }
}

/** Green when on, grey when off, and a quiet blue until the first answer arrives. */
@Composable
private fun StatusDot(online: Boolean?) {
    val color: Color = when (online) {
        true -> MosqueTheme.colors.success
        false -> MosqueTheme.colors.textDisabled
        null -> MosqueTheme.colors.info
    }
    Box(modifier = Modifier.size(MosqueTheme.size.iconSm / 2).clip(CircleShape).background(color))
}

@Composable
private fun statusLabel(online: Boolean?): String = when (online) {
    true -> Lang.get("Online", "Online")
    false -> Lang.get("Offline", "Offline")
    null -> Lang.get("Checking…", "Memeriksa…")
}
