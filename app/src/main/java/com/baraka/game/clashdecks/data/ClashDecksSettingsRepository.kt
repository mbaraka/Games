package com.baraka.game.clashdecks.data

import android.content.Context
import android.content.SharedPreferences

/** Persists the sound on/off preference — same SharedPreferences pattern as roadrush's RoadRushSettingsRepository / memorymatch's MemoryMatchSettingsRepository. */
class ClashDecksSettingsRepository private constructor(
    private val preferences: SharedPreferences
) {
    fun isSoundEnabled(): Boolean = preferences.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    /** What a two-phone co-op partner sees you as (DuelEngine.startDuel's playerName). Empty until the player picks one. */
    fun playerName(): String = preferences.getString(KEY_PLAYER_NAME, null).orEmpty()

    fun setPlayerName(name: String) {
        preferences.edit().putString(KEY_PLAYER_NAME, name).apply()
    }

    /** Fewest rounds a single-player duel has ever been won in. 0 means no win recorded yet. */
    fun bestWinRounds(): Int = preferences.getInt(KEY_BEST_WIN_ROUNDS, 0)

    fun setBestWinRounds(rounds: Int) {
        preferences.edit().putInt(KEY_BEST_WIN_ROUNDS, rounds).apply()
    }

    /** How far through `CardLibrary.unlockOrder` single-player wins have gotten — every Common is unlocked from the start regardless. */
    fun unlockedNonCommonCount(): Int = preferences.getInt(KEY_UNLOCKED_COUNT, 0)

    fun setUnlockedNonCommonCount(count: Int) {
        preferences.edit().putInt(KEY_UNLOCKED_COUNT, count).apply()
    }

    /** The player's built deck (card ids), or empty if they've never opened the deck builder — callers fall back to "everything unlocked" in that case. */
    fun selectedDeckCardIds(): Set<String> {
        val raw = preferences.getString(KEY_SELECTED_DECK, null) ?: return emptySet()
        return raw.split(',').filter { it.isNotBlank() }.toSet()
    }

    fun setSelectedDeckCardIds(ids: Set<String>) {
        preferences.edit().putString(KEY_SELECTED_DECK, ids.joinToString(",")).apply()
    }

    companion object {
        private const val PREF_FILE = "clash_decks_settings"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_BEST_WIN_ROUNDS = "best_win_rounds"
        private const val KEY_UNLOCKED_COUNT = "unlocked_non_common_count"
        private const val KEY_SELECTED_DECK = "selected_deck_card_ids"

        @Volatile
        private var instance: ClashDecksSettingsRepository? = null

        fun getInstance(context: Context): ClashDecksSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: ClashDecksSettingsRepository(
                    context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                ).also { instance = it }
            }
        }
    }
}
