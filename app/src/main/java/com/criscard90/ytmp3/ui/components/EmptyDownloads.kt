package com.criscard90.ytmp3.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.criscard90.ytmp3.R
import com.criscard90.ytmp3.ui.theme.YtTextSecondary

/** Stato vuoto della schermata Download. */
@Composable
fun EmptyDownloads(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            tint = YtTextSecondary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text("Nessun audio scaricato", color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Cerca un video e premi\n\"Scarica MP3 · massima qualità\"",
            color = YtTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}
