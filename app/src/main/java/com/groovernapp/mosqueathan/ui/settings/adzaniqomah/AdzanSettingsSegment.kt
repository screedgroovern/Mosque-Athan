package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

import com.groovernapp.mosqueathan.ui.settings.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun AdzanSettingsSegment(
    adzanLeadMinutes: Int,
    onAdzanLeadMinutesChange: (Int) -> Unit,
    config: MosqueConfigEntity,
    onUpdateFooters: (String, String) -> Unit,
    onPreviewAdzan: () -> Unit,
    isCompact: Boolean = false
) {
    val adzanFooters = remember(config.adzanFooterText) {
        parseFooterTexts(config.adzanFooterText)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.xl)
    ) {
        Text(
            text = Lang.get("Adzan Preparation", "Persiapan Adzan"),
            color = MosqueTheme.colors.accent,
            fontSize = if (isCompact) MosqueTheme.typography.bodyCompact else MosqueTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        AdjustmentRow(
            title = Lang.get("Adzan Preparation Duration", "Durasi Persiapan Adzan"),
            value = adzanLeadMinutes,
            minValue = 0,
            maxValue = 60,
            showSign = false,
            onValueChange = onAdzanLeadMinutesChange
        )

        HorizontalDivider(color = MosqueTheme.colors.fillStrong)

        RunningTextListSettingsComponent(
            title = Lang.get("Running Text", "Teks Berjalan"),
            subtitle = Lang.get("Adzan Screen Footer (Running Text)", "Footer Layar Adzan (Teks Berjalan)"),
            items = adzanFooters,
            onItemsChange = { updated ->
                onUpdateFooters(formatFooterTexts(updated), config.iqomahFooterText)
            },
            placeholderText = Lang.get("Type adzan footer...", "Ketik footer adzan..."),
            isCompact = isCompact
        )

        HorizontalDivider(color = MosqueTheme.colors.fillStrong)

        // Preview Button
        TvFocusablePreviewButton(
            onClick = onPreviewAdzan,
            shape = RoundedCornerShape(MosqueTheme.radius.md),
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Text(
                text = Lang.get("Preview Adzan Screen", "Preview Layar Adzan"),
                color = MosqueTheme.colors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = if (isCompact) MosqueTheme.typography.labelSmall else MosqueTheme.typography.label
            )
        }
    }
}
