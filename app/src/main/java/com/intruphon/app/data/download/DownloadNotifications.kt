package com.intruphon.app.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.intruphon.app.R

/** Builds the progress notification (while downloading) and the finished/failed ones. */
internal object DownloadNotifications {
    private const val CHANNEL_PROGRESS = "progress"
    private const val CHANNEL_DONE = "done"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, context.getString(R.string.notif_channel_progress), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.notif_channel_done), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun progress(context: Context, active: List<DownloadItem>): Notification {
        createChannels(context)
        val title = when (active.size) {
            0 -> context.getString(R.string.app_name)
            1 -> active[0].record.title
            else -> context.getString(R.string.notif_downloading_many, active.size)
        }
        val total = active.sumOf { it.status.totalBytes }
        val done = active.sumOf { it.status.downloadedBytes }
        val percent = if (total > 0) (done * 100 / total).toInt().coerceIn(0, 100) else 0
        val cancelAll = PendingIntent.getService(
            context, 1,
            Intent(context, DownloadService::class.java).setAction(DownloadService.ACTION_CANCEL_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(context, CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(if (total > 0) "$percent%" else context.getString(R.string.downloads_status_waiting))
            .setProgress(100, percent, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(context))
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_download),
                    context.getString(R.string.notif_cancel_all),
                    cancelAll,
                ).build(),
            )
            .build()
    }

    /** Posted once when a download ends. Silently not shown if the user turned notifications off. */
    fun finished(context: Context, item: DownloadItem) {
        createChannels(context)
        val ok = item.status.state == DownloadState.DONE
        val notification = Notification.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(context.getString(if (ok) R.string.notif_done else R.string.notif_failed))
            .setContentText(item.record.title)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify((item.record.id % Int.MAX_VALUE).toInt(), notification)
    }

    private fun openApp(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
