package com.baraka.game.knightsurvivor.model

/**
 * A purely cosmetic choice of how the knight looks — no gameplay effect at all, so unlike
 * [Difficulty] or [WeaponKind] this never needs to flow through systems/. Picked in
 * Settings (see ui/SettingsOverlay.kt), persisted by data/SettingsRepository.kt, and
 * rendered by render/KnightRenderer.kt.
 */
enum class KnightDesign(val displayName: String, val description: String) {
    SWORN_BLADE("Sworn Blade", "Helmet, shield, and a sword held at the ready."),
    BERSERKER("Berserker", "No helmet, wild hair, war paint, and a two-handed axe raised overhead."),
    BATTLEMAGE("Battlemage", "A hooded robe and a glowing orb-topped staff."),
    GUNNER("Gunner", "Tactical helmet, flak vest, and a machine gun. Deliberately out of place.")
}
