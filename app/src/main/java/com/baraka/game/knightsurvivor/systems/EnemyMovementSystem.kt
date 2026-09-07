package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.Vec2

/**
 * Steers every enemy each tick. Every current [com.baraka.game.knightsurvivor.model.EnemyKind]
 * shares the same "walk straight at the player" behavior; a future kind needing different AI
 * (e.g. a ranged enemy that keeps its distance) adds one `when(enemy.kind)` branch here,
 * without touching spawning, collision, or rendering.
 */
object EnemyMovementSystem {

    fun step(enemies: List<EnemyInstance>, playerPosition: Vec2, dtSeconds: Float): List<EnemyInstance> =
        enemies.map { enemy ->
            val direction = (playerPosition - enemy.position).normalized()
            enemy.copy(position = enemy.position + direction * enemy.speed * dtSeconds)
        }
}
