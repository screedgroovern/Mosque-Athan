package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import com.groovernapp.mosqueathan.ui.design.isRunning
import com.groovernapp.mosqueathan.domain.PreviewKind
import com.groovernapp.mosqueathan.util.footerLines
import com.groovernapp.mosqueathan.util.encodeFooterLines
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.domain.MosqueConfig
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.ui.settings.RunningTextListSettingsComponent
import com.groovernapp.mosqueathan.ui.settings.StopPreviewButton
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.LocalWindowClass
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.MosquePreviewButton

@Composable
fun IqomahSettingsSegment(
    state: AdzanIqomahUiState,
    onEvent: (AdzanIqomahEvent) -> Unit
) {
    val isCompact = LocalWindowClass.current.isCompact
    val onUpdateFooters: (String, String) -> Unit =
        { adzan, iqomah -> onEvent(AdzanIqomahEvent.FootersChanged(adzan, iqomah)) }
    val onPreviewIqomah: () -> Unit = { onEvent(AdzanIqomahEvent.PreviewIqomahRequested) }
    val iqomahFooters = remember(state.iqomahFooterText) {
        footerLines(state.iqomahFooterText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        // Subtitle: Iqomah wait time after adzan
        Text(
            text = Lang.get("Iqomah wait time after adzan", "Jeda waktu iqomah setelah adzan"),
            color = MosqueTheme.colors.accent,
            fontSize = if (isCompact) MosqueTheme.typography.bodyCompact else MosqueTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // One row per prayer, from IQOMAH_PRAYERS. This was five near-identical
        // lines; the label comes from PrayerType.displayName, which already knows
        // both languages and renders Friday's Dzuhur as JUM'AT.
        Column(verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md)) {
            IQOMAH_PRAYERS.forEach { prayer ->
                AdjustmentRow(
                    title = prayer.displayName,
                    value = state.iqomahFor(prayer),
                    minValue = 0,
                    maxValue = 60,
                    showSign = false,
                    onValueChange = { onEvent(AdzanIqomahEvent.IqomahMinutesChanged(prayer, it)) }
                )
            }
        }

        HorizontalDivider(
            Modifier,
            DividerDefaults.Thickness,
            color = MosqueTheme.colors.fillStrong
        )

        RunningTextListSettingsComponent(
            title = Lang.get("Running Text", "Teks Berjalan"),
            subtitle = Lang.get("Iqomah Screen Footer (Running Text)", "Footer Layar Iqomah (Teks Berjalan)"),
            items = iqomahFooters,
            onItemsChange = { updated ->
                onUpdateFooters(state.adzanFooterText, encodeFooterLines(updated))
            },
            placeholderText = Lang.get("Type iqomah footer...", "Ketik footer iqomah..."),
            isCompact = isCompact
        )

        HorizontalDivider(
            Modifier,
            DividerDefaults.Thickness,
            color = MosqueTheme.colors.fillStrong
        )

        // Preview Button, which becomes Stop while this preview is on the display
        if (PreviewKind.Iqomah.isRunning) {
            StopPreviewButton(
                onClick = onPreviewIqomah,
                modifier = Modifier.fillMaxWidth(),
                fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.label
            )
        } else MosquePreviewButton(
            onClick = onPreviewIqomah,
            shape = RoundedCornerShape(MosqueTheme.radius.md),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Text(
                text = Lang.get("Preview Iqomah Screen", "Preview Layar Iqomah"),
                color = MosqueTheme.colors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.label
            )
        }
    }
}
