package com.arijit.pomodoro.system

import android.app.Activity
import android.view.WindowManager

object WakeLockManager {

    fun setScreenKeepOn(activity: Activity, keepOn: Boolean) {
        if (keepOn) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

