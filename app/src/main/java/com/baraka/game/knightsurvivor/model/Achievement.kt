package com.baraka.game.knightsurvivor.model

/**
 * A persistent milestone the player can earn once and keep forever, evaluated purely from
 * [GameState] by systems/AchievementSystem.kt and persisted by
 * data/AchievementRepository.kt. Adding a new one is a single enum entry plus (if it needs
 * a genuinely new signal) one more field/condition — the same low-friction pattern this
 * game already uses for enemies, weapons, and passives.
 */
enum class Achievement(val displayName: String, val description: String) {
    FIRST_BLOOD("First Blood", "Defeat your first enemy."),
    GETTING_STARTED("Getting Started", "Survive for 1 minute."),
    SEASONED_SURVIVOR("Seasoned Survivor", "Survive for 5 minutes."),
    CHAMPION("Champion", "Survive the full run and win."),
    GROWING_STRONG("Growing Strong", "Reach level 5 in a single run."),
    BATTLE_HARDENED("Battle Hardened", "Reach level 10 in a single run."),
    BOSS_SLAYER("Boss Slayer", "Defeat the Bone King."),
    CENTURY("Century", "Defeat 100 enemies in a single run."),
    FULL_ARSENAL("Full Arsenal", "Fill every weapon slot at once."),
    BATTLE_TESTED("Battle-Tested", "Win a run on Hard difficulty.")
}
