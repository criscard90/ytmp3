package com.criscard90.ytmp3.youtube

/**
 * Risultato di ricerca YouTube.
 *
 * @property videoId id del video (usato per richiedere lo stream audio al player)
 * @property duration durata formattata ("3:34"), null se il video è live
 * @property avatarUrl immagine del canale (opzionale)
 */
data class VideoSearchItem(
    val videoId: String,
    val title: String,
    val channel: String,
    val duration: String?,
    val viewsText: String?,
    val publishedText: String?,
    val thumbnailUrl: String?,
    val avatarUrl: String?,
)

/**
 * Stream audio selezionato per il download.
 *
 * @property url URL diretto (scade dopo qualche ora: va usato subito)
 * @property bitrate bitrate medio in bit/s (serve per scegliere la massima qualità)
 * @property mimeType es. `audio/mp4; codecs="mp4a.40.2"`
 */
data class AudioStreamInfo(
    val videoId: String,
    val url: String,
    val itag: Int,
    val mimeType: String,
    val bitrate: Int,
    val contentLength: Long,
) {
    val container: String
        get() = mimeType.substringBefore(";").substringAfter("/").trim()

    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
}
