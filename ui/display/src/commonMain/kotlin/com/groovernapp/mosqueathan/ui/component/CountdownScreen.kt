package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.groovernapp.mosqueathan.ui.display.displayBackgrounds
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun CountdownScreen(
    background: Painter,
    currentTime: String,
    titleText: String,
    prayerName: String,
    countdownValue: String,
    footerText: String,
    gradientColors: List<Color> = listOf(
        MosqueTheme.colors.scrim.copy(alpha = 0.76f),
        MosqueTheme.colors.backdrop.copy(alpha = 0.86f),
        MosqueTheme.colors.scrim.copy(alpha = 0.80f)
    ),
    onDismiss: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Image(
            painter = background,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = gradientColors))
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = 72.dp * MosqueTheme.displayScale,
                        vertical = 40.dp * MosqueTheme.displayScale
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = currentTime,
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = MosqueTheme.typography.countdownTitle,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = titleText,
                        color = MosqueTheme.colors.accent,
                        fontSize = MosqueTheme.typography.countdownCaption,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp * MosqueTheme.displayScale))

                    Text(
                        text = prayerName,
                        color = MosqueTheme.colors.textPrimary,
                        fontSize = MosqueTheme.typography.countdownDigits,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        modifier = Modifier.height(
                            MosqueTheme.spacing.xxxl * MosqueTheme.displayScale
                        )
                    )

                    CountdownPanel(value = countdownValue)
                }

                Spacer(modifier = Modifier.height(1.dp))
            }

            RunningTextBar(text = footerText)
        }
    }
}

/**
 * The countdown digits in their bordered panel.
 *
 * Every padding here is multiplied by [MosqueTheme.displayScale], and it is not
 * cosmetic. The enclosing column is `weight(1f)` with `SpaceBetween`, so when the
 * content is taller than the window the children get compressed — and because this
 * panel is `clip`ped to its rounded shape, the overflow is *cut off* rather than merely
 * overlapping. On a phone in landscape that clipped the countdown in half: the one
 * number the whole screen exists to show.
 */
@Composable
private fun CountdownPanel(value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(MosqueTheme.radius.md))
            .background(MosqueTheme.colors.scrim.copy(alpha = 0.40f))
            .border(
                width = 1.dp,
                color = MosqueTheme.colors.textDisabled,
                shape = RoundedCornerShape(MosqueTheme.radius.md)
            )
            .padding(
                horizontal = 56.dp * MosqueTheme.displayScale,
                vertical = 28.dp * MosqueTheme.displayScale
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = MosqueTheme.colors.textPrimary,
            fontSize = MosqueTheme.typography.countdownDigitsLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, widthDp = 1920, heightDp = 1080)
@Composable
fun CountdownScreenPreview() {
    MosqueTheme {
        CountdownScreen(
            background = displayBackgrounds.iqomah,
            currentTime = "18:30",
            titleText = "Menuju Waktu",
            prayerName = "Maghrib",
            countdownValue = "05:00",
            footerText = "Bersegeralah menuju masjid untuk menunaikan shalat berjamaah."
        )
    }
}
