package com.criscard90.ytmp3.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/**
 * Scrive l'MP3 nella cartella pubblica `Music/ytmp3` tramite MediaStore.
 * Su Android 10+ non servono permessi di archiviazione.
 */
object MediaStoreSaver {

    /** Restituisce il nome effettivamente assegnato (MediaStore gestisce le collisioni). */
    fun save(
        context: Context,
        audioFile: File,
        displayName: String,
        title: String,
        channel: String,
        mimeType: String = "audio/mpeg",
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/ytmp3")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, channel)
        }

        val uri: Uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Impossibile creare il file in Music/ytmp3")

        try {
            val output = resolver.openOutputStream(uri)
                ?: throw IOException("Stream di scrittura non disponibile")
            output.use { out ->
                audioFile.inputStream().use { input -> input.copyTo(out) }
            }
            val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }

        // Nome reale del file (in caso di collisioni il sistema aggiunge " (1)")
        return resolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.DISPLAY_NAME),
            null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else displayName
        } ?: displayName
    }
}
