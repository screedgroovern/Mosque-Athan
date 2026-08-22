package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.groovernapp.mosqueathan.ui.design.LocalWindowMetrics
import com.groovernapp.mosqueathan.ui.design.isShort
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueSectionCard
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.panePadding
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

/**
 * Remote & Devices.
 *
 * The first pane restructured to `state` + `onEvent`. It previously took eleven
 * parameters — a whole `MosqueConfigEntity` plus nine callbacks plus `isCompact` —
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
    val windowClass = LocalWindowClass.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

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
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        ModeSelector(
            state = state,
            onEvent = onEvent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = windowClass.panePadding)
        )

        Text(
            text = if (state.isServer) {
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
            modifier = Modifier.padding(horizontal = windowClass.panePadding)
        )

        Spacer(modifier = Modifier.height(MosqueTheme.spacing.md))

        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MosqueTheme.colors.transparent,
            contentColor = MosqueTheme.colors.accent,
            divider = {}
        ) {
            CategoryTab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                icon = if (state.isServer) Icons.Default.Dns else Icons.Default.Sync,
                label = if (state.isServer) {
                    Lang.get("Web Control Server", "Server Web Remote")
                } else {
                    Lang.get("Sync", "Sinkronisasi")
                }
            )
            CategoryTab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = Icons.Default.Devices,
                label = Lang.get("Connected Devices", "Perangkat Terhubung")
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MosqueTheme.spacing.xxxl)
        ) {
            when {
                selectedTabIndex == 1 -> MosqueSectionCard(
                    title = Lang.get("Connected Devices", "Perangkat Terhubung"),
                    icon = Icons.Default.Devices
                ) {
                    DevicesSettingsSegment(state = state, onEvent = onEvent)
                }

                state.isServer -> MosqueSectionCard(
                    title = Lang.get("Web Control Server", "Server Web Remote"),
                    icon = Icons.Default.Dns
                ) {
                    WebControlSettingsSegment(state = state, onEvent = onEvent)
                }

                else -> MosqueSectionCard(
                    title = Lang.get("Sync", "Sinkronisasi"),
                    icon = Icons.Default.Sync
                ) {
                    RemoteControlSettingsSegment(state = state, onEvent = onEvent)
                }
            }
        }
    }
}

/**
 * The two mode cards.
 *
 * They sit side by side wherever there is room and stack on a phone, where two
 * cards in a row leaves each too narrow to read.
 */
@Composable
private fun ModeSelector(
    state: RemoteDevicesUiState,
    onEvent: (RemoteDevicesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val gap = Arrangement.spacedBy(MosqueTheme.spacing.xl)

    if (LocalWindowMetrics.current.isShort) {
        Column(modifier = modifier, verticalArrangement = gap) {
            ModeCard(ConnectivityMode.Server, state, onEvent, Modifier.fillMaxWidth())
            ModeCard(ConnectivityMode.Remote, state, onEvent, Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = gap) {
            ModeCard(ConnectivityMode.Server, state, onEvent, Modifier.weight(1f))
            ModeCard(ConnectivityMode.Remote, state, onEvent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeCard(
    mode: ConnectivityMode,
    state: RemoteDevicesUiState,
    onEvent: (RemoteDevicesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = state.mode == mode
    val tint = if (selected) MosqueTheme.colors.accent else MosqueTheme.colors.textSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(MosqueTheme.radius.lg))
            .background(
                if (selected) MosqueTheme.colors.accent.copy(alpha = 0.25f)
                else MosqueTheme.colors.transparent
            )
            .border(
                width = MosqueTheme.stroke.thin,
                color = if (selected) MosqueTheme.colors.accent else MosqueTheme.colors.fillStrong,
                shape = RoundedCornerShape(MosqueTheme.radius.lg)
            )
            .focusHighlight(shape = RoundedCornerShape(MosqueRadius.md), scaleUp = false)
            .clickable { onEvent(RemoteDevicesEvent.ModeSelected(mode)) }
            .padding(vertical = MosqueTheme.spacing.xl),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
        ) {
            Icon(
                imageVector = when (mode) {
                    ConnectivityMode.Server -> Icons.Default.Dns
                    ConnectivityMode.Remote -> Icons.Default.Devices
                },
                contentDescription = null,
                modifier = Modifier.size(MosqueTheme.size.controlMd),
                tint = tint
            )
            Text(
                text = when (mode) {
                    ConnectivityMode.Server -> Lang.get("Server Mode", "Mode Server")
                    ConnectivityMode.Remote -> Lang.get("Remote Control Mode", "Mode Remote Control")
                },
                color = tint,
                fontSize = MosqueTheme.typography.label,
                fontWeight = FontWeight.Bold
            )
        }
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
