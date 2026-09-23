package com.criscard90.ytmp3.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Client HTTP condiviso da tutta l'app (ricerca YouTube + download degli stream audio).
 */
object HttpClient {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Esegue una richiesta POST JSON e restituisce il corpo di risposta come String.
     * Lancia un'eccezione in caso di risposta HTTP non riuscita.
     */
    fun postJson(
        url: String,
        body: String,
        userAgent: String,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val builder = Request.Builder()
            .url(url)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .header("User-Agent", userAgent)
            .header("Accept-Language", "*")
        headers.forEach { (name, value) -> builder.header(name, value) }

        client.newCall(builder.build()).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Richiesta fallita (HTTP ${response.code})")
            }
            return payload
        }
    }
}
