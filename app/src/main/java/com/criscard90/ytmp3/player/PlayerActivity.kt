package com.criscard90.ytmp3.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.criscard90.ytmp3.ui.theme.YtRed
import com.criscard90.ytmp3.ui.theme.YtTextSecondary
import com.criscard90.ytmp3.ui.theme.YtTheme
import com.criscard90.ytmp3.util.YouTubeOpener
import com.criscard90.ytmp3.youtube.InnertubePlayer
import com.criscard90.ytmp3.youtube.PlaybackSources
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Riproduzione video in-app con Media3/ExoPlayer.
 *
 * Risolve gli stream adattivi (video ≤1080p + audio, fusi con [MergingMediaSource])
 * oppure usa il formato progressivo muxed. In caso di errore rimanda a YouTube.
 */
class PlayerActivity : ComponentActivity() {

    private val resolveScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var exoPlayer: ExoPlayer? by mutableStateOf(null)
    private var loading by mutableStateOf(true)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (videoId.isNullOrEmpty()) {
            finish()
            return
        }

        setContent {
            YtTheme {
                PlayerScreen(
                    title = title,
                    loading = loading,
                    errorMessage = errorMessage,
                    player = exoPlayer,
                    onBack = { finish() },
                    onOpenYouTube = { YouTubeOpener.open(this, videoId) },
                )
            }
        }

        load(videoId)
    }

    private fun load(videoId: String) {
        resolveScope.launch {
            try {
                loading = true
                errorMessage = null
                val sources = withContext(Dispatchers.IO) {
                    InnertubePlayer.playbackSources(videoId)
                }
                val player = ExoPlayer.Builder(this@PlayerActivity).build().apply {
                    setMediaSource(buildSource(sources))
                    playWhenReady = true
                    prepare()
                }
                exoPlayer = player
            } catch (t: Throwable) {
                errorMessage = t.message ?: "Impossibile riprodurre il video"
            } finally {
                loading = false
            }
        }
    }

    /** Crea la MediaSource: video adattivo fuso con l'audio, oppure il flusso muxed. */
    private fun buildSource(sources: PlaybackSources): MediaSource {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(InnertubePlayer.USER_AGENT)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
        val factory = ProgressiveMediaSource.Factory(httpFactory)
        val video = factory.createMediaSource(MediaItem.fromUri(sources.videoUrl))
        val audioUrl = sources.audioUrl
        return if (audioUrl.isNullOrEmpty()) {
            video
        } else {
            MergingMediaSource(
                video,
                factory.createMediaSource(MediaItem.fromUri(audioUrl)),
            )
        }
    }

    override fun onDestroy() {
        resolveScope.cancel()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_TITLE = "title"

        /** Apre il player per il video. */
        fun start(context: Context, videoId: String, title: String) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java).apply {
                    putExtra(EXTRA_VIDEO_ID, videoId)
                    putExtra(EXTRA_TITLE, title)
                }
            )
        }
    }
}

@Composable
private fun PlayerScreen(
    title: String,
    loading: Boolean,
    errorMessage: String?,
    player: ExoPlayer?,
    onBack: () -> Unit,
    onOpenYouTube: () -> Unit,
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = Color.White,
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 16.dp),
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                loading -> CircularProgressIndicator(color = YtRed, strokeWidth = 3.dp)

                errorMessage != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage,
                            color = YtTextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onOpenYouTube) {
                            Text("Apri su YouTube", color = Color.White)
                        }
                    }
                }

                player != null -> AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            keepScreenOn = true
                            useController = true
                        }
                    },
                    update = { view -> view.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
