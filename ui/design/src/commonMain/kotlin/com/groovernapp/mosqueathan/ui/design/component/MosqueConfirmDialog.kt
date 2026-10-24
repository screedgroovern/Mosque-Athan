package com.groovernapp.mosqueathan.ui.design.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.initialFocus
import com.groovernapp.mosqueathan.util.Lang

/**
 * A yes/no question before something that cannot be taken back, or that cuts a connection.
 *
 * Takes [initialFocus] on the confirm action like every dialog here, so a D-pad has somewhere to
 * start. [destructive] swaps the gold confirm button for the red one, which is what a delete wants:
 * the colour is the last thing a person sees before pressing Enter.
 */
@Composable
fun MosqueConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, color = MosqueTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(text = message, color = MosqueTheme.colors.textPrimary)
        },
        confirmButton = {
            if (destructive) {
                MosqueDangerButton(onClick = onConfirm, modifier = Modifier.initialFocus()) {
                    Text(confirmText, fontWeight = FontWeight.Bold)
                }
            } else {
                MosquePrimaryButton(onClick = onConfirm, modifier = Modifier.initialFocus()) {
                    Text(confirmText, color = MosqueTheme.colors.onAccent, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            MosqueTextButton(onClick = onDismiss) {
                Text(Lang.get("Cancel", "Batal"), color = MosqueTheme.colors.textPrimary)
            }
        },
        containerColor = MosqueTheme.colors.surface,
        shape = RoundedCornerShape(MosqueTheme.radius.lg)
    )
}
