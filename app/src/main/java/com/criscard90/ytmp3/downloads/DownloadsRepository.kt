package com.criscard90.ytmp3.downloads

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.criscard90.ytmp3.util.formatBytes
import com.criscard90.ytmp3.util.formatDate

/** Un MP3 presente nella cartella `Music/ytmp3`. */
data class DownloadedTrack(
    val uri: Uri,
    val title: String,
    val sizeBytes: Long,
    val dateMillis: Long,
) {
    val infoText: String
        get() = "${formatBytes(sizeBytes)} · ${formatDate(dateMillis)}"
}

/**
 * Legge/elimina i file scaricati dalla cartella pubblica `Music/ytmp3` (MediaStore).
 */
object DownloadsRepository {

    private const val RELATIVE_DIR = "Music/ytmp3/"

    /** Elenco dei file presenti nella cartella, dal più recente al più vecchio. */
    fun list(context: Context): List<DownloadedTrack> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_MODIFIED,
        )
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("$RELATIVE_DIR%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        val tracks = mutableListOf<DownloadedTrack>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)
                    ?: cursor.getString(nameCol)?.removeSuffix(".mp3")
                    ?: "Audio"
                tracks.add(
                    DownloadedTrack(
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        title = title,
                        sizeBytes = cursor.getLong(sizeCol),
                        dateMillis = cursor.getLong(dateCol),
                    )
                )
            }
        }
        return tracks
    }

    /** Elimina un file (leazel app possedute possono essere rimosse senza permessi extra). */
    fun delete(context: Context, track: DownloadedTrack): Boolean {
        return try {
            context.contentResolver.delete(track.uri, null, null) > 0
        } catch (_: SecurityException) {
            false
        }
    }

    /** Intent per riprodurre il MP3 con un player esterno. */
    fun playIntent(track: DownloadedTrack): Intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(track.uri, "audio/mpeg")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
