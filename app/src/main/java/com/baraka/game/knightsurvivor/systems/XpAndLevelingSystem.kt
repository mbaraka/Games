package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.XpGem

/** Outcome of one tick of XP magnetism/pickup/leveling, for GameLoopOrchestrator to fold in. */
data class XpStepResult(
    val player: PlayerState,
    val xpGems: List<XpGem>,
    val leveledUp: Boolean
)

/**
 * Pulls nearby gems toward the player, collects the ones close enough, and rolls the
 * player's level up (possibly more than once from a single big XP drop, e.g. a boss kill)
 * using [GameTunables.xpRequiredForLevel]. Never decides WHAT the level-up offers — that's
 * systems/UpgradeChoiceSystem.kt's job; this only flags [XpStepResult.leveledUp] so
 * GameLoopOrchestrator knows to ask for choices.
 *
 * Known v1 simplification: gaining several levels from one XP step still only pauses the
 * game for a single round of upgrade choices, not one per level crossed. Easy to change
 * later (e.g. a `pendingLevelUps` counter) without touching any other system.
 */
object XpAndLevelingSystem {

    fun step(player: PlayerState, xpGems: List<XpGem>, dtSeconds: Float): XpStepResult {
        val pickupRadius = GameTunables.PLAYER_BASE_PICKUP_RADIUS * player.pickupRadiusMultiplier
        val magnetSpeed = GameTunables.XP_GEM_MAGNET_SPEED_PX_PER_SEC

        var xpGained = 0
        val remainingGems = mutableListOf<XpGem>()

        for (gem in xpGems) {
            val distance = gem.position.distanceTo(player.position)
            val magnetized = gem.magnetized || distance <= pickupRadius

            if (magnetized && distance <= GameTunables.XP_GEM_COLLECT_DISTANCE) {
                xpGained += gem.value
                continue
            }

            val newPosition = if (magnetized) {
                gem.position + (player.position - gem.position).normalized() * magnetSpeed * dtSeconds
            } else {
                gem.position
            }
            remainingGems += gem.copy(position = newPosition, magnetized = magnetized)
        }

        if (xpGained == 0) {
            return XpStepResult(player, remainingGems, leveledUp = false)
        }

        var xp = player.xp + xpGained
        var level = player.level
        var xpToNextLevel = player.xpToNextLevel
        var leveledUp = false

        while (xp >= xpToNextLevel) {
            xp -= xpToNextLevel
            level += 1
            xpToNextLevel = GameTunables.xpRequiredForLevel(level)
            leveledUp = true
        }

        val updatedPlayer = player.copy(xp = xp, level = level, xpToNextLevel = xpToNextLevel)
        return XpStepResult(updatedPlayer, remainingGems, leveledUp)
    }
}
