package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.PassiveKind
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.UpgradeOption
import com.baraka.game.knightsurvivor.model.WeaponInstance
import com.baraka.game.knightsurvivor.model.WeaponKind
import kotlin.random.Random

/**
 * Rolls the level-up choices and applies whichever one the player picks.
 *
 * The pool is simply every currently-legal option — an unowned weapon from the player's
 * chosen [weaponDeck] while there's a free slot, an owned weapon below its max level, a
 * passive below its max stacks — uniformly shuffled down to
 * [GameTunables.UPGRADE_CHOICES_PER_LEVEL_UP]. No separate weighting table to keep in
 * sync: the pool construction already guarantees at most one entry per kind, so there's
 * nothing to weight.
 */
object UpgradeChoiceSystem {

    fun rollChoices(player: PlayerState, weaponDeck: Set<WeaponKind>, random: Random = Random.Default): List<UpgradeOption> {
        val pool = mutableListOf<UpgradeOption>()

        if (player.weapons.size < GameTunables.MAX_ACTIVE_WEAPONS) {
            weaponDeck
                .filter { kind -> player.weaponOf(kind) == null }
                .forEach { kind -> pool += UpgradeOption.NewWeapon(kind) }
        }

        player.weapons
            .filter { instance -> instance.level < instance.kind.maxLevel }
            .forEach { instance -> pool += UpgradeOption.UpgradeWeapon(instance.kind, instance.level + 1) }

        PassiveKind.entries
            .filter { kind -> player.passiveStackCount(kind) < kind.maxStacks }
            .forEach { kind -> pool += UpgradeOption.BoostPassive(kind, player.passiveStackCount(kind) + 1) }

        return pool.shuffled(random).take(GameTunables.UPGRADE_CHOICES_PER_LEVEL_UP)
    }

    fun applyChoice(player: PlayerState, choice: UpgradeOption): PlayerState = when (choice) {
        is UpgradeOption.NewWeapon ->
            player.copy(weapons = player.weapons + WeaponInstance(kind = choice.kind, level = 1))

        is UpgradeOption.UpgradeWeapon ->
            player.copy(
                weapons = player.weapons.map { instance ->
                    if (instance.kind == choice.kind) instance.copy(level = choice.newLevel) else instance
                }
            )

        is UpgradeOption.BoostPassive -> {
            // Heal by exactly however much this stack raised the max, rather than assuming
            // a flat amount — stays correct even if PassiveKind.magnitudeAtStacks() ever
            // becomes non-linear.
            val healedHp = if (choice.kind == PassiveKind.MAX_HP) {
                (choice.kind.magnitudeAtStacks(choice.newStackCount) - choice.kind.magnitudeAtStacks(choice.newStackCount - 1))
                    .toInt()
            } else {
                0
            }
            val newMaxHp = player.effectiveMaxHp + healedHp
            player.copy(
                passiveStacks = player.passiveStacks + (choice.kind to choice.newStackCount),
                hp = (player.hp + healedHp).coerceAtMost(newMaxHp)
            )
        }
    }
}
