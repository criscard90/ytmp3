package com.criscard90.ytmp3.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Apre un video su YouTube: prima cerca l'app YouTube, in alternativa il browser.
 */
object YouTubeOpener {

    fun open(context: Context, videoId: String) {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl))
            .setPackage("com.google.android.youtube")
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(watchUrl))

        try {
            context.startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(webIntent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "Nessuna app per aprire il video", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}
