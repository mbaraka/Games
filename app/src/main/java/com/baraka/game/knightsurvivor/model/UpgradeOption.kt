package com.baraka.game.knightsurvivor.model

/**
 * One selectable choice offered on the level-up screen.
 *
 * systems/UpgradeChoiceSystem.kt decides which options are legal and rolls a set of
 * three; ui/LevelUpOverlay.kt only ever renders [title]/[description]/[colorArgb] and
 * hands the chosen option back to `UpgradeChoiceSystem.applyChoice()` — the UI layer
 * never needs to know about [WeaponKind]/[PassiveKind] internals directly.
 */
/**
 * Just enough to render a level-up card — [UpgradeOption] implements this directly;
 * knightsurvivor/coop/KnightCoopMsg.kt's UpgradeOptionDto implements it too, so
 * ui/LevelUpOverlay.kt can render either a real local choice or a co-op partner's choice
 * with the same composable. [icon] is nullable because it isn't sent over the co-op wire
 * (see UpgradeOptionDto) - a guest's view of the partner's choices falls back to no icon.
 */
interface UpgradeDisplay {
    val title: String
    val description: String
    val colorArgb: Long
    val icon: VectorIcon?
}

sealed class UpgradeOption : UpgradeDisplay {
    abstract override val title: String
    abstract override val description: String
    abstract override val colorArgb: Long
    abstract override val icon: VectorIcon?

    data class NewWeapon(val kind: WeaponKind) : UpgradeOption() {
        override val title: String = "New: ${kind.displayName}"
        override val description: String = kind.description
        override val colorArgb: Long = kind.colorArgb
        override val icon: VectorIcon = kind.icon
    }

    data class UpgradeWeapon(val kind: WeaponKind, val newLevel: Int) : UpgradeOption() {
        override val title: String = "${kind.displayName} -> Lv.$newLevel"
        override val description: String =
            "Damage ${kind.damageAtLevel(newLevel)} (was ${kind.damageAtLevel(newLevel - 1)})"
        override val colorArgb: Long = kind.colorArgb
        override val icon: VectorIcon = kind.icon
    }

    data class BoostPassive(val kind: PassiveKind, val newStackCount: Int) : UpgradeOption() {
        override val title: String = "${kind.displayName} ($newStackCount/${kind.maxStacks})"
        override val description: String = kind.description
        override val colorArgb: Long = 0xFF9E9E9EL
        override val icon: VectorIcon = kind.icon
    }
}
