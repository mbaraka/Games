package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.HealthPickup
import com.baraka.game.knightsurvivor.model.PlayerState

/** Outcome of one tick of heal-pickup magnetism/collection. Same shape as [XpStepResult] since the pickup behavior mirrors [XpAndLevelingSystem]'s. */
data class HealthPickupStepResult(val player: PlayerState, val pickups: List<HealthPickup>)

/** Pulls nearby heal pickups toward the player and applies healing once collected, capped at [PlayerState.effectiveMaxHp]. */
object HealthPickupSystem {

    fun step(player: PlayerState, pickups: List<HealthPickup>, dtSeconds: Float): HealthPickupStepResult {
        val pickupRadius = GameTunables.PLAYER_BASE_PICKUP_RADIUS * player.pickupRadiusMultiplier
        val magnetSpeed = GameTunables.XP_GEM_MAGNET_SPEED_PX_PER_SEC

        var healed = 0
        val remaining = mutableListOf<HealthPickup>()

        for (pickup in pickups) {
            val distance = pickup.position.distanceTo(player.position)
            val magnetized = pickup.magnetized || distance <= pickupRadius

            if (magnetized && distance <= GameTunables.XP_GEM_COLLECT_DISTANCE) {
                healed += pickup.healAmount
                continue
            }

            val newPosition = if (magnetized) {
                pickup.position + (player.position - pickup.position).normalized() * magnetSpeed * dtSeconds
            } else {
                pickup.position
            }
            remaining += pickup.copy(position = newPosition, magnetized = magnetized)
        }

        val updatedPlayer = if (healed > 0) player.copy(hp = (player.hp + healed).coerceAtMost(player.effectiveMaxHp)) else player
        return HealthPickupStepResult(updatedPlayer, remaining)
    }
}
