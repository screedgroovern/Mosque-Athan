package com.groovernapp.mosqueathan.ui.design.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

/**
 * A titled section on a settings pane: heading, optional icon, and its content in a
 * bordered card.
 *
 * [dimmed] is for a section whose controls are inactive because a switch above it is
 * off — it fades the heading and lightens the card rather than hiding the section,
 * so the setting stays discoverable.
 */
@Composable
fun MosqueSectionCard(
    title: String,
    icon: ImageVector? = null,
    dimmed: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MosqueTheme.colors.scrim.copy(alpha = if (dimmed) 0.15f else 0.35f)
        ),
        shape = RoundedCornerShape(MosqueTheme.radius.lg),
        border = BorderStroke(
            MosqueTheme.stroke.hairline,
            MosqueTheme.colors.textPrimary.copy(alpha = if (dimmed) 0.05f else 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(MosqueTheme.spacing.xxxl)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.md),
                modifier = Modifier.padding(bottom = MosqueTheme.spacing.xl)
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
