package com.groovernapp.mosqueathan.ui.settings

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.component.MosqueDangerButton
import com.groovernapp.mosqueathan.util.Lang

/**
 * What a Preview button turns into while its preview is on the display.
 *
 * Four panes each have a Preview button and all four need the same second state, so it is written
 * once. Red, because the next press ends something rather than starting it.
 */
@Composable
fun StopPreviewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MosqueTheme.radius.md),
    fontSize: TextUnit = MosqueTheme.typography.bodyCompact
) {
    MosqueDangerButton(
        onClick = onClick,
        shape = shape,
        modifier = modifier.heightIn(min = MosqueTheme.size.controlLg)
    ) {
        Text(
            text = Lang.get("Stop Preview", "Hentikan Preview"),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    }
}
