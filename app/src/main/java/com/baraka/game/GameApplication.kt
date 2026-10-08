package com.baraka.game

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.WindowManager
import com.baraka.game.keyreader.KeyReaderActivity

/**
 * Keeps the screen awake while a game is open, so the display doesn't time out and lock while the
 * player is watching or thinking. The hub's game-picker and the Key Reader tool are left alone and
 * follow the normal screen timeout. The flag only holds while the activity is in the foreground.
 */
class GameApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is MainActivity || activity is KeyReaderActivity) return
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
