package com.criscard90.ytmp3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.criscard90.ytmp3.ui.components.VideoRow
import com.criscard90.ytmp3.ui.theme.YtRed
import com.criscard90.ytmp3.ui.theme.YtTextSecondary
import com.criscard90.ytmp3.youtube.VideoSearchItem

/**
 * Schermata dei risultati di ricerca (con stati vuoto/errore/caricamento).
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onSelect: (VideoSearchItem) -> Unit,
) {
    when (state) {
        SearchUiState.Idle -> EmptyHint(modifier)
        SearchUiState.Loading -> Centered(modifier) {
            CircularProgressIndicator(color = YtRed, strokeWidth = 3.dp)
        }
        is SearchUiState.Error -> Centered(modifier) {
            Text(
                text = state.message,
                color = YtTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = YtRed),
            ) {
                Text("Riprova", color = Color.White)
            }
        }
        is SearchUiState.Success -> {
            if (state.items.isEmpty()) {
                Centered(modifier) {
                    Text("Nessun risultato", color = YtTextSecondary, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.items, key = { it.videoId }) { item ->
                        VideoRow(video = item, onClick = { onSelect(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Centered(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun EmptyHint(modifier: Modifier) {
    Centered(modifier) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(YtRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Cerca su YouTube",
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scarica l'audio in MP3 alla massima qualità\nsenza login Google",
            color = YtTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "MP3 320 kbps · salvataggio in Music/ytmp3",
            color = YtTextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
