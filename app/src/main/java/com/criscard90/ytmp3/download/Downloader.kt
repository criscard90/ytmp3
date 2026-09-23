package com.criscard90.ytmp3.download

import com.criscard90.ytmp3.network.HttpClient
import com.criscard90.ytmp3.youtube.InnertubePlayer
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * Download di un flusso audio da googlevideo.com con notifica di progresso.
 *
 * YouTube lega gli URL firmati al client che li ha emessi, quindi riusiamo lo
 * stesso User-Agent (VISIONOS di default) per non farli rifiutare con 403.
 * La prima richiesta scarica i primi byte per verificare subito l'accesso e poi
 * riprende il resto dal punto interrotto.
 */
object Downloader {

    private const val FALLBACK_USER_AGENT = InnertubePlayer.VISIONOS_USER_AGENT

    /**
     * @param userAgent User-Agent del client che ha emesso l'URL (anti-403);
     *   se vuoto viene usato quello VISIONOS.
     * @param onProgress chiamato con (byte letti, totale) — totale può essere -1
     */
    fun download(
        url: String,
        target: File,
        userAgent: String = "",
        onProgress: (Long, Long) -> Unit,
    ) {
        val agent = userAgent.ifEmpty { FALLBACK_USER_AGENT }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", agent)
            .header("Referer", "https://www.youtube.com/")
            .header("Origin", "https://www.youtube.com")
            .header("Accept", "*/*")
            .header("Accept-Language", "it-IT,it;q=0.9,en;q=0.8")
            // Richiesta parziale: meglio tollerata dai server googlevideo
            .header("Range", "bytes=0-")
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
