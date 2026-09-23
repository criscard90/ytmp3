package com.criscard90.ytmp3.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.criscard90.ytmp3.download.DownloadManager
import com.criscard90.ytmp3.download.DownloadState
import com.criscard90.ytmp3.downloads.DownloadedTrack
import com.criscard90.ytmp3.downloads.DownloadsRepository
import com.criscard90.ytmp3.ui.components.EmptyDownloads
import com.criscard90.ytmp3.ui.components.LazyTracks
import com.criscard90.ytmp3.ui.theme.YtTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Schermata "Download": elenco degli MP3 salvati in Music/ytmp3.
 */
@Composable
fun DownloadsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tracks by remember { mutableStateOf<List<DownloadedTrack>>(emptyList()) }
    var refreshToken by remember { mutableStateOf(0) }
    val tasks by DownloadManager.tasks.collectAsState()

    // Ricarica l'elenco quando un download viene completato o all'apertura della tab
    val doneCount = tasks.count { it.state is DownloadState.Done }
    LaunchedEffect(doneCount, refreshToken) {
        tracks = withContext(Dispatchers.IO) { DownloadsRepository.list(context) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Download",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (tracks.size == 1) "1 file" else "${tracks.size} file",
                color = YtTextSecondary,
                fontSize = 13.sp,
            )
            IconButton(onClick = { refreshToken++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna", tint = YtTextSecondary)
            }
        }
        Spacer(Modifier.height(4.dp))

        if (tracks.isEmpty()) {
            EmptyDownloads(modifier = Modifier.weight(1f))
        } else {
            LazyTracks(
                tracks = tracks,
                onPlay = { track ->
                    try {
                        context.startActivity(DownloadsRepository.playIntent(track))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "Nessun player audio trovato", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                onDelete = { track ->
                    scope.launch {
                        val removed = withContext(Dispatchers.IO) {
                            DownloadsRepository.delete(context, track)
                        }
                        if (removed) tracks = tracks.filterNot { it.uri == track.uri }
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
