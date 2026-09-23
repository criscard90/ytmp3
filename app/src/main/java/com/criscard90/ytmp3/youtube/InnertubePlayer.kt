package com.criscard90.ytmp3.youtube

import com.criscard90.ytmp3.network.HttpClient
import org.json.JSONObject

/**
 * Recupero dello stream audio tramite l'endpoint `player` di Innertube.
 *
 * I client ANDROID (e il fallback ANDROID_VR) restituiscono URL diretti senza
 * cifratura e senza login. Verificati funzionanti senza autenticazione.
 */
object InnertubePlayer {

    /** User-Agent dell'app YouTube per Android (necessario per il client ANDROID). */
    const val USER_AGENT =
        "com.google.android.youtube/20.10.37 (Linux; U; Android 15) gzip"
    private const val ANDROID_USER_AGENT = USER_AGENT
    private const val ANDROID_VR_USER_AGENT =
        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12; VR) gzip"

    private data class PlayerClient(
        val name: String,
        val version: String,
        val userAgent: String,
        val extra: (JSONObject) -> Unit,
    )

    private fun clients(): List<PlayerClient> = listOf(
        PlayerClient("ANDROID", "20.10.37", ANDROID_USER_AGENT) {
            it.put("androidSdkVersion", 35)
                .put("osName", "Android")
                .put("osVersion", "15")
                .put("deviceMake", "Google")
                .put("deviceModel", "Pixel 8")
        },
        PlayerClient("ANDROID_VR", "1.60.19", ANDROID_VR_USER_AGENT) {
            it.put("androidSdkVersion", 34)
                .put("osName", "Android")
                .put("osVersion", "12")
        },
    )

    /**
     * Restituisce lo stream audio di **massima bitrate** per il video.
     * Da chiamare su un thread non-UI.
     */
    fun bestAudioStream(videoId: String): AudioStreamInfo {
        val errors = mutableListOf<String>()
        for (client in clients()) {
            try {
                return requestAudioStream(client, videoId)
            } catch (t: Throwable) {
                errors.add("${client.name}: ${t.message}")
            }
        }
        throw IllegalStateException(
            "Impossibile recuperare l'audio del video. " + errors.joinToString(" · ")
        )
    }

    /** Richiesta player comune: costruisce il body, verifica errori e playability. */
    private fun playerResponse(client: PlayerClient, videoId: String): JSONObject {
        val (hl, gl) = Innertube.localeHlGl()
        val clientJson = JSONObject()
            .put("clientName", client.name)
            .put("clientVersion", client.version)
            .put("hl", hl)
            .put("gl", gl)
        Innertube.visitorData?.takeIf { it.isNotEmpty() }?.let { clientJson.put("visitorData", it) }
        client.extra(clientJson)

        val body = JSONObject()
            .put("videoId", videoId)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)
            .put("context", JSONObject().put("client", clientJson))
            .toString()

        val url = "https://www.youtube.com/youtubei/v1/player?key=${Innertube.ANDROID_KEY}&prettyPrint=false"
        val headers = buildMap {
            Innertube.visitorData?.takeIf { it.isNotEmpty() }?.let { put("X-Goog-Visitor-Id", it) }
            put("X-Youtube-Client-Version", client.version)
        }
        val root = JSONObject(HttpClient.postJson(url, body, client.userAgent, headers))

        root.optJSONObject("error")?.let {
            throw IllegalStateException(it.optString("message").ifEmpty { "Errore player" })
        }

        val status = root.optJSONObject("playabilityStatus")
        if (status?.optString("status") != "OK") {
            val reason = status?.optString("reason")?.takeIf { it.isNotEmpty() }
                ?: status?.optString("status")?.takeIf { it.isNotEmpty() }
                ?: "sconosciuto"
            throw IllegalStateException("video non riproducibile ($reason)")
        }
        return root
    }

    private fun requestAudioStream(client: PlayerClient, videoId: String): AudioStreamInfo {
        val root = playerResponse(client, videoId)

        val adaptive = root.optJSONObject("streamingData")?.optJSONArray("adaptiveFormats")
            ?: throw IllegalStateException("nessun formato disponibile")

        val audioFormats = (0 until adaptive.length()).mapNotNull { index ->
            val format = adaptive.optJSONObject(index) ?: return@mapNotNull null
            val mimeType = format.optString("mimeType")
            val streamUrl = format.optString("url")
            if (!mimeType.startsWith("audio/") || streamUrl.isEmpty()) return@mapNotNull null

            val bitrate = format.optInt("averageBitrate").takeIf { it > 0 }
                ?: format.optInt("bitrate")

            AudioStreamInfo(
                videoId = videoId,
                url = streamUrl,
                itag = format.optInt("itag"),
                mimeType = mimeType,
                bitrate = bitrate,
                contentLength = format.optLong("contentLength", -1L),
            )
        }

        // Massima qualità = bitrate più alto disponibile (es. itag 140 AAC o 251 Opus)
        return audioFormats.maxByOrNull { it.bitrate }
            ?: throw IllegalStateException("nessun flusso audio con URL diretto")
    }

    // ---------------------------------------------------------------------------------------------
    // Riproduzione video in-app
    // ---------------------------------------------------------------------------------------------

    /** Estrae i codec da una mimeType del tipo `video/mp4; codecs="avc1.640028"`. */
    private val CODECS_IN_MIME = Regex("codecs=\"([^\"]+)\"")

    private data class Candidate(
        val url: String,
        val height: Int,
        val bitrate: Int,
        val isH264Mp4: Boolean,
        val isAacMp4: Boolean,
    )

    /**
     * Sorgenti per la riproduzione in-app: preferisce video adattivo (≤1080p, H.264/VP9,
     * escluso AV1) fuso con l'audio separato; in alternativa il formato muxed (es. itag 18).
     */
    fun playbackSources(videoId: String): PlaybackSources {
        val errors = mutableListOf<String>()
        for (client in clients()) {
            try {
                val root = playerResponse(client, videoId)
                return parsePlaybackSources(root)
            } catch (t: Throwable) {
                errors.add("${client.name}: ${t.message}")
            }
        }
        throw IllegalStateException(
            "Impossibile caricare il video. " + errors.joinToString(" · ")
        )
    }

    private fun parsePlaybackSources(root: JSONObject): PlaybackSources {
        val streaming = root.optJSONObject("streamingData")
            ?: throw IllegalStateException("nessun formato disponibile")

        val videoCandidates = mutableListOf<Candidate>()
        val audioCandidates = mutableListOf<Candidate>()
        val muxedCandidates = mutableListOf<Candidate>()

        val adaptive = streaming.optJSONArray("adaptiveFormats")
        if (adaptive != null) {
            for (i in 0 until adaptive.length()) {
                val f = adaptive.optJSONObject(i) ?: continue
                val mime = f.optString("mimeType")
                val url = f.optString("url")
                if (url.isEmpty()) continue
                // Alcuni client non espongono il campo "codecs": è contenuto nella mimeType
                val codecs = f.optString("codecs").ifEmpty {
                    CODECS_IN_MIME.find(mime)?.groupValues?.get(1).orEmpty()
                }
                val height = f.optInt("height")
                val bitrate = f.optInt("averageBitrate").takeIf { it > 0 } ?: f.optInt("bitrate")
                val candidate = Candidate(
                    url = url,
                    height = height,
                    bitrate = bitrate,
                    isH264Mp4 = mime.startsWith("video/mp4") && codecs.contains("avc1"),
                    isAacMp4 = mime.startsWith("audio/mp4"),
                )
                when {
                    mime.startsWith("video/") -> {
                        // AV1 non è decodificato su molti dispositivi: escluso
                        if (!codecs.contains("av01") && height in 1..1080) {
                            videoCandidates.add(candidate)
                        }
                    }
                    mime.startsWith("audio/") -> audioCandidates.add(candidate)
                }
            }
        }

        // Formati progressivi muxed (video+audio nello stesso flusso, es. itag 18 a 360p)
        val progressive = streaming.optJSONArray("formats")
        if (progressive != null) {
            for (i in 0 until progressive.length()) {
                val f = progressive.optJSONObject(i) ?: continue
                val url = f.optString("url")
                val height = f.optInt("height")
                if (url.isNotEmpty() && f.optString("mimeType").startsWith("video/") && height > 0) {
                    muxedCandidates.add(
                        Candidate(url, height, f.optInt("bitrate"), false, false)
                    )
                }
            }
        }

        val bestMuxed = muxedCandidates.maxByOrNull { it.height }
        val bestVideo = videoCandidates.maxWithOrNull(
            compareByDescending<Candidate> { it.height }
                .thenByDescending { if (it.isH264Mp4) 1 else 0 }
                .thenByDescending { it.bitrate }
        )
        val bestAudio = audioCandidates.maxWithOrNull(
            compareByDescending<Candidate> { if (it.isAacMp4) 1 else 0 }
                .thenByDescending { it.bitrate }
        )

        return when {
            bestVideo != null && bestAudio != null &&
                (bestMuxed == null || bestVideo.height >= bestMuxed.height) ->
                PlaybackSources(
                    videoUrl = bestVideo.url,
                    audioUrl = bestAudio.url,
                    height = bestVideo.height,
                    isMuxed = false,
                )
            bestMuxed != null ->
                PlaybackSources(
                    videoUrl = bestMuxed.url,
                    audioUrl = null,
                    height = bestMuxed.height,
                    isMuxed = true,
                )
            else -> throw IllegalStateException("nessun formato video riproducibile")
        }
    }
}

/**
 * Sorgenti di riproduzione video.
 *
 * @property audioUrl null quando il flusso video è già muxed (contiene l'audio)
 * @property isMuxed true per i formati progressivi (es. itag 18)
 */
data class PlaybackSources(
    val videoUrl: String,
    val audioUrl: String?,
    val height: Int,
    val isMuxed: Boolean,
)
