package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector? = null,
    dimmed: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MosqueTheme.colors.onAccent.copy(alpha = if (dimmed) 0.15f else 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MosqueTheme.colors.textPrimary.copy(alpha = if (dimmed) 0.05f else 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MosqueTheme.colors.accent.copy(alpha = if (dimmed) 0.4f else 1f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = title,
                    color = MosqueTheme.colors.accent.copy(alpha = if (dimmed) 0.4f else 1f),
                    fontSize = MosqueTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}
