package com.par9uet.jm.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.par9uet.jm.data.models.LauncherDisguise

class LauncherDisguiseApplier(
    private val context: Context,
) {
    fun apply(disguise: LauncherDisguise) {
        val packageManager = context.packageManager
        // Manifest aliases are resolved from the app namespace (com.par9uet.jm),
        // which can differ from the runtime applicationId (jmcomicoi.net).
        // ApplicationInfo.className is available on Android 6 and avoids Class.getPackageName().
        val componentClassPrefix = context.applicationInfo.className
            ?.substringBeforeLast('.', context.packageName)
        LauncherDisguise.entries.forEach { item ->
            runCatching {
                packageManager.setComponentEnabledSetting(
                    ComponentName(context.packageName, "$componentClassPrefix${item.aliasClassName}"),
                    if (item == disguise) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP
                )
            }.onFailure {
                log("切换桌面图标入口失败：${item.id}，原因：${it.message}")
            }
        }
    }
}
