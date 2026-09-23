package com.criscard90.ytmp3.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.criscard90.ytmp3.MainActivity
import com.criscard90.ytmp3.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service che mantiene vivo il processo durante download/conversione
 * e mostra la notifica con il progresso.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initial = DownloadManager.tasks.value
        foreground(initial)

        if (collectJob == null) {
            collectJob = scope.launch {
                DownloadManager.tasks.collect { tasks -> update(tasks) }
            }
        }
        return START_NOT_STICKY
    }

    private fun update(tasks: List<DownloadTask>) {
        val active = tasks.firstOrNull { it.state.isActive }
        if (active != null) {
            foreground(tasks)
        } else {
            // Nessun lavoro attivo: notifica finale e arresto
            tasks.lastOrNull()?.let { notify(buildNotification(it, ongoing = false)) }
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun foreground(tasks: List<DownloadTask>) {
        val current = tasks.firstOrNull { it.state.isActive } ?: tasks.lastOrNull()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(current, ongoing = current?.state?.isActive == true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun buildNotification(task: DownloadTask?, ongoing: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setColor(0xFFFF0000.toInt())
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(ongoing)

        when (val state = task?.state) {
            is DownloadState.Resolving -> builder
                .setContentTitle("Preparazione download…")
                .setContentText(task.title)
                .setProgress(0, 0, true)
            is DownloadState.Downloading -> builder
                .setContentTitle(task.title)
                .setContentText(
                    if (state.progress >= 0) "Download audio · ${state.progress}%"
                    else "Download audio…"
                )
                .setProgress(100, state.progress.coerceAtLeast(0), state.progress < 0)
            is DownloadState.Converting -> builder
                .setContentTitle(task.title)
                .setContentText("Conversione in MP3 (320 kbps)…")
                .setProgress(0, 0, true)
            is DownloadState.Done -> builder
                .setContentTitle("Download completato")
                .setContentText(state.fileName)
                .setAutoCancel(true)
                .setOngoing(false)
            is DownloadState.Failed -> builder
                .setContentTitle("Download non riuscito")
                .setContentText(state.message.take(120))
                .setAutoCancel(true)
                .setOngoing(false)
            null -> builder
                .setContentTitle("Download ytmp3")
                .setAutoCancel(true)
                .setOngoing(false)
        }
        return builder.build()
    }

    private fun notify(notification: Notification) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Progresso dei download MP3" }
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        collectJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 1001

        /** Avvia il service (da chiamare da un contesto in primo piano). */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            context.startForegroundService(intent)
        }
    }
}
