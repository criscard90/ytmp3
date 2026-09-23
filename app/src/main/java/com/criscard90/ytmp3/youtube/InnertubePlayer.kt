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

    private fun requestAudioStream(client: PlayerClient, videoId: String): AudioStreamInfo {
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
}
