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

    /** Esito della conversione: MP3, oppure fallback senza ricodifica. */
    data class ConvertedFile(val file: File, val mimeType: String, val extension: String)

    /**
     * @param preferredAac true quando lo stream scelto è già AAC/MP4: in caso di
     *   problemi con FFmpeg, prima prova il percorso nativo (veloce).
     */
    fun convert(
        src: File,
        dst: File,
        title: String,
        channel: String,
        preferredAac: Boolean,
    ): ConvertedFile {
        validateSource(src)
        val intermediates = mutableListOf<File>()

        // Percorso nativo (copia senza ricodifica): veloce e senza librerie native.
        // Vale SOLO per MP4/AAC: un WebM/Opus non può finire in un M4A e un raw
        // WebM non si apre come .mp3 — per quelli serve per forza FFmpeg.
        if (preferredAac) {
            val native = File(dst.parentFile, dst.nameWithoutExtension + ".m4a")
            val nativeOk = runCatching {
                remuxToM4a(src, native)
                NativeProbe.checkM4a(native)
            }.isSuccess
            if (nativeOk) {
                return ConvertedFile(native, "audio/mp4", "m4a")
            }
            intermediates += native
        }

        // Percorso principale: MP3 320 kbps con FFmpeg.
        val ffmpegError = runCatching {
            convertMp3(src, dst, title, channel)
            NativeProbe.checkMp3(dst)
        }.exceptionOrNull()
        if (ffmpegError == null) {
            intermediates.forEach { it.delete() }
            return ConvertedFile(dst, "audio/mpeg", "mp3")
        }
        dst.delete()
        // Ultima spiaggia, SOLO per AAC: raw -> M4A (mai per Opus/WebM, che
        // darebbe di nuovo "failed to add the track to the muxer").
        if (preferredAac) {
            val fallback = File(dst.parentFile, dst.nameWithoutExtension + ".m4a")
            val fallbackOk = runCatching {
                remuxToM4a(src, fallback)
                NativeProbe.checkM4a(fallback)
            }.isSuccess
            if (fallbackOk) {
                intermediates.forEach { if (it != fallback) it.delete() }
                return ConvertedFile(fallback, "audio/mp4", "m4a")
            }
        }
        intermediates.forEach { it.delete() }
        throw IOException(
            "Conversione audio fallita (${ffmpegError.message ?: "errore FFmpeg"})",
            ffmpegError as? Exception,
        )
    }

    /** Il download può essere troncato: scarta subito file vuoti o pagine HTML. */
    private fun validateSource(src: File) {
        if (!src.exists() || src.length() < 32 * 1024) {
            throw IOException(
                "File audio scaricato troppo piccolo (${src.length()} byte): " +
                    "riprova il download"
            )
        }
        // YouTube a volte risponde con una pagina di errore invece dell'audio.
        val head = ByteArray(256)
        src.inputStream().use { input ->
            val read = input.read(head)
            if (read > 0) {
                val text = String(head, 0, read, Charsets.ISO_8859_1).trimStart()
                if (text.startsWith("<") || text.startsWith("\uFEFF<")) {
                    throw IOException(
                        "YouTube ha restituito una pagina di errore invece dell'audio: " +
                            "riprova il download"
                    )
                }
            }
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
     *
     * Va chiamato SOLO per contenitori MP4/AAC: MediaMuxer con output MPEG_4
     * rifiuta Opus/Vorbis con `failed to add the track to the muxer`.
     */
    private fun remuxToM4a(src: File, dst: File) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(src.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                val mime = extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME).orEmpty()
                mime.startsWith("audio/mp4") || mime == "audio/aac"
            } ?: throw IOException(
                "Traccia AAC non trovata nel file scaricato: " +
                    "serve la conversione MP3 (FFmpeg)"
            )
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

/**
 * Verifica con i decoder di sistema che il file prodotto sia davvero audio
 * riproducibile (evita di salvare MP3 troncati o M4A corrotti in MediaStore).
 */
internal object NativeProbe {

    fun checkMp3(file: File) = checkAudio(file, "MP3")

    fun checkM4a(file: File) = checkAudio(file, "M4A")

    private fun checkAudio(file: File, label: String) {
        if (!file.exists() || file.length() < 16 * 1024) {
            file.delete()
            throw IOException("File $label non valido (${file.length()} byte)")
        }
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
            val hasAudio = (0 until extractor.trackCount).any { index ->
                extractor.getTrackFormat(index)
                    .getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            if (!hasAudio) {
                throw IOException("File $label senza traccia audio")
            }
        } catch (e: IOException) {
            file.delete()
            throw e
        } catch (e: Exception) {
            file.delete()
            throw IOException("File $label non leggibile (${e.message})", e)
        } finally {
            extractor.release()
        }
    }
}
