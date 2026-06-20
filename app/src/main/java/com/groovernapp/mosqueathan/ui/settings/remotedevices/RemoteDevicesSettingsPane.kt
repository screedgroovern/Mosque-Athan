package com.groovernapp.mosqueathan.ui.settings.remotedevices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.ConnectedDeviceEntity
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.ui.settings.SettingsCard
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

@Composable
fun RemoteDevicesSettingsPane(
    config: MosqueConfigEntity,
    isTv: Boolean,
    onUpdateShowSettingsOnTv: (Boolean) -> Unit,
    devices: List<ConnectedDeviceEntity>,
    onSetDeviceMaster: (String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = Color(0xFFFFD54F),
            divider = {}
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Server Web Remote",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Web Control Server", "Server Web Remote"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = Color(0xFFFFD54F),
                unselectedContentColor = Color.White.copy(alpha = 0.6f)
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Perangkat Terhubung",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Lang.get("Connected Devices", "Perangkat Terhubung"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = Color(0xFFFFD54F),
                unselectedContentColor = Color.White.copy(alpha = 0.6f)
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            if (page == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsCard(
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
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SettingsCard(
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
}
