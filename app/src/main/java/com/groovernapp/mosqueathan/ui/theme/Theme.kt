package com.groovernapp.mosqueathan.ui.theme

import androidx.compose.runtime.Composable
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

/**
 * Kept as a thin delegate so the ten existing call sites do not have to change in the
 * same commit that introduces the design tokens. Call [MosqueTheme] directly in new
 * code; this alias goes away once the last caller is migrated.
 */
@Composable
fun MosqueAthanTheme(
    content: @Composable () -> Unit
) {
    MosqueTheme(content = content)
}
