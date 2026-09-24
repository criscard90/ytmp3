package com.criscard90.ytmp3.audio

/**
 * Ponte JNI verso libmp3lame (LAME 3.100) compilata da noi con NDK.
 *
 * A differenza di ffmpeg-kit (libreria esterna che su alcune ROM non si
 * avvia), questa `.so` vive nel nostro package e usa solo bionic/libm:
 * il caricamento non dipende da estrazioni/configurazioni esterne.
 */
internal object LameBridge {

    private var loaded = false
    private var loadError: Throwable? = null

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        loadError?.let { throw it }
        try {
            System.loadLibrary("ytmp3lame")
            loaded = true
        } catch (t: Throwable) {
            loadError = t
            throw t
        }
    }

    @JvmStatic external fun nInit(
        inSampleRate: Int,
        numChannels: Int,
        outSampleRate: Int,
        bitrateKbps: Int,
        quality: Int,
    ): Long

    @JvmStatic external fun nEncodeInterleaved(
        handle: Long,
        pcm: ShortArray,
        numSamples: Int,
        mp3buf: ByteArray,
    ): Int

    @JvmStatic external fun nFlush(handle: Long, mp3buf: ByteArray): Int

    @JvmStatic external fun nClose(handle: Long)
}
