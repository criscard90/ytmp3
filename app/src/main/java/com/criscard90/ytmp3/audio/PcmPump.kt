package com.criscard90.ytmp3.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Pompa PCM dal `MediaCodec` verso LAME: gestisce il loop di decodifica,
 * l'inizializzazione lazy dell'encoder (serve il formato PCM reale) e il
 * flush finale. Supporta PCM 16-bit intero e PCM float.
 */
internal object PcmPump {

    fun decodeToMp3(
        decoder: MediaCodec,
        extractor: MediaExtractor,
        dst: File,
        title: String,
        artist: String,
        requestedBitrate: Int,
    ) {
        var handle = 0L
        var out: BufferedOutputStream? = null
        try {
            dst.parentFile?.mkdirs()
            if (dst.exists()) dst.delete()
            out = BufferedOutputStream(FileOutputStream(dst), 256 * 1024)
            Id3Writer.writeId3v23(out, title, artist)
            val stream = out

            var sampleRate = 44100
            var channels = 2
            var pcmEncoding = AudioFormat.ENCODING_PCM_16BIT
            var encoderReady = false
            var inputDone = false
            var inputEosQueued = false
            var outputDone = false
            var totalPcmFrames = 0L
            val mp3buf = ByteArray(128 * 1024)
            val info = MediaCodec.BufferInfo()

            // Il decoder può impiegare un po' a produrre il primo buffer:
            // limitiamo solo gli stalli *dopo* aver accodato tutto l'input.
            var stalledRounds = 0

            while (!outputDone) {
                if (!inputEosQueued) {
                    val inIndex = decoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inBuf = decoder.getInputBuffer(inIndex)!!
                        if (inputDone) {
                            decoder.queueInputBuffer(
                                inIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                            )
                            inputEosQueued = true
                        } else {
                            val sampleSize = extractor.readSampleData(inBuf, 0)
                            if (sampleSize < 0) {
                                inputDone = true
                                decoder.queueInputBuffer(
                                    inIndex, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputEosQueued = true
                            } else {
                                decoder.queueInputBuffer(
                                    inIndex, 0, sampleSize,
                                    extractor.sampleTime, extractor.sampleFlags,
                                )
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex >= 0 -> {
                        val outBuf = decoder.getOutputBuffer(outIndex)!!
                        if (info.size > 0) {
                            if (!encoderReady) {
                                val of = decoder.outputFormat
                                sampleRate = of.intOrDefault(
                                    MediaFormat.KEY_SAMPLE_RATE, sampleRate,
                                )
                                channels = of.intOrDefault(
                                    MediaFormat.KEY_CHANNEL_COUNT, channels,
                                ).coerceIn(1, 2)
                                pcmEncoding = of.intOrDefault(
                                    MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT,
                                )
                                val bitrate = if (channels == 1) {
                                    requestedBitrate.coerceAtMost(160)
                                } else {
                                    requestedBitrate
                                }
                                handle = LameBridge.nInit(
                                    sampleRate, channels, sampleRate, bitrate,
                                    2, // qualità alta (2 = alta, veloce)
                                )
                                if (handle == 0L) {
                                    throw IOException(
                                        "Inizializzazione encoder MP3 fallita",
                                    )
                                }
                                encoderReady = true
                            }
                            totalPcmFrames += PcmConvert.encodeBuffer(
                                outBuf, info, channels, pcmEncoding,
                                handle, mp3buf, stream,
                            )
                        }
                        val reachedEnd = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(outIndex, false)
                        stalledRounds = 0
                        if (reachedEnd) outputDone = true
                    }
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        // Uscita prematura solo se l'input è già tutto accodato
                        // E il decoder continua a non produrre nulla.
                        if (inputEosQueued && totalPcmFrames > 0) {
                            if (++stalledRounds >= 200) outputDone = true
                        }
                    }
                    else -> {
                        // INFO_OUTPUT_FORMAT_CHANGED e simili: azzera lo stallo.
                        stalledRounds = 0
                    }
                }
                if (dst.length() > 500L * 1024 * 1024) {
                    throw IOException("Output MP3 oltre il limite (500 MB)")
                }
            }

            if (!encoderReady || totalPcmFrames == 0L) {
                throw IOException("Decodifica audio fallita: nessun campione PCM")
            }
            while (true) {
                val n = LameBridge.nFlush(handle, mp3buf)
                if (n < 0) throw IOException("Flush encoder MP3 fallito")
                if (n == 0) break
                stream.write(mp3buf, 0, n)
            }
            stream.flush()
        } finally {
            if (handle != 0L) {
                try {
                    LameBridge.nClose(handle)
                } catch (_: Exception) {
                }
            }
            try {
                out?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun MediaFormat.intOrDefault(key: String, default: Int): Int {
        return try {
            if (containsKey(key)) getInteger(key) else default
        } catch (_: Exception) {
            default
        }
    }
}
