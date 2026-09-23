package com.criscard90.ytmp3.youtube

import com.criscard90.ytmp3.network.HttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Ricerca video tramite l'API Innertube pubblica di YouTube (client WEB).
 * Nessun login richiesto. Da usare su un thread non-UI.
 */
object Innertube {

    internal const val WEB_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    internal const val ANDROID_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"

    private const val WEB_CLIENT_VERSION = "2.20250601.00.00"
    private const val WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    /**
     * visitorData restituito dalla ricerca: riutilizzato nelle richieste successive
     * al posto dei cookie di consenso.
     */
    @Volatile
    var visitorData: String? = null
        private set

    /** Esegue una ricerca di video. */
    fun search(query: String): List<VideoSearchItem> {
        val (hl, gl) = localeHlGl()
        val client = JSONObject()
            .put("clientName", "WEB")
            .put("clientVersion", WEB_CLIENT_VERSION)
            .put("hl", hl)
            .put("gl", gl)
        visitorData?.takeIf { it.isNotEmpty() }?.let { client.put("visitorData", it) }

        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("query", query)
            .toString()

        val url = "https://www.youtube.com/youtubei/v1/search?key=$WEB_KEY&prettyPrint=false"
        val root = JSONObject(HttpClient.postJson(url, body, WEB_USER_AGENT))

        root.optJSONObject("error")?.let {
            throw IllegalStateException(it.optString("message").ifEmpty { "Errore di ricerca" })
        }
        root.optJSONObject("responseContext")
            ?.optString("visitorData")
            ?.takeIf { it.isNotEmpty() }
            ?.let { visitorData = it }

        val results = ArrayList<VideoSearchItem>()
        collectVideoRenderers(root, results)
        return results.distinctBy { it.videoId }
    }

    /** Ricorsivo: raccoglie tutti i `videoRenderer` presenti nella risposta. */
    private fun collectVideoRenderers(node: Any?, out: MutableList<VideoSearchItem>) {
        when (node) {
            is JSONObject -> {
                node.optJSONObject("videoRenderer")?.let { renderer ->
                    parseVideoRenderer(renderer)?.let { out.add(it) }
                }
                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "videoRenderer") continue
                    collectVideoRenderers(node.opt(key), out)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) collectVideoRenderers(node.opt(i), out)
            }
        }
    }

    private fun parseVideoRenderer(renderer: JSONObject): VideoSearchItem? {
        val videoId = renderer.optString("videoId")
        if (videoId.isEmpty()) return null

        val title = runsText(renderer.optJSONObject("title"))
            .ifEmpty { renderer.optJSONObject("title")?.optString("simpleText").orEmpty() }
        if (title.isEmpty()) return null

        val channel = firstNonEmpty(
            runsText(renderer.optJSONObject("ownerText")),
            runsText(renderer.optJSONObject("longBylineText")),
            runsText(renderer.optJSONObject("shortBylineText")),
        )

        val duration = renderer.optJSONObject("lengthText")?.optString("simpleText")
            ?.takeIf { it.isNotEmpty() && !it.equals("LIVE", ignoreCase = true) }

        val views = firstNonEmpty(
            renderer.optJSONObject("viewCountText")?.optString("simpleText"),
            renderer.optJSONObject("shortViewCountText")?.optString("simpleText"),
        ).takeIf { it.isNotEmpty() }

        val published = renderer.optJSONObject("publishedTimeText")
            ?.optString("simpleText")
            ?.takeIf { it.isNotEmpty() }

        val thumbnailUrl = bestThumbnailUrl(renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails"))
        val avatarUrl = renderer.optJSONObject("channelThumbnailSupportedRenderers")
            ?.let { findThumbnails(it) }
            ?.let { bestThumbnailUrl(it) }

        return VideoSearchItem(
            videoId = videoId,
            title = title,
            channel = channel,
            duration = duration,
            viewsText = views,
            publishedText = published,
            thumbnailUrl = thumbnailUrl,
            avatarUrl = avatarUrl,
        )
    }

    internal fun runsText(node: JSONObject?): String {
        if (node == null) return ""
        val runs = node.optJSONArray("runs") ?: return node.optString("simpleText")
        return (0 until runs.length())
            .mapNotNull { runs.optJSONObject(it)?.optString("text") }
            .joinToString("")
    }

    internal fun firstNonEmpty(vararg values: String?): String =
        values.firstOrNull { !it.isNullOrEmpty() }.orEmpty()

    internal fun localeHlGl(): Pair<String, String> {
        val locale = Locale.getDefault()
        return locale.language.ifEmpty { "it" } to
            (locale.country.ifEmpty { "IT" }).uppercase()
    }

    private fun bestThumbnailUrl(thumbnails: JSONArray?): String? {
        if (thumbnails == null || thumbnails.length() == 0) return null
        var best: String? = null
        var bestWidth = -1
        for (i in 0 until thumbnails.length()) {
            val thumb = thumbnails.optJSONObject(i) ?: continue
            val width = thumb.optInt("width")
            if (width >= bestWidth) {
                bestWidth = width
                best = thumb.optString("url").takeIf { it.isNotEmpty() }
            }
        }
        return best
    }

    /** Trova ricorsivamente il primo array "thumbnails" contenuto nel nodo. */
    private fun findThumbnails(node: JSONObject): JSONArray? {
        node.optJSONArray("thumbnails")?.let { return it }
        val keys = node.keys()
        while (keys.hasNext()) {
            when (val child = node.opt(keys.next())) {
                is JSONObject -> findThumbnails(child)?.let { return it }
                is JSONArray -> {
                    for (i in 0 until child.length()) {
                        val item = child.optJSONObject(i) ?: continue
                        findThumbnails(item)?.let { return it }
                    }
                }
            }
        }
        return null
    }
}
