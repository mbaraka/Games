package com.baraka.game.knightsurvivor.model

/**
 * One selectable choice offered on the level-up screen.
 *
 * systems/UpgradeChoiceSystem.kt decides which options are legal and rolls a set of
 * three; ui/LevelUpOverlay.kt only ever renders [title]/[description]/[colorArgb] and
 * hands the chosen option back to `UpgradeChoiceSystem.applyChoice()` — the UI layer
 * never needs to know about [WeaponKind]/[PassiveKind] internals directly.
 */
sealed class UpgradeOption {
    abstract val title: String
    abstract val description: String
    abstract val colorArgb: Long

    data class NewWeapon(val kind: WeaponKind) : UpgradeOption() {
        override val title: String = "New: ${kind.displayName}"
        override val description: String = kind.description
        override val colorArgb: Long = kind.colorArgb
    }

    data class UpgradeWeapon(val kind: WeaponKind, val newLevel: Int) : UpgradeOption() {
        override val title: String = "${kind.displayName} -> Lv.$newLevel"
        override val description: String =
            "Damage ${kind.damageAtLevel(newLevel)} (was ${kind.damageAtLevel(newLevel - 1)})"
        override val colorArgb: Long = kind.colorArgb
    }

    data class BoostPassive(val kind: PassiveKind, val newStackCount: Int) : UpgradeOption() {
        override val title: String = "${kind.displayName} ($newStackCount/${kind.maxStacks})"
        override val description: String = kind.description
        override val colorArgb: Long = 0xFF9E9E9EL
    }
}
