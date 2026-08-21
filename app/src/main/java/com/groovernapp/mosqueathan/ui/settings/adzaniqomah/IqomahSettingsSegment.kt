package com.groovernapp.mosqueathan.ui.settings.adzaniqomah

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
import com.groovernapp.mosqueathan.data.MosqueConfigEntity
import com.groovernapp.mosqueathan.ui.settings.AdjustmentRow
import com.groovernapp.mosqueathan.ui.settings.TvFocusablePreviewButton
import com.groovernapp.mosqueathan.ui.settings.TvFocusablePrimaryButton
import com.groovernapp.mosqueathan.ui.settings.formatFooterTexts
import com.groovernapp.mosqueathan.ui.settings.parseFooterTexts
import com.groovernapp.mosqueathan.ui.settings.textFieldColors
import com.groovernapp.mosqueathan.ui.settings.RunningTextListSettingsComponent
import com.groovernapp.mosqueathan.util.Lang
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun IqomahSettingsSegment(
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
    onPreviewIqomah: () -> Unit,
    isCompact: Boolean = false
) {
    val iqomahFooters = remember(config.iqomahFooterText) {
        parseFooterTexts(config.iqomahFooterText)
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

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AdjustmentRow(Lang.get("Fajr", "Subuh"), iqomahSubuh, minValue = 0, maxValue = 60, showSign = false, onValueChange = onIqomahSubuhChange)
            AdjustmentRow(Lang.get("Dhuhr", "Dzuhur"), iqomahDzuhur, minValue = 0, maxValue = 60, showSign = false, onValueChange = onIqomahDzuhurChange)
            AdjustmentRow(Lang.get("Asr", "Ashar"), iqomahAshar, minValue = 0, maxValue = 60, showSign = false, onValueChange = onIqomahAsharChange)
            AdjustmentRow(Lang.get("Maghrib", "Maghrib"), iqomahMaghrib, minValue = 0, maxValue = 60, showSign = false, onValueChange = onIqomahMaghribChange)
            AdjustmentRow(Lang.get("Isha'", "Isya'"), iqomahIsya, minValue = 0, maxValue = 60, showSign = false, onValueChange = onIqomahIsyaChange)
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
                onUpdateFooters(config.adzanFooterText, formatFooterTexts(updated))
            },
            placeholderText = Lang.get("Type iqomah footer...", "Ketik footer iqomah..."),
            isCompact = isCompact
        )

        HorizontalDivider(
            Modifier,
            DividerDefaults.Thickness,
            color = MosqueTheme.colors.fillStrong
        )

        // Preview Button
        TvFocusablePreviewButton(
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
