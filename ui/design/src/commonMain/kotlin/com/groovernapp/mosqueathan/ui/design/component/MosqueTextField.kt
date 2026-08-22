package com.groovernapp.mosqueathan.ui.design.component

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

/** Colours for every `OutlinedTextField` in the app. */
@Composable
fun mosqueTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MosqueTheme.colors.textPrimary,
    unfocusedTextColor = MosqueTheme.colors.textPrimary,
    focusedBorderColor = MosqueTheme.colors.accent,
    unfocusedBorderColor = MosqueTheme.colors.border,
    focusedLabelColor = MosqueTheme.colors.accent,
    unfocusedLabelColor = MosqueTheme.colors.textSecondary,
    cursorColor = MosqueTheme.colors.accent,
    disabledTextColor = MosqueTheme.colors.textDisabled,
    disabledBorderColor = MosqueTheme.colors.fill,
    disabledLabelColor = MosqueTheme.colors.textDisabled,
    disabledLeadingIconColor = MosqueTheme.colors.textDisabled,
    disabledTrailingIconColor = MosqueTheme.colors.textDisabled
)
