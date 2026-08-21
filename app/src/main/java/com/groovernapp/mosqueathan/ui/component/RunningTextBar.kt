package com.groovernapp.mosqueathan.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.groovernapp.mosqueathan.ui.design.MosqueTheme

@Composable
fun RunningTextBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MosqueTheme.colors.runningTextBackground)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {

        Text(
            text = text,
            color = MosqueTheme.colors.accentAmber,
            fontSize = MosqueTheme.typography.headline,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.basicMarquee(
                iterations = Int.MAX_VALUE,
                velocity = 50.dp
            )
        )
    }
}