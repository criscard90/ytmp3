package com.criscard90.ytmp3.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.criscard90.ytmp3.ui.theme.YtRed
import com.criscard90.ytmp3.ui.theme.YtSurfaceVariant
import com.criscard90.ytmp3.ui.theme.YtTextSecondary
import com.criscard90.ytmp3.youtube.VideoSearchItem

/**
 * Riga risultato di ricerca stile YouTube: avatar · titolo/meta · thumbnail con durata.
 */
@Composable
fun VideoRow(
    video: VideoSearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Avatar del canale (o iniziale su sfondo grigio)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(YtSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val avatar = video.avatarUrl
            if (avatar != null) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
            } else {
                Text(
                    text = video.channel.firstOrNull()?.uppercase() ?: "?",
                    color = YtRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = video.channel,
                color = YtTextSecondary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(video.viewsText, video.publishedText).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    color = YtTextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Thumbnail 16:9 con badge durata
        Box(
            modifier = Modifier
                .width(158.dp)
                .height(89.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(YtSurfaceVariant),
        ) {
            val thumb = video.thumbnailUrl
            if (thumb != null) {
                AsyncImage(
                    model = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            video.duration?.let { duration ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = duration,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
