package com.criscard90.ytmp3.download

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Conversione audio sorgente → MP3 320 kbps con metadata ID3
 * usando FFmpeg (ffmpeg-kit, encoder libmp3lame).
 *
 * Se FFmpeg non si avvia sul dispositivo (es. `FFmpegKit failed to start`
 * su alcuni Xiaomi), usa il fallback nativo: il flusso AAC viene solo
 * reincapsulato in M4A (nessuna ricodifica) con le API di sistema, che
 * funzionano ovunque senza librerie native di terze parti.
 */
object Mp3Converter {

    /** Esito della conversione: MP3 oppure fallback M4A nativo. */
    data class ConvertedFile(val file: File, val mimeType: String, val extension: String)

    fun convert(src: File, dst: File, title: String, channel: String): ConvertedFile {
        try {
            convertMp3(src, dst, title, channel)
            return ConvertedFile(dst, "audio/mpeg", "mp3")
        } catch (t: Throwable) {
            // Fallback: copia senza ricodifica in M4A con le API di sistema
            val fallback = File(dst.parentFile, dst.nameWithoutExtension + ".m4a")
            remuxToM4a(src, fallback)
            if (!fallback.exists() || fallback.length() == 0L) {
                throw IOException(
                    "Conversione audio fallita (${t.message ?: "errore FFmpeg"})",
                    t as? Exception,
                )
            }
            return ConvertedFile(fallback, "audio/mp4", "m4a")
        }
    }

    private fun convertMp3(src: File, dst: File, title: String, channel: String) {
        val args = arrayOf(
            "-y",
            "-i", src.absolutePath,
            "-vn", "-sn", "-dn",
            "-c:a", "libmp3lame",
            "-b:a", "320k",
            "-metadata", "title=$title",
            "-metadata", "artist=$channel",
            "-metadata", "comment=Scaricato con ytmp3",
            "-id3v2_version", "3",
            dst.absolutePath,
        )
        val session = FFmpegKit.executeWithArguments(args)
        if (!ReturnCode.isSuccess(session.returnCode)) {
            val details = session.failStackTrace?.take(600)
                ?: "codice ${session.returnCode.value}"
            throw IOException("Conversione MP3 fallita: $details")
        }
    }

    /**
     * Reincapsula il flusso AAC in un contenitore M4A senza ricodificare:
     * veloce (una copia) e sempre disponibile (API Android di sistema).
     */
    private fun remuxToM4a(src: File, dst: File) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(src.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("Nessuna traccia audio nel file scaricato")
            extractor.selectTrack(trackIndex)

            dst.parentFile?.mkdirs()
            if (dst.exists()) dst.delete()
            val muxer = MediaMuxer(dst.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val outIndex = muxer.addTrack(extractor.getTrackFormat(trackIndex))
                muxer.start()
                val buffer = ByteBuffer.allocate(256 * 1024)
                val info = android.media.MediaCodec.BufferInfo()
                while (true) {
                    info.offset = 0
                    info.size = extractor.readSampleData(buffer, 0)
                    if (info.size < 0) break
                    info.presentationTimeUs = extractor.sampleTime
                    info.flags = extractor.sampleFlags
                    muxer.writeSampleData(outIndex, buffer, info)
                    extractor.advance()
                }
                muxer.stop()
            } finally {
                try {
                    muxer.release()
                } catch (_: Exception) {
                }
            }
        } finally {
            extractor.release()
        }
    }
}
