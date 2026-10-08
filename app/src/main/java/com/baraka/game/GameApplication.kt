package com.baraka.game

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager

/**
 * Keeps the screen awake in every screen of the app (hub and all games), so the display doesn't
 * time out and lock while the player is watching or thinking. The flag only holds while the
 * activity is in the foreground; the normal screen timeout applies again once the app is left.
 */
class GameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
