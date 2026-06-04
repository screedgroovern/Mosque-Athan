package com.groovernapp.mosqueathan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MosqueColorScheme = darkColorScheme()

@Composable
fun MosqueAthanTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MosqueColorScheme,
        content = content
    )
}