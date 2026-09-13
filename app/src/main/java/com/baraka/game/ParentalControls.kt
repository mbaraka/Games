package com.baraka.game

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily play-time limit enforced at the Game Hub level: parents set a limit (and a PIN to
 * protect it), the hub tracks minutes played per calendar day, and blocks launching new games
 * once the limit is reached. Session time is measured by pairing a game launch (from
 * [MainActivity]) with the hub's next `onResume`, since every game here is a separate Activity
 * that returns to the hub when the child backs out.
 */
object ParentalControls {
    private const val PREFS_NAME = "parental_controls"
    private const val KEY_PIN = "pin"
    private const val KEY_DAILY_LIMIT_MINUTES = "daily_limit_minutes"
    private const val KEY_USAGE_DATE = "usage_date"
    private const val KEY_USAGE_MINUTES = "usage_minutes"
    private const val KEY_SESSION_START = "session_start_millis"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun isPinSet(context: Context): Boolean = prefs(context).contains(KEY_PIN)

    fun setPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun checkPin(context: Context, pin: String): Boolean =
        prefs(context).getString(KEY_PIN, null) == pin

    /** 0 means "no limit" (parental controls off). */
    fun getDailyLimitMinutes(context: Context): Int =
        prefs(context).getInt(KEY_DAILY_LIMIT_MINUTES, 0)

    fun setDailyLimitMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_DAILY_LIMIT_MINUTES, minutes.coerceAtLeast(0)).apply()
    }

    /** Minutes already played today, rolling the counter over on a new calendar day. */
    fun getMinutesUsedToday(context: Context): Int {
        val p = prefs(context)
        return if (p.getString(KEY_USAGE_DATE, null) == today()) {
            p.getInt(KEY_USAGE_MINUTES, 0)
        } else {
            0
        }
    }

    fun getRemainingMinutes(context: Context): Int? {
        val limit = getDailyLimitMinutes(context)
        if (limit <= 0) return null
        return (limit - getMinutesUsedToday(context)).coerceAtLeast(0)
    }

    fun isOverLimit(context: Context): Boolean = getRemainingMinutes(context) == 0

    fun resetUsageToday(context: Context) {
        prefs(context).edit()
            .putString(KEY_USAGE_DATE, today())
            .putInt(KEY_USAGE_MINUTES, 0)
            .apply()
    }

    fun addBonusMinutes(context: Context, minutes: Int) {
        val p = prefs(context)
        val used = getMinutesUsedToday(context)
        p.edit()
            .putString(KEY_USAGE_DATE, today())
            .putInt(KEY_USAGE_MINUTES, (used - minutes).coerceAtLeast(0))
            .apply()
    }

    /** Call right before launching a game Activity. */
    fun startSession(context: Context) {
        prefs(context).edit().putLong(KEY_SESSION_START, System.currentTimeMillis()).apply()
    }

    /** Call from the hub's onResume to fold the just-finished game session into today's usage. */
    fun endSession(context: Context) {
        val p = prefs(context)
        val start = p.getLong(KEY_SESSION_START, 0L)
        if (start <= 0L) return
        p.edit().remove(KEY_SESSION_START).apply()

        val elapsedMinutes = ((System.currentTimeMillis() - start) / 60_000L).toInt()
        if (elapsedMinutes <= 0) return

        val usedToday = getMinutesUsedToday(context)
        p.edit()
            .putString(KEY_USAGE_DATE, today())
            .putInt(KEY_USAGE_MINUTES, usedToday + elapsedMinutes)
            .apply()
    }
}
