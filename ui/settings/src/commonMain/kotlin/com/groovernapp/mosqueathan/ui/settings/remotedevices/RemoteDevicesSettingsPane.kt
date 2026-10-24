@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.LocalCanServe
import com.groovernapp.mosqueathan.ui.design.WindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueConfirmDialog
import com.groovernapp.mosqueathan.ui.design.component.MosqueSectionCard
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.panePadding
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

/**
 * Remote & Devices.
 *
 * The first pane restructured to `state` + `onEvent`. It previously took eleven
 * parameters — a whole `MosqueConfig` plus nine callbacks plus `isCompact` —
 * and the whole category now reads from one state and reports through one channel.
 *
 * Screen size comes from [LocalWindowClass] rather than a threaded `isCompact`
 * boolean, so all four classes are handled instead of two.
 */
@Composable
fun RemoteDevicesSettingsPane(
    state: RemoteDevicesUiState,
    onEvent: (RemoteDevicesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var pendingMode by remember { mutableStateOf<ConnectivityMode?>(null) }
    val windowClass = LocalWindowClass.current
    val canServe = LocalCanServe.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // A host that cannot serve is a controller whatever the stored mode says, and has no choice to
    // make: the selector is not drawn there rather than drawn with one option that does nothing.
    val isServer = canServe && state.isServer

    pendingMode?.let { target ->
        ModeChangeDialog(
            state = state,
            onConfirm = {
                onEvent(RemoteDevicesEvent.ModeSelected(target))
                pendingMode = null
            },
            onDismiss = { pendingMode = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // The pane scrolls on D-pad up/down. Without this a remote cannot reach
            // anything below the fold, because nothing inside is focusable enough to
            // pull the scroll along.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionDown -> scrollBy(scope, scrollState, STEP)
                    Key.DirectionUp -> scrollBy(scope, scrollState, -STEP)
                    else -> false
                }
            }
            .focusable()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg)
    ) {
        ModeHeader(
            isServer = isServer,
            showSwitch = canServe,
            compact = windowClass == WindowClass.Compact,
            onSelect = { mode ->
                when {
                    mode == state.mode -> Unit
                    // Only ask when something live would be dropped; see the state's property.
                    state.switchingModeDropsConnection -> pendingMode = mode
                    else -> onEvent(RemoteDevicesEvent.ModeSelected(mode))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = windowClass.panePadding)
        )

        // The tabs split a server's two jobs. A remote has one, so they are gone and its card
        // follows the header directly -- a tab row over a single tab is just a gap.
        if (isServer) {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MosqueTheme.colors.transparent,
                contentColor = MosqueTheme.colors.accent,
                divider = {}
            ) {
                CategoryTab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = Icons.Default.Dns,
                    label = Lang.get("Web Control Server", "Server Web Remote")
                )
                CategoryTab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = Icons.Default.Devices,
                    label = Lang.get("Connected Devices", "Perangkat Terhubung")
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MosqueTheme.spacing.xxxl)
        ) {
            when {
                !isServer -> MosqueSectionCard(
                    title = Lang.get("Sync", "Sinkronisasi"),
                    icon = Icons.Default.Sync
                ) {
                    RemoteControlSettingsSegment(state = state, onEvent = onEvent)
                }

                selectedTabIndex == 1 -> MosqueSectionCard(
                    title = Lang.get("Connected Devices", "Perangkat Terhubung"),
                    icon = Icons.Default.Devices
                ) {
                    DevicesSettingsSegment(state = state, onEvent = onEvent)
                }

                else -> MosqueSectionCard(
                    title = Lang.get("Web Control Server", "Server Web Remote"),
                    icon = Icons.Default.Dns
                ) {
                    WebControlSettingsSegment(state = state, onEvent = onEvent)
                }
            }
        }
    }
}

/**
 * What this device is, in a sentence, with the mode switch at its top-right.
 *
 * This used to be two full-size cards stacked above the sentence, which pushed a server's QR code
 * below the fold on a phone and on the television. The switch is now a small control beside the
 * text where there is room, and above it on a phone, where a row would leave the sentence too narrow.
 */
@Composable
private fun ModeHeader(
    isServer: Boolean,
    showSwitch: Boolean,
    compact: Boolean,
    onSelect: (ConnectivityMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val description: @Composable (Modifier) -> Unit = { textModifier ->
        Text(
            text = if (isServer) {
                Lang.get(
                    "This device is the display: it shows the prayer schedule, adzan, iqomah and media. Other devices can connect to it as remote controls.",
                    "Aplikasi berfungsi sebagai server utama yang menampilkan jadwal shalat, adzan, iqomah, dan media di TV / layar utama. Perangkat lain dapat terhubung sebagai remote kontrol."
                )
            } else {
                Lang.get(
                    "This device is a remote control: it changes settings and syncs them with the main MosqueAthan display.",
                    "Aplikasi berfungsi sebagai remote kontrol untuk mengendalikan, mengubah pengaturan, dan menyinkronkan data dengan TV / server MosqueAthan utama."
                )
            },
            color = MosqueTheme.colors.textSecondary,
            fontSize = MosqueTheme.typography.labelSmall,
            modifier = textModifier
        )
    }
    val mode = if (isServer) ConnectivityMode.Server else ConnectivityMode.Remote

    when {
        !showSwitch -> description(modifier)

        compact -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
            ModeSwitch(selected = mode, onSelect = onSelect, modifier = Modifier.align(Alignment.End))
            description(Modifier.fillMaxWidth())
        }

        else -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl),
            verticalAlignment = Alignment.Top
        ) {
            description(Modifier.weight(1f))
            ModeSwitch(selected = mode, onSelect = onSelect)
        }
    }
}

/** Two segments in one outline: a choice of equal standing, small enough to sit in a corner. */
@Composable
private fun ModeSwitch(
    selected: ConnectivityMode,
    onSelect: (ConnectivityMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(MosqueTheme.radius.md)
    Row(
        modifier = modifier
            .clip(shape)
            .border(width = MosqueTheme.stroke.thin, color = MosqueTheme.colors.fillStrong, shape = shape)
    ) {
        ConnectivityMode.entries.forEach { mode ->
            ModeSegment(mode = mode, selected = mode == selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun ModeSegment(
    mode: ConnectivityMode,
    selected: Boolean,
    onSelect: (ConnectivityMode) -> Unit
) {
    val tint = if (selected) MosqueTheme.colors.accent else MosqueTheme.colors.textSecondary

    Row(
        modifier = Modifier
            // heightIn, not height: a label that wraps must grow the segment, not be clipped.
            .heightIn(min = MosqueTheme.size.controlMd)
            .background(
                if (selected) MosqueTheme.colors.accent.copy(alpha = 0.25f)
                else MosqueTheme.colors.transparent
            )
            .focusHighlight(shape = RoundedCornerShape(MosqueRadius.md), scaleUp = false)
            .clickable { onSelect(mode) }
            .padding(horizontal = MosqueTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.sm)
    ) {
        Icon(
            imageVector = when (mode) {
                ConnectivityMode.Server -> Icons.Default.Dns
                ConnectivityMode.Remote -> Icons.Default.Devices
            },
            contentDescription = null,
            modifier = Modifier.size(MosqueTheme.size.iconSm),
            tint = tint
        )
        Text(
            text = when (mode) {
                ConnectivityMode.Server -> Lang.get("Server", "Server")
                ConnectivityMode.Remote -> Lang.get("Remote", "Remote")
            },
            color = tint,
            fontSize = MosqueTheme.typography.label,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Says what the switch will cut, in the words of the direction being left. */
@Composable
private fun ModeChangeDialog(
    state: RemoteDevicesUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state.isServer) {
        MosqueConfirmDialog(
            title = Lang.get("Switch to Remote Control mode?", "Beralih ke mode Remote Control?"),
            message = Lang.get(
                "The web control server on this device will shut down, and all ${state.controllersOnline} connected remote controls will be disconnected from it.",
                "Server kontrol web di perangkat ini akan dimatikan, dan ${state.controllersOnline} remote control yang sedang terhubung akan terputus dari server ini."
            ),
            confirmText = Lang.get("Switch", "Beralih"),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    } else {
        MosqueConfirmDialog(
            title = Lang.get("Switch to Server mode?", "Beralih ke mode Server?"),
            message = Lang.get(
                "This device will disconnect from the display at ${state.remoteTvIp} and start as its own independent server.",
                "Perangkat ini akan terputus dari server di ${state.remoteTvIp} dan berjalan sebagai server mandiri."
            ),
            confirmText = Lang.get("Switch", "Beralih"),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}

private const val STEP = 120

private fun scrollBy(
    scope: kotlinx.coroutines.CoroutineScope,
    scrollState: androidx.compose.foundation.ScrollState,
    delta: Int
): Boolean {
    val target = scrollState.value + delta
    if (delta > 0 && scrollState.value >= scrollState.maxValue) return false
    if (delta < 0 && scrollState.value <= 0) return false
    scope.launch { scrollState.animateScrollTo(target.coerceIn(0, scrollState.maxValue)) }
    return true
}

@Composable
private fun CategoryTab(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    Tab(
        selected = selected,
        onClick = onClick,
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(MosqueTheme.size.iconMd)
                )
                Text(text = label, fontSize = MosqueTheme.typography.bodyCompact, fontWeight = FontWeight.Bold)
            }
        },
        selectedContentColor = MosqueTheme.colors.accent,
        unselectedContentColor = MosqueTheme.colors.textSecondary
    )
}
