package com.par9uet.jm.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.par9uet.jm.data.models.LauncherDisguise

object LauncherDisguiseApplier {

    private fun componentName(context: Context, aliasSimpleName: String): ComponentName =
        ComponentName(context.packageName, "${context.packageName}.$aliasSimpleName")

    private fun aliasFor(disguise: LauncherDisguise): String = when (disguise) {
        LauncherDisguise.DEFAULT -> "MainActivityDefault"
        LauncherDisguise.INNOCUOUS_LAUNCHER -> "MainActivityDisguiseLauncher"
        LauncherDisguise.INNOCUOUS_ROUND -> "MainActivityDisguiseRound"
    }

    fun apply(context: Context, disguise: LauncherDisguise) {
        val pm = context.packageManager
        val enabledAlias = aliasFor(disguise)
        val allAliases = listOf(
            "MainActivityDefault",
            "MainActivityDisguiseLauncher",
            "MainActivityDisguiseRound",
        )
        for (alias in allAliases) {
            val component = componentName(context, alias)
            val newState = if (alias == enabledAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                component,
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}