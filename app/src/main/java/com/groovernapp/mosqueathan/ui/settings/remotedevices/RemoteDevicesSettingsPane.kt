package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.focusHighlight
import com.groovernapp.mosqueathan.ui.design.MosqueRadius
import com.groovernapp.mosqueathan.ui.design.component.MosqueSectionCard

@Composable
fun RemoteDevicesSettingsPane(
    config: MosqueConfigEntity,
    isTv: Boolean,
    onUpdateShowSettingsOnTv: (Boolean) -> Unit,
    devices: List<ConnectedDeviceEntity>,
    onSetDeviceMaster: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onUpdateConnectivityMode: (String) -> Unit,
    onUpdateRemoteTvIp: (String) -> Unit,
    onUpdatePinCode: (String) -> Unit,
    onSyncWithTv: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val isServer = config.connectivityMode == "SERVER"
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionDown -> {
                            if (scrollState.value < scrollState.maxValue) {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.value + 120)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        Key.DirectionUp -> {
                            if (scrollState.value > 0) {
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.value - 120)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .focusable()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        // Mode Selector Toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isCompact) 16.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
        ) {
            // Mode Server Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(MosqueTheme.radius.lg))
                    .background(
                        if (isServer) MosqueTheme.colors.accent.copy(alpha = 0.25f)
                        else MosqueTheme.colors.transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (isServer) MosqueTheme.colors.accent else MosqueTheme.colors.fillStrong,
                        shape = RoundedCornerShape(MosqueTheme.radius.lg)
                    )
                    .focusHighlight(shape = RoundedCornerShape(MosqueRadius.md), scaleUp = false)
                    .clickable { onUpdateConnectivityMode("SERVER") }
                    .padding(vertical = MosqueTheme.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = if (isServer) MosqueTheme.colors.accent else MosqueTheme.colors.textSecondary
                    )
                    Text(
                        text = Lang.get("Mode Server", "Mode Server"),
                        color = if (isServer) MosqueTheme.colors.accent else MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.label,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mode Remote Control Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(MosqueTheme.radius.lg))
                    .background(
                        if (!isServer) MosqueTheme.colors.accent.copy(alpha = 0.25f)
                        else MosqueTheme.colors.transparent
                    )
                    .border(
                        width = 2.dp,
                        color = if (!isServer) MosqueTheme.colors.accent else MosqueTheme.colors.fillStrong,
                        shape = RoundedCornerShape(MosqueTheme.radius.lg)
                    )
                    .focusHighlight(shape = RoundedCornerShape(MosqueRadius.md), scaleUp = false)
                    .clickable { onUpdateConnectivityMode("REMOTE") }
                    .padding(vertical = MosqueTheme.spacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = if (!isServer) MosqueTheme.colors.accent else MosqueTheme.colors.textSecondary
                    )
                    Text(
                        text = Lang.get("Mode Remote Control", "Mode Remote Control"),
                        color = if (!isServer) MosqueTheme.colors.accent else MosqueTheme.colors.textSecondary,
                        fontSize = MosqueTheme.typography.label,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Mode Explanation Text
        Text(
            text = if (isServer) {
                Lang.get(
                    "Aplikasi berfungsi sebagai server utama yang menampilkan jadwal shalat, adzan, iqomah, dan media di TV / layar utama. Perangkat lain dapat terhubung sebagai remote kontrol.",
                    "Aplikasi berfungsi sebagai server utama yang menampilkan jadwal shalat, adzan, iqomah, dan media di TV / layar utama. Perangkat lain dapat terhubung sebagai remote kontrol."
                )
            } else {
                Lang.get(
                    "Aplikasi berfungsi sebagai remote kontrol untuk mengendalikan, mengubah pengaturan, dan menyinkronkan data dengan TV / server MosqueAthan utama.",
                    "Aplikasi berfungsi sebagai remote kontrol untuk mengendalikan, mengubah pengaturan, dan menyinkronkan data dengan TV / server MosqueAthan utama."
                )
            },
            color = MosqueTheme.colors.textSecondary,
            fontSize = MosqueTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = if (isCompact) 16.dp else 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MosqueTheme.colors.transparent,
            contentColor = MosqueTheme.colors.accent,
            divider = {}
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = if (isServer) Icons.Default.Dns else Icons.Default.Sync,
                            contentDescription = if (isServer) "Server Web Remote" else "Sinkronisasi",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isServer) Lang.get("Web Control Server", "Server Web Remote") else Lang.get("Sinkronisasi", "Sinkronisasi"),
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Perangkat Terhubung",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Connected Devices", "Perangkat Terhubung"),
                            fontSize = MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MosqueTheme.spacing.xxxl)
        ) {
            if (selectedTabIndex == 0) {
                if (isServer) {
                    MosqueSectionCard(
                        title = Lang.get("Web Control Server", "Server Web Remote"),
                        icon = Icons.Default.Dns
                    ) {
                        WebControlSettingsSegment(
                            showSettingsOnTv = config.showSettingsOnTv,
                            isTv = isTv,
                            onUpdateShowSettingsOnTv = onUpdateShowSettingsOnTv,
                            isCompact = isCompact
                        )
                    }
                } else {
                    MosqueSectionCard(
                        title = Lang.get("Sinkronisasi", "Sinkronisasi"),
                        icon = Icons.Default.Sync
                    ) {
                        RemoteControlSettingsSegment(
                            config = config,
                            onUpdateRemoteTvIp = onUpdateRemoteTvIp,
                            onUpdatePinCode = onUpdatePinCode,
                            onSyncWithTv = onSyncWithTv,
                            isCompact = isCompact
                        )
                    }
                }
            } else {
                MosqueSectionCard(
                    title = Lang.get("Connected Devices", "Perangkat Terhubung"),
                    icon = Icons.Default.Devices
                ) {
                    DevicesSettingsSegment(
                        config = config,
                        devices = devices,
                        onSetDeviceMaster = onSetDeviceMaster,
                        onDeleteDevice = onDeleteDevice
                    )
                }
            }
        }
    }
}
