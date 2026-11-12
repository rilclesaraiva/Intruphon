package com.intruphon.app.data.download

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process alive while downloads run and shows the progress
 * notification. It stops itself when nothing is left to download.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, DownloadNotifications.progress(this, activeItems()), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        scope.launch {
            combine(DownloadEngine.items, DownloadEngine.reading) { items, reading -> items to reading }.collect { (items, reading) ->
                val active = items.filter { it.status.state.active }
                if (active.isEmpty() && reading) {
                    // Playlist items are still being read: stay up, the next one is about to be queued.
                } else if (active.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    getSystemService(android.app.NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, DownloadNotifications.progress(this@DownloadService, active))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_ALL) scope.launch(Dispatchers.IO) { DownloadEngine.cancelAll() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun activeItems() =
        DownloadEngine.items.value.filter { it.status.state.active }

    companion object {
        const val ACTION_CANCEL_ALL = "com.intruphon.app.CANCEL_ALL"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            // Android 12+ refuses to start a foreground service from the background; the one already running keeps going.
            runCatching { ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java)) }
        }
    }
}
