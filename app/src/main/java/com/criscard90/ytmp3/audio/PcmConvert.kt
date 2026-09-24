package com.criscard90.ytmp3.audio

import android.media.AudioFormat
import android.media.MediaCodec
import java.io.BufferedOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Converte i buffer PCM del decoder in short interleaved e li passa a LAME.
 * Gestisce PCM 16-bit intero e PCM float (convertito con clipping in [-1, 1]).
 */
internal object PcmConvert {

    /** Codifica un buffer e restituisce i frame PCM (per canale) elaborati. */
    fun encodeBuffer(
        buf: ByteBuffer,
        info: MediaCodec.BufferInfo,
        channels: Int,
        pcmEncoding: Int,
        handle: Long,
        mp3buf: ByteArray,
        out: BufferedOutputStream,
    ): Long {
        val pcm = toInterleavedShorts(buf, info, pcmEncoding)
        if (pcm.isEmpty()) return 0L
        val numSamples = pcm.size / channels // campioni *per canale*
        if (numSamples <= 0) return 0L

        var offset = 0
        while (offset < numSamples) {
            val chunk = minOf(8192, numSamples - offset)
            val slice = if (offset == 0 && chunk == numSamples) {
                pcm
            } else {
                pcm.copyOfRange(offset * channels, (offset + chunk) * channels)
            }
            val encoded = LameBridge.nEncodeInterleaved(handle, slice, chunk, mp3buf)
            if (encoded < 0) throw IOException("Codifica MP3 fallita")
            if (encoded > 0) out.write(mp3buf, 0, encoded)
            offset += chunk
        }
        return numSamples.toLong()
    }

    private fun toInterleavedShorts(
        buf: ByteBuffer,
        info: MediaCodec.BufferInfo,
        pcmEncoding: Int,
    ): ShortArray {
        val dup = buf.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        return if (pcmEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
            val floats = FloatArray(info.size / 4)
            dup.asFloatBuffer().get(floats)
            ShortArray(floats.size) { i ->
                (floats[i].coerceIn(-1f, 1f) * 32767f).roundToInt().toShort()
            }
        } else {
            val shorts = ShortArray(info.size / 2)
            dup.asShortBuffer().get(shorts)
            shorts
        }
    }
}
