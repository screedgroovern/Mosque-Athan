@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun AdzanIqomahSettingsPane(
    state: AdzanIqomahUiState,
    onEvent: (AdzanIqomahEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalWindowClass.current.isCompact
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MosqueTheme.colors.transparent,
            contentColor = MosqueTheme.colors.accent,
            divider = {}
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Adzan",
                            modifier = Modifier.size(MosqueTheme.size.iconMd)
                        )
                        Text(
                            text = Lang.get("Adzan", "Adzan"),
                            fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Iqomah",
                            modifier = Modifier.size(MosqueTheme.size.iconMd)
                        )
                        Text(
                            text = Lang.get("Iqomah", "Iqomah"),
                            fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.bodyCompact,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                selectedContentColor = MosqueTheme.colors.accent,
                unselectedContentColor = MosqueTheme.colors.textSecondary
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            if (page == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(MosqueTheme.radius.lg),
                    border = BorderStroke(MosqueTheme.stroke.hairline, MosqueTheme.colors.fill)
                ) {
                    Column(modifier = Modifier.padding(MosqueTheme.spacing.xxxl)) {
                        AdzanSettingsSegment(state = state, onEvent = onEvent)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MosqueTheme.colors.scrim.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(MosqueTheme.radius.lg),
                    border = BorderStroke(MosqueTheme.stroke.hairline, MosqueTheme.colors.fill)
                ) {
                    Column(modifier = Modifier.padding(MosqueTheme.spacing.xxxl)) {
                        IqomahSettingsSegment(state = state, onEvent = onEvent)
                    }
                }
            }
        }
    }
}
