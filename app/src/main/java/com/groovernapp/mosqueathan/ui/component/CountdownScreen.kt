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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.R
import com.groovernapp.mosqueathan.ui.theme.MosqueAthanTheme
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun CountdownScreen(
    backgroundImageRes: Int,
    currentTime: String,
    titleText: String,
    prayerName: String,
    countdownValue: String,
    footerText: String,
    gradientColors: List<Color> = listOf(
        MosqueTheme.colors.scrim.copy(alpha = 0.76f),
        MosqueTheme.colors.backdropIqomah.copy(alpha = 0.86f),
        MosqueTheme.colors.scrim.copy(alpha = 0.80f)
    ),
    onDismiss: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().clickable { onDismiss() }) {
        Image(
            painter = painterResource(backgroundImageRes),
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
                    .padding(horizontal = 72.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = currentTime,
                    color = MosqueTheme.colors.textPrimary,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = titleText,
                        color = MosqueTheme.colors.accentMuted,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = prayerName,
                        color = MosqueTheme.colors.textPrimary,
                        fontSize = 92.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    CountdownPanel(value = countdownValue)
                }

                Spacer(modifier = Modifier.height(1.dp))
            }

            RunningTextBar(text = footerText)
        }
    }
}

@Composable
private fun CountdownPanel(value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MosqueTheme.colors.scrim.copy(alpha = 0.40f))
            .border(
                width = 1.dp,
                color = MosqueTheme.colors.textGhostSoft,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 56.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = MosqueTheme.colors.textPrimary,
            fontSize = 112.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true, device = "spec:width=1920dp,height=1080dp")
@Composable
fun CountdownScreenPreview() {
    MosqueAthanTheme {
        CountdownScreen(
            backgroundImageRes = R.drawable.iqomah_bg_default,
            currentTime = "18:30",
            titleText = "Menuju Waktu",
            prayerName = "Maghrib",
            countdownValue = "05:00",
            footerText = "Bersegeralah menuju masjid untuk menunaikan shalat berjamaah."
        )
    }
}
