package com.groovernapp.mosqueathan.ui.settings.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovernapp.mosqueathan.ui.design.DisplayApproval
import com.groovernapp.mosqueathan.ui.design.MosqueTheme
import com.groovernapp.mosqueathan.ui.design.PinResult
import com.groovernapp.mosqueathan.ui.design.component.MosquePrimaryButton
import com.groovernapp.mosqueathan.ui.design.component.mosqueTextFieldColors
import com.groovernapp.mosqueathan.ui.design.initialFocus
import com.groovernapp.mosqueathan.util.Lang
import kotlinx.coroutines.launch

/**
 * Covers the settings UI until the display has let this controller in.
 *
 * ### Why it blocks rather than asks politely
 *
 * A device the display has not allowed cannot read *or* write: the display withholds its state and
 * refuses every action, so a dismissible prompt would leave a takmir looking at a panel full of
 * default values that silently discards everything they touch. The hand-written panel got this
 * right and this matches it deliberately — a full-screen "Akses Terkunci" with one way forward.
 * Preserving that is the point: step 8 deletes the old panel, and until this existed the new one
 * had no PIN entry at all, so deleting it would have removed the only way into a PIN-protected
 * display.
 *
 * ### Why the field holds initial focus and not the button
 *
 * The TV rule is that a dialog's confirm action takes `initialFocus`, because otherwise the first
 * remote press does nothing. Here the first thing anyone does is type, and a focused button with
 * an empty field below it is one press further from the only useful action. The button is still
 * reachable by one press down, and the field's focus ring comes from `mosqueTextFieldColors`.
 */
@Composable
fun PinGate(approval: DisplayApproval) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (pin.isEmpty() || submitting) return
        submitting = true
        error = null
        scope.launch {
            when (val result = approval.verifyPin(pin)) {
                PinResult.Accepted -> {
                    // Nothing to do and nothing to say: being allowed changes the link status, so
                    // this whole gate leaves the composition. A success message here would be a
                    // toast nobody reads on a screen that is already changing.
                    pin = ""
                }
                is PinResult.Rejected -> {
                    error = result.message
                        ?: Lang.get("That PIN was not accepted.", "PIN itu tidak diterima.")
                    pin = ""
                }
                is PinResult.Unreachable -> {
                    // Deliberately not phrased as a wrong PIN. Telling a takmir their PIN is wrong
                    // when the wifi dropped sends them looking for the wrong problem.
                    error = Lang.get(
                        "Could not reach the display${result.reason?.let { ": $it" } ?: ""}.",
                        "Tidak bisa menghubungi display${result.reason?.let { ": $it" } ?: ""}."
                    )
                }
            }
            submitting = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MosqueTheme.colors.backdrop),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = GATE_MAX_WIDTH)
                .padding(MosqueTheme.spacing.xl)
                .background(
                    color = MosqueTheme.colors.surfaceRaised,
                    shape = RoundedCornerShape(MosqueTheme.radius.lg)
                )
                .padding(MosqueTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(MosqueTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MosqueTheme.colors.accent,
                modifier = Modifier.size(MosqueTheme.size.iconLg)
            )
            Text(
                Lang.get("Access locked", "Akses Terkunci"),
                color = MosqueTheme.colors.accent,
                fontSize = MosqueTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                Lang.get(
                    "This display asks for a PIN before a remote control may change its settings.",
                    "Display ini meminta PIN sebelum remote boleh mengubah pengaturannya."
                ),
                color = MosqueTheme.colors.textSecondary,
                fontSize = MosqueTheme.typography.bodyCompact,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter { c -> c.isDigit() }
                    error = null
                },
                label = { Text(Lang.get("Security PIN", "PIN Keamanan")) },
                singleLine = true,
                enabled = !submitting,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = mosqueTextFieldColors(),
                modifier = Modifier.fillMaxWidth().initialFocus()
            )

            error?.let {
                Text(
                    it,
                    color = MosqueTheme.colors.danger,
                    fontSize = MosqueTheme.typography.caption,
                    textAlign = TextAlign.Center
                )
            }

            MosquePrimaryButton(
                onClick = { submit() },
                enabled = pin.isNotEmpty() && !submitting,
                shape = RoundedCornerShape(MosqueTheme.radius.md),
                modifier = Modifier.fillMaxWidth().heightIn(min = MosqueTheme.size.controlLg)
            ) {
                Text(
                    if (submitting) Lang.get("Checking…", "Memeriksa...")
                    else Lang.get("Enter", "Masuk"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private val GATE_MAX_WIDTH = 400.dp
