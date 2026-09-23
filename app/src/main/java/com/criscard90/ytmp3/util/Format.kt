package com.criscard90.ytmp3.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Rimuove i caratteri non validi nei nomi file e limita la lunghezza.
 */
fun sanitizeFileName(name: String): String {
    val cleaned = name
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('.')
        .take(80)
    return cleaned.ifEmpty { "audio" }
}

/** Formatta un size in byte in forma leggibile ("4,3 MB"). */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.getDefault(), "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

/** Formatta una data epoch-second in "dd/MM/yyyy HH:mm". */
fun formatDate(epochSeconds: Long): String {
    val pattern = if (Locale.getDefault().language == "it") "dd/MM/yyyy HH:mm" else "MM/dd/yyyy HH:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochSeconds * 1000))
}
