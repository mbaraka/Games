package com.baraka.game

import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.os.Bundle
import android.view.WindowManager
import com.baraka.game.keyreader.KeyReaderActivity

/**
 * Keeps the screen awake while a game is open, so the display doesn't time out and lock while the
 * player is watching or thinking. The hub's game-picker and the Key Reader tool are left alone and
 * follow the normal screen timeout. The flag only holds while the activity is in the foreground.
 *
 * Debug builds only: also shows the game over the lock screen and dismisses it while the game is
 * in the foreground, so testing on a physical device doesn't mean repeatedly unlocking it. This is
 * cleared the moment the activity leaves the foreground, so the device locks normally again as
 * soon as you switch away - release builds never touch the keyguard.
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

            override fun onActivityResumed(activity: Activity) {
                if (BuildConfig.DEBUG && activity !is MainActivity && activity !is KeyReaderActivity) {
                    setShownOverKeyguard(activity, true)
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (BuildConfig.DEBUG && activity !is MainActivity && activity !is KeyReaderActivity) {
                    setShownOverKeyguard(activity, false)
                }
            }

            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun setShownOverKeyguard(activity: Activity, show: Boolean) {
        activity.setShowWhenLocked(show)
        activity.setTurnScreenOn(show)
        if (show) {
            activity.getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(activity, null)
        }
    }
}
