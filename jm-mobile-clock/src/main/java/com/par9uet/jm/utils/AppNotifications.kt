package com.par9uet.jm.utils

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.par9uet.jm.MainActivity
import com.par9uet.jm.R

const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_progress"
const val UPDATE_DOWNLOADED_CHANNEL_ID = "update_downloaded"
const val COMIC_CACHE_NOTIFICATION_ID_BASE = 20_000
const val APP_UPDATE_NOTIFICATION_ID = 10_001
const val APP_UPDATE_PENDING_INTENT_REQUEST_CODE = 10_101
const val EXTRA_NAVIGATE_ROUTE = "navigate_route"
const val NAVIGATE_ROUTE_CHECK_UPDATE = "checkUpdate"

fun ensureAppNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    val progressChannel = NotificationChannel(
        DOWNLOAD_NOTIFICATION_CHANNEL_ID,
        "Download progress",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Shows app update and comic cache download progress"
        setSound(null, null)
    }
    manager.createNotificationChannel(progressChannel)
    val updateChannel = NotificationChannel(
        UPDATE_DOWNLOADED_CHANNEL_ID,
        "Update downloaded",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "Notifies you when an update package has been downloaded"
        enableVibration(true)
    }
    manager.createNotificationChannel(updateChannel)
}

/**
 * 下载完成通知：点击后打开 MainActivity 并携带 [EXTRA_NAVIGATE_ROUTE] = checkUpdate，
 * 由 AppScreen 读取后导航到检查更新页面。
 */
@SuppressLint("MissingPermission")
fun showUpdateDownloadedNotification(
    context: Context,
    version: String,
    savedPath: String,
) {
    if (!canPostNotification(context)) return
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_NAVIGATE_ROUTE, NAVIGATE_ROUTE_CHECK_UPDATE)
        putExtra(EXTRA_UPDATE_SAVED_PATH, savedPath)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        APP_UPDATE_PENDING_INTENT_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val notification = NotificationCompat.Builder(context, UPDATE_DOWNLOADED_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_download_notification)
        .setContentTitle("更新包已下载完成")
        .setContentText("v$version 已就绪，点击前往安装")
        .setAutoCancel(true)
        .setOngoing(false)
        .setContentIntent(pendingIntent)
        .build()
    runCatching {
        NotificationManagerCompat.from(context).notify(APP_UPDATE_NOTIFICATION_ID, notification)
    }
}

const val EXTRA_UPDATE_SAVED_PATH = "update_saved_path"

@SuppressLint("MissingPermission")
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
