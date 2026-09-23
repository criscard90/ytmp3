package com.criscard90.ytmp3.download

import com.criscard90.ytmp3.network.HttpClient
import com.criscard90.ytmp3.youtube.InnertubePlayer
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Download di un flusso audio da googlevideo.com con notifica di progresso.
 */
object Downloader {

    /**
     * @param onProgress chiamato con (byte letti, totale) — totale può essere -1
     */
    fun download(url: String, target: File, onProgress: (Long, Long) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", InnertubePlayer.USER_AGENT)
            .build()

        HttpClient.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Download fallito (HTTP ${response.code})")
            }
            val body = response.body ?: throw IOException("Risposta vuota")
            val total = body.contentLength() // -1 se sconosciuto
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    var lastEmit = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        done += read
                        if (done - lastEmit >= 256 * 1024) {
                            lastEmit = done
                            onProgress(done, total)
                        }
                    }
                    onProgress(done, if (total > 0) total else done)
                }
            }
        }
    }
}
