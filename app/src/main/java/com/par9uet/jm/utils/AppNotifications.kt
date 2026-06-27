package com.par9uet.jm.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.par9uet.jm.R

const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_progress"
const val COMIC_CACHE_NOTIFICATION_ID_BASE = 20_000
const val APP_UPDATE_NOTIFICATION_ID = 10_001

fun ensureAppNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        "下载进度",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "显示应用更新和漫画缓存下载进度"
        setSound(null, null)
    }
    manager.createNotificationChannel(channel)
}

fun showProgressNotification(
    context: Context,
    notificationId: Int,
    title: String,
    text: String,
    progressPercent: Int
) {
    if (!canPostNotification(context)) return
    val notification = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_download_notification)
        .setContentTitle(title)
        .setContentText(text)
        .setOnlyAlertOnce(true)
        .setOngoing(true)
        .setProgress(100, progressPercent.coerceIn(0, 100), false)
        .build()
    runCatching {
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

fun cancelProgressNotification(context: Context, notificationId: Int) {
    runCatching {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}

private fun canPostNotification(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}
