package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.model.Achievement
import com.baraka.game.knightsurvivor.model.Difficulty
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.WeaponKind

/**
 * Decides which [Achievement]s the *current* [GameState] qualifies for — a pure read, not
 * an event stream. The caller (ui/KnightSurvivorScreen.kt) diffs this against whatever's
 * already persisted in data/AchievementRepository.kt to find newly-earned ones; this
 * system doesn't know or care what's already unlocked, so it stays trivially testable and
 * can be called as often as convenient (every tick is cheap — it's a handful of comparisons).
 */
object AchievementSystem {

    fun evaluate(state: GameState): Set<Achievement> = buildSet {
        if (state.killCount >= 1) add(Achievement.FIRST_BLOOD)
        if (state.survivalTimeMs >= ONE_MINUTE_MS) add(Achievement.GETTING_STARTED)
        if (state.survivalTimeMs >= FIVE_MINUTES_MS) add(Achievement.SEASONED_SURVIVOR)
        if (state.phase == GamePhase.VICTORY) add(Achievement.CHAMPION)
        if (state.player.level >= 5) add(Achievement.GROWING_STRONG)
        if (state.player.level >= 10) add(Achievement.BATTLE_HARDENED)
        if (state.bossDefeatedThisRun) add(Achievement.BOSS_SLAYER)
        if (state.killCount >= 100) add(Achievement.CENTURY)
        if (state.player.weapons.size >= WeaponKind.entries.size) add(Achievement.FULL_ARSENAL)
        if (state.difficulty == Difficulty.HARD && state.phase == GamePhase.VICTORY) add(Achievement.BATTLE_TESTED)
    }

    private const val ONE_MINUTE_MS = 60_000L
    private const val FIVE_MINUTES_MS = 5 * ONE_MINUTE_MS
}
