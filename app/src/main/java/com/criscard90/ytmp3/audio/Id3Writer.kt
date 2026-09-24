package com.criscard90.ytmp3.audio

import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Scrive un tag ID3v2.3 minimale (TIT2 + TPE1, UTF-16 con BOM). */
internal object Id3Writer {

    fun writeId3v23(out: BufferedOutputStream, title: String, artist: String) {
        val frames = mutableListOf<ByteArray>()
        if (title.isNotEmpty()) frames += textFrame("TIT2", title)
        if (artist.isNotEmpty()) frames += textFrame("TPE1", artist)
        if (frames.isEmpty()) return
        val bodySize = frames.sumOf { it.size }
        val header = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN)
        header.put("ID3".toByteArray(Charsets.ISO_8859_1))
        header.put(3) // versione 2.3
        header.put(0) // revisione
        header.put(0) // flags
        header.put(syncSafe(bodySize))
        out.write(header.array())
        frames.forEach { out.write(it) }
    }

    private fun textFrame(id: String, text: String): ByteArray {
        // 1 byte encoding (1 = UTF-16 con BOM) + BOM + testo UTF-16LE + fine.
        val payload = byteArrayOf(1, 0xFF.toByte(), 0xFE.toByte()) +
            text.toByteArray(Charsets.UTF_16LE) + byteArrayOf(0, 0)
        val header = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN)
        header.put(id.toByteArray(Charsets.ISO_8859_1))
        header.putInt(payload.size)
        header.putShort(0)
        return header.array() + payload
    }

    private fun syncSafe(value: Int): ByteArray {
        return byteArrayOf(
            ((value shr 21) and 0x7F).toByte(),
            ((value shr 14) and 0x7F).toByte(),
            ((value shr 7) and 0x7F).toByte(),
            (value and 0x7F).toByte(),
        )
    }
}
