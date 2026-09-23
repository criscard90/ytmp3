package com.criscard90.ytmp3.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.criscard90.ytmp3.R
import com.criscard90.ytmp3.downloads.DownloadedTrack
import com.criscard90.ytmp3.downloads.DownloadsRepository
import com.criscard90.ytmp3.ui.theme.YtRed
import com.criscard90.ytmp3.ui.theme.YtSurfaceVariant
import com.criscard90.ytmp3.ui.theme.YtTextSecondary

/** Lista degli MP3 scaricati. */
@Composable
fun LazyTracks(
    tracks: List<DownloadedTrack>,
    onPlay: (DownloadedTrack) -> Unit,
    onDelete: (DownloadedTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(tracks, key = { it.uri.toString() }) { track ->
            TrackRow(track = track, onPlay = { onPlay(track) }, onDelete = { onDelete(track) })
        }
    }
}

/** Riga di un MP3: icona, titolo, metadati, play ed elimina. */
@Composable
fun TrackRow(
    track: DownloadedTrack,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(YtSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = YtRed,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.infoText,
                color = YtTextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PlayButton(onClick = onPlay)
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Elimina",
                tint = YtTextSecondary,
            )
        }
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    val context = LocalContext.current
    IconButton(
        onClick = {
            try {
                onClick()
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Nessun player audio trovato", Toast.LENGTH_SHORT).show()
            }
        },
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Riproduci",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(YtRed)
                .padding(6.dp),
        )
    }
}
