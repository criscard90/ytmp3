package com.criscard90.ytmp3.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.criscard90.ytmp3.R
import com.criscard90.ytmp3.download.DownloadManager
import com.criscard90.ytmp3.download.DownloadState
import com.criscard90.ytmp3.ui.theme.YtDivider
import com.criscard90.ytmp3.ui.theme.YtRed
import com.criscard90.ytmp3.ui.theme.YtSurfaceVariant
import com.criscard90.ytmp3.ui.theme.YtTextSecondary
import com.criscard90.ytmp3.youtube.VideoSearchItem

/**
 * Bottom sheet di conferma download con indicatore di stato in tempo reale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSheet(
    video: VideoSearchItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val tasks by DownloadManager.tasks.collectAsState()
    val state = tasks.firstOrNull { it.videoId == video.videoId }?.state

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = YtDivider) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(79.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(YtSurfaceVariant),
                ) {
                    video.thumbnailUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(79.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = video.channel,
                        color = YtTextSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            when (state) {
                null -> DownloadButton(onClick = onDownload)

                is DownloadState.Resolving -> BusyRow("Richiesta dello stream audio…")

                is DownloadState.Downloading -> {
                    if (state.progress >= 0) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = YtRed,
                            trackColor = YtSurfaceVariant,
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = YtRed,
                            trackColor = YtSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusText(
                        if (state.progress >= 0) "Download audio · ${state.progress}%"
                        else "Download audio…"
                    )
                }

                is DownloadState.Converting -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = YtRed,
                        trackColor = YtSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusText("Conversione in MP3 320 kbps…")
                }

                is DownloadState.Done -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF3DDC84),
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Salvato in Music/ytmp3 · ${state.fileName}",
                            color = Color.White,
                            fontSize = 13.5.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDownload) {
                        Text("Scarica di nuovo", color = YtRed)
                    }
                }

                is DownloadState.Failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = YtRed),
                    ) {
                        Text("Riprova", color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "MP3 320 kbps · massima qualità disponibile · nessun login Google",
                color = YtTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DownloadButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = YtRed),
        shape = RoundedCornerShape(25.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_download),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "Scarica MP3 · massima qualità",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BusyRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = YtRed,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        StatusText(label)
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text = text, color = Color.White, fontSize = 14.sp)
}
