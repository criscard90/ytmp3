package com.criscard90.ytmp3.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Codifica MP3 320 kbps **senza FFmpeg**: decodifica il sorgente (AAC/Opus/…)
 * con il `MediaCodec` di sistema e ricodifica in MP3 con libmp3lame (LAME
 * 3.100 compilata da noi con NDK, `.so` nel nostro package).
 */
internal object NativeMp3Encoder {

    fun encode(src: File, dst: File, title: String, artist: String, bitrateKbps: Int = 320) {
        validateSource(src)
        LameBridge.ensureLoaded()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(src.absolutePath)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
                extractor.getTrackFormat(i)
                    .getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("Nessuna traccia audio nel file scaricato")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            extractor.selectTrack(trackIndex)

            val decoder = MediaCodec.createDecoderByType(mime)
            try {
                decoder.configure(inputFormat, null, null, 0)
                decoder.start()
                PcmPump.decodeToMp3(decoder, extractor, dst, title, artist, bitrateKbps)
            } finally {
                try {
                    decoder.stop()
                } catch (_: Exception) {
                }
                decoder.release()
            }
        } finally {
            extractor.release()
        }
    }

    private fun validateSource(src: File) {
        if (!src.exists() || src.length() < 32 * 1024) {
            throw IOException(
                "File audio scaricato troppo piccolo (${src.length()} byte): " +
                    "riprova il download",
            )
        }
    }
}
