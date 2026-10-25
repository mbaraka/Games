package com.baraka.game

/** A broad category a game in the hub belongs to, used only to filter [GAME_CATALOG] in the hub UI. */
enum class GameGenre(val label: String) {
    ARCADE("Arcade"),
    SHOOTER("Shooter"),
    PUZZLE("Puzzle"),
    STRATEGY("Strategy"),
    BOARD("Board"),
    CARD("Card"),
    SURVIVAL("Survival"),
    RACING("Racing"),
    ACTION("Action"),
    UTILITY("Utility")
}
