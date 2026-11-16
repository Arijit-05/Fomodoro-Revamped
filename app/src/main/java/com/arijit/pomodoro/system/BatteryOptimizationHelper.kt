package com.arijit.pomodoro.system

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
        return true
    }

    @SuppressLint("BatteryLife")
    fun createRequestIgnoreBatteryOptimizationsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        return null
    }

    fun createOemAutostartIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()

        val intents = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> listOf(
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.securityscan.MainActivity"))
            )
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> listOf(
                Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
                Intent().setComponent(ComponentName("com.oplus.battery", "com.oplus.battery.BatteryActivity")),
                Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"))
            )
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> listOf(
                Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
                Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
            )
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> listOf(
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
                Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity"))
            )
            manufacturer.contains("samsung") -> listOf(
                Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
                Intent().setComponent(ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"))
            )
            else -> emptyList()
        }

        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        }

        // Generic fallback to battery settings
        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return if (context.packageManager.resolveActivity(fallback, 0) != null) fallback else null
    }

    fun getDontKillMyAppUri(): Uri {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val path = when {
            manufacturer.contains("xiaomi") -> "xiaomi"
            manufacturer.contains("samsung") -> "samsung"
            manufacturer.contains("oneplus") -> "oneplus"
            manufacturer.contains("huawei") -> "huawei"
            manufacturer.contains("oppo") -> "oppo"
            manufacturer.contains("vivo") -> "vivo"
            manufacturer.contains("realme") -> "realme"
            else -> ""
        }
        return Uri.parse("https://dontkillmyapp.com/$path")
    }
}

