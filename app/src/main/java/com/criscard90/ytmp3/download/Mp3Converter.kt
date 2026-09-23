package com.criscard90.ytmp3.download

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.IOException

/**
 * Conversione audio sorgente → MP3 320 kbps con metadata ID3
 * usando FFmpeg (ffmpeg-kit, encoder libmp3lame).
 */
object Mp3Converter {

    fun convert(src: File, dst: File, title: String, channel: String) {
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
}
