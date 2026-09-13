package com.baraka.game

import android.content.Context

/**
 * Tracks per-game launch counts so the home screen can surface a "Most Played" section.
 * Keyed by [GameCatalogEntry.id] rather than any enum/ordinal so entries can be added,
 * removed, or reordered in [GAME_CATALOG] without invalidating stored counts.
 */
object GamePlayStats {
    private const val PREFS_NAME = "game_play_stats"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordPlay(context: Context, gameId: String) {
        val p = prefs(context)
        p.edit().putInt(gameId, p.getInt(gameId, 0) + 1).apply()
    }

    fun getCounts(context: Context, gameIds: List<String>): Map<String, Int> {
        val p = prefs(context)
        return gameIds.associateWith { p.getInt(it, 0) }
    }
}
