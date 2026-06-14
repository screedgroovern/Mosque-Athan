package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.ui.settings.SettingsCard
import com.groovernapp.mosqueathan.util.Lang

@Composable
fun AdzanIqomahSettingsPane(
    adzanLeadMinutes: Int,
    onAdzanLeadMinutesChange: (Int) -> Unit,
    iqomahSubuh: Int,
    onIqomahSubuhChange: (Int) -> Unit,
    iqomahDzuhur: Int,
    onIqomahDzuhurChange: (Int) -> Unit,
    iqomahAshar: Int,
    onIqomahAsharChange: (Int) -> Unit,
    iqomahMaghrib: Int,
    onIqomahMaghribChange: (Int) -> Unit,
    iqomahIsya: Int,
    onIqomahIsyaChange: (Int) -> Unit,
    config: MosqueConfigEntity,
    onUpdateFooters: (String, String) -> Unit,
    onPreviewAdzan: () -> Unit,
    onPreviewIqomah: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SettingsCard(
            title = Lang.get("Adzan", "Adzan"),
            icon = Icons.Default.Campaign
        ) {
            AdzanSettingsSegment(
                adzanLeadMinutes = adzanLeadMinutes,
                onAdzanLeadMinutesChange = onAdzanLeadMinutesChange,
                config = config,
                onUpdateFooters = onUpdateFooters,
                onPreviewAdzan = onPreviewAdzan,
                isCompact = isCompact
            )
        }

        SettingsCard(
            title = Lang.get("Iqomah", "Iqomah"),
            icon = Icons.Default.AccessTime
        ) {
            IqomahSettingsSegment(
                iqomahSubuh = iqomahSubuh,
                onIqomahSubuhChange = onIqomahSubuhChange,
                iqomahDzuhur = iqomahDzuhur,
                onIqomahDzuhurChange = onIqomahDzuhurChange,
                iqomahAshar = iqomahAshar,
                onIqomahAsharChange = onIqomahAsharChange,
                iqomahMaghrib = iqomahMaghrib,
                onIqomahMaghribChange = onIqomahMaghribChange,
                iqomahIsya = iqomahIsya,
                onIqomahIsyaChange = onIqomahIsyaChange,
                config = config,
                onUpdateFooters = onUpdateFooters,
                onPreviewIqomah = onPreviewIqomah,
                isCompact = isCompact
            )
        }
    }
}
