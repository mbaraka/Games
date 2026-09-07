package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.DamageEvent
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.EntityIdGenerator
import com.baraka.game.knightsurvivor.model.HitFeedback
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Projectile
import com.baraka.game.knightsurvivor.model.XpGem

/** Everything one tick of collision resolution produced, for GameLoopOrchestrator to fold into the next GameState. */
data class CollisionResult(
    val enemies: List<EnemyInstance>,
    val projectiles: List<Projectile>,
    val newXpGems: List<XpGem>,
    val killsThisTick: Int,
    val player: PlayerState,
    /** An enemy that took damage and survived — GameLoopOrchestrator turns each into a small spark burst. */
    val hitFeedback: List<HitFeedback> = emptyList(),
    /** An enemy whose HP reached zero — GameLoopOrchestrator turns each into a bigger death burst. */
    val deathFeedback: List<HitFeedback> = emptyList()
)

/**
 * The one place damage is actually applied. Resolves projectile-vs-enemy hits, folds in
 * any [DamageEvent]s handed over by systems/WeaponFiringSystem.kt (AoE/orbit weapons have
 * no projectile of their own), kills enemies whose HP drops to zero and drops their XP,
 * then resolves enemy-vs-player contact damage respecting the player's invulnerability
 * window. Keeping all of this in one file means "how much damage did enemy X take this
 * tick" never has to be pieced together from two different systems.
 */
object CollisionSystem {

    fun resolve(
        enemies: List<EnemyInstance>,
        projectiles: List<Projectile>,
        player: PlayerState,
        externalDamageEvents: List<DamageEvent>,
        nowMs: Long
    ): CollisionResult {
        val (survivingProjectiles, projectileDamageEvents) = resolveProjectileHits(projectiles, enemies)

        val totalDamageByEnemyId = (projectileDamageEvents + externalDamageEvents)
            .groupingBy { it.targetEnemyId }
            .fold(0) { accumulated, event -> accumulated + event.amount }

        val survivingEnemies = mutableListOf<EnemyInstance>()
        val newXpGems = mutableListOf<XpGem>()
        val hitFeedback = mutableListOf<HitFeedback>()
        val deathFeedback = mutableListOf<HitFeedback>()
        var kills = 0

        for (enemy in enemies) {
            val damage = totalDamageByEnemyId[enemy.id] ?: 0
            val newHp = enemy.hp - damage
            if (newHp <= 0) {
                kills++
                newXpGems += XpGem(id = EntityIdGenerator.next(), position = enemy.position, value = enemy.kind.xpValue)
                deathFeedback += HitFeedback(enemy.position, enemy.kind.colorArgb)
            } else {
                survivingEnemies += enemy.copy(
                    hp = newHp,
                    hitFlashUntilMs = if (damage > 0) nowMs + HIT_FLASH_MS else enemy.hitFlashUntilMs
                )
                if (damage > 0) hitFeedback += HitFeedback(enemy.position, enemy.kind.colorArgb)
            }
        }

        val updatedPlayer = resolvePlayerContactDamage(player, survivingEnemies, nowMs)

        return CollisionResult(
            enemies = survivingEnemies,
            projectiles = survivingProjectiles,
            newXpGems = newXpGems,
            killsThisTick = kills,
            player = updatedPlayer,
            hitFeedback = hitFeedback,
            deathFeedback = deathFeedback
        )
    }

    private fun resolveProjectileHits(
        projectiles: List<Projectile>,
        enemies: List<EnemyInstance>
    ): Pair<List<Projectile>, List<DamageEvent>> {
        val survivingProjectiles = mutableListOf<Projectile>()
        val damageEvents = mutableListOf<DamageEvent>()

        for (projectile in projectiles) {
            val hitEnemy = enemies.firstOrNull { enemy ->
                enemy.id !in projectile.hitEnemyIds &&
                    projectile.position.distanceTo(enemy.position) <= enemy.radius + PROJECTILE_HIT_RADIUS
            }

            if (hitEnemy == null) {
                survivingProjectiles += projectile
                continue
            }

            damageEvents += DamageEvent(hitEnemy.id, projectile.damage, projectile.sourceWeapon)

            // pierceRemaining counts how many MORE enemies this projectile may hit after this one.
            val updatedProjectile = projectile.copy(
                hitEnemyIds = projectile.hitEnemyIds + hitEnemy.id,
                pierceRemaining = projectile.pierceRemaining - 1
            )
            if (updatedProjectile.pierceRemaining >= 0) {
                survivingProjectiles += updatedProjectile
            }
        }

        return survivingProjectiles to damageEvents
    }

    private fun resolvePlayerContactDamage(player: PlayerState, enemies: List<EnemyInstance>, nowMs: Long): PlayerState {
        if (player.isInvulnerableAt(nowMs)) return player

        // If several enemies touch the player in the same tick, only the hardest hit lands —
        // the invulnerability window that follows would have absorbed the rest anyway.
        val touchingEnemy = enemies
            .filter { enemy -> enemy.position.distanceTo(player.position) <= enemy.radius + player.radius }
            .maxByOrNull { enemy -> enemy.damage }
            ?: return player

        val newHp = (player.hp - touchingEnemy.damage).coerceAtLeast(0)
        return player.copy(hp = newHp, invulnerableUntilMs = nowMs + GameTunables.PLAYER_INVULNERABILITY_MS)
    }

    private const val PROJECTILE_HIT_RADIUS = 6f
    private const val HIT_FLASH_MS = 120L
}
