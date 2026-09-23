package com.criscard90.ytmp3.download

import android.content.Context
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.criscard90.ytmp3.util.sanitizeFileName
import com.criscard90.ytmp3.youtube.InnertubePlayer
import com.criscard90.ytmp3.youtube.VideoSearchItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/** Stato di un singolo download. */
sealed interface DownloadState {
    /** Richiesta dello stream audio al player YouTube. */
    data object Resolving : DownloadState

    /** Download dello stream sorgente; `progress` in 0..100, oppure -1 se sconosciuto. */
    data class Downloading(val progress: Int) : DownloadState

    /** Conversione a MP3 con FFmpeg (libmp3lame). */
    data object Converting : DownloadState

    /** Completato: file salvato in Music/ytmp3. */
    data class Done(val fileName: String) : DownloadState

    data class Failed(val message: String) : DownloadState

    val isActive: Boolean
        get() = this is Resolving || this is Downloading || this is Converting
}

data class DownloadTask(
    val videoId: String,
    val title: String,
    val channel: String,
    val state: DownloadState,
)

/**
 * Coda dei download: risoluzione stream → download con progresso → conversione MP3 → MediaStore.
 *
 * Il lavoro gira su un [CoroutineScope] indipendente (SupervisorJob + IO): sopravvive a
 * rotazioni e cambi di schermata. Il [DownloadService] si occupa della notifica di progresso.
 */
object DownloadManager {

    private const val MAX_HISTORY = 20
    private const val TMP_DIR = "downloads"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    /** Avvia il download dell'audio del video (ignorato se già in corso). */
    fun enqueue(context: Context, video: VideoSearchItem) {
        if (_tasks.value.any { it.videoId == video.videoId && it.state.isActive }) return

        val task = DownloadTask(video.videoId, video.title, video.channel, DownloadState.Resolving)
        _tasks.update { current ->
            var list = current.filterNot { it.videoId == video.videoId } + task
            if (list.size > MAX_HISTORY) {
                val firstTerminal = list.indexOfFirst { !it.state.isActive }
                if (firstTerminal >= 0) {
                    list = list.filterIndexed { index, _ -> index != firstTerminal }
                }
            }
            list
        }

        DownloadService.start(context)
        val appContext = context.applicationContext
        scope.launch { runTask(appContext, video.videoId, video.title, video.channel) }
    }

    private suspend fun runTask(
        appContext: Context,
        videoId: String,
        title: String,
        channel: String,
    ) {
        val tmpDir = File(appContext.cacheDir, TMP_DIR).apply { mkdirs() }
        val srcFile = File(tmpDir, "$videoId.src")
        val mp3File = File(tmpDir, "$videoId.mp3")
        try {
            setState(videoId, DownloadState.Resolving)
            val stream = InnertubePlayer.bestAudioStream(videoId)

            setState(videoId, DownloadState.Downloading(-1))
            Downloader.download(stream.url, srcFile) { read, total ->
                val progress = if (total > 0) {
                    ((read * 100) / total).toInt().coerceIn(0, 100)
                } else {
                    -1
                }
                setState(videoId, DownloadState.Downloading(progress))
            }

            setState(videoId, DownloadState.Converting)
            Mp3Converter.convert(srcFile, mp3File, title, channel)

            val displayName = "${sanitizeFileName(title)}.mp3"
            MediaStoreSaver.save(appContext, mp3File, displayName, title, channel)

            setState(videoId, DownloadState.Done(displayName))
        } catch (t: Throwable) {
            setState(videoId, DownloadState.Failed(t.message ?: "Errore durante il download"))
        } finally {
            srcFile.delete()
            mp3File.delete()
        }
    }

    private fun setState(videoId: String, state: DownloadState) {
        _tasks.update { list ->
            list.map { if (it.videoId == videoId) it.copy(state = state) else it }
        }
    }
}
