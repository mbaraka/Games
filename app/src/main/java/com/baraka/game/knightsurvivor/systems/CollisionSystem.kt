package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.DamageEvent
import com.baraka.game.knightsurvivor.model.EnemyDebuff
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.EnemyKind
import com.baraka.game.knightsurvivor.model.EnemyProjectile
import com.baraka.game.knightsurvivor.model.EntityIdGenerator
import com.baraka.game.knightsurvivor.model.HealthPickup
import com.baraka.game.knightsurvivor.model.HitFeedback
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Projectile
import com.baraka.game.knightsurvivor.model.Vec2
import com.baraka.game.knightsurvivor.model.XpGem
import kotlin.random.Random

/** Everything one tick of collision resolution produced, for GameLoopOrchestrator to fold into the next GameState. */
data class CollisionResult(
    val enemies: List<EnemyInstance>,
    val projectiles: List<Projectile>,
    val newXpGems: List<XpGem>,
    val newHealthPickups: List<HealthPickup>,
    val killsThisTick: Int,
    val player: PlayerState,
    val enemyProjectiles: List<EnemyProjectile>,
    /** An enemy that took damage and survived — GameLoopOrchestrator turns each into a small spark burst. */
    val hitFeedback: List<HitFeedback> = emptyList(),
    /** An enemy whose HP reached zero — GameLoopOrchestrator turns each into a bigger death burst. */
    val deathFeedback: List<HitFeedback> = emptyList(),
    /** Slime's split-on-death offspring — added alongside [enemies] by GameLoopOrchestrator. */
    val newEnemies: List<EnemyInstance> = emptyList(),
    /** Enemy id -> stun/slow from a splash weapon (Egg Toss) — merged the same way as WeaponFiringResult.enemyDebuffs. */
    val enemyDebuffs: Map<Long, EnemyDebuff> = emptyMap()
)

/**
 * The one place damage is actually applied. Resolves projectile-vs-enemy hits (including
 * splash from Firecracker String/Egg Toss), folds in any [DamageEvent]s handed over by
 * systems/WeaponFiringSystem.kt (AoE/orbit weapons have no projectile of their own), kills
 * enemies whose HP drops to zero (spawning Slime's split offspring, dropping XP/heals),
 * then resolves enemy-vs-player damage — both contact and Imp's ranged
 * [EnemyProjectile]s — respecting the player's invulnerability window and applying
 * Golem's root. Keeping all of this in one file means "how much damage did X take this
 * tick" never has to be pieced together from multiple systems.
 */
object CollisionSystem {

    fun resolve(
        enemies: List<EnemyInstance>,
        projectiles: List<Projectile>,
        enemyProjectiles: List<EnemyProjectile>,
        player: PlayerState,
        externalDamageEvents: List<DamageEvent>,
        nowMs: Long,
        random: Random = Random.Default
    ): CollisionResult {
        val (survivingProjectiles, projectileDamageEvents, splashDebuffs) = resolveProjectileHits(projectiles, enemies, nowMs)

        val totalDamageByEnemyId = (projectileDamageEvents + externalDamageEvents)
            .groupingBy { it.targetEnemyId }
            .fold(0) { accumulated, event -> accumulated + event.amount }

        val survivingEnemies = mutableListOf<EnemyInstance>()
        val newEnemies = mutableListOf<EnemyInstance>()
        val newXpGems = mutableListOf<XpGem>()
        val newHealthPickups = mutableListOf<HealthPickup>()
        val hitFeedback = mutableListOf<HitFeedback>()
        val deathFeedback = mutableListOf<HitFeedback>()
        var kills = 0

        for (enemy in enemies) {
            val rawDamage = totalDamageByEnemyId[enemy.id] ?: 0
            val damage = if (rawDamage > 0 && enemy.isPhaseInvulnerableAt(nowMs)) 0 else rawDamage
            val newHp = enemy.hp - damage
            if (newHp <= 0) {
                kills++
                newXpGems += XpGem(id = EntityIdGenerator.next(), position = enemy.position, value = enemy.kind.xpValue)
                if (random.nextFloat() < enemy.kind.healthDropChance) {
                    newHealthPickups += HealthPickup(
                        id = EntityIdGenerator.next(),
                        position = enemy.position,
                        healAmount = GameTunables.HEALTH_PICKUP_HEAL_AMOUNT
                    )
                }
                deathFeedback += HitFeedback(enemy.position, enemy.kind.colorArgb, isBoss = enemy.isBoss)

                enemy.kind.splitsOnDeathInto?.let { splitKind ->
                    newEnemies += spawnSplit(splitKind, enemy, random)
                    newEnemies += spawnSplit(splitKind, enemy, random)
                }
            } else {
                survivingEnemies += enemy.copy(
                    hp = newHp,
                    hitFlashUntilMs = if (damage > 0) nowMs + HIT_FLASH_MS else enemy.hitFlashUntilMs
                )
                if (damage > 0) hitFeedback += HitFeedback(enemy.position, enemy.kind.colorArgb)
            }
        }

        val (updatedPlayer, survivingEnemyProjectiles) = resolvePlayerDamage(player, survivingEnemies, enemyProjectiles, nowMs)

        return CollisionResult(
            enemies = survivingEnemies,
            projectiles = survivingProjectiles,
            newXpGems = newXpGems,
            newHealthPickups = newHealthPickups,
            killsThisTick = kills,
            player = updatedPlayer,
            enemyProjectiles = survivingEnemyProjectiles,
            hitFeedback = hitFeedback,
            deathFeedback = deathFeedback,
            newEnemies = newEnemies,
            enemyDebuffs = splashDebuffs
        )
    }

    private fun spawnSplit(kind: EnemyKind, parent: EnemyInstance, random: Random): EnemyInstance {
        val offset = Vec2((random.nextFloat() - 0.5f) * 24f, (random.nextFloat() - 0.5f) * 24f)
        val hp = (parent.maxHp / 2).coerceAtLeast(1)
        return EnemyInstance(
            id = EntityIdGenerator.next(),
            kind = kind,
            position = parent.position + offset,
            hp = hp,
            maxHp = hp,
            speed = kind.baseSpeed,
            damage = kind.baseDamage
        )
    }

    /** Resolves projectile-vs-enemy hits, including Firecracker String's splash damage and Egg Toss's splash slow. */
    private fun resolveProjectileHits(
        projectiles: List<Projectile>,
        enemies: List<EnemyInstance>,
        nowMs: Long
    ): Triple<List<Projectile>, List<DamageEvent>, Map<Long, EnemyDebuff>> {
        val survivingProjectiles = mutableListOf<Projectile>()
        val damageEvents = mutableListOf<DamageEvent>()
        val splashDebuffs = mutableMapOf<Long, EnemyDebuff>()

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

            val splashRadius = projectile.sourceWeapon.splashRadius
            if (splashRadius > 0f) {
                val splashDamage = (projectile.damage * projectile.sourceWeapon.splashDamageFraction).toInt()
                enemies
                    .filter { other -> other.id != hitEnemy.id && other.position.distanceTo(hitEnemy.position) <= splashRadius }
                    .forEach { other ->
                        if (splashDamage > 0) damageEvents += DamageEvent(other.id, splashDamage, projectile.sourceWeapon)
                        if (projectile.sourceWeapon.splashSlowDurationMs > 0L) {
                            splashDebuffs[other.id] = EnemyDebuff(
                                slowUntilMs = nowMs + projectile.sourceWeapon.splashSlowDurationMs,
                                slowMultiplier = projectile.sourceWeapon.splashSlowMultiplier
                            )
                        }
                    }
                if (projectile.sourceWeapon.splashSlowDurationMs > 0L) {
                    splashDebuffs[hitEnemy.id] = EnemyDebuff(
                        slowUntilMs = nowMs + projectile.sourceWeapon.splashSlowDurationMs,
                        slowMultiplier = projectile.sourceWeapon.splashSlowMultiplier
                    )
                }
            }

            // pierceRemaining counts how many MORE enemies this projectile may hit after this one.
            val updatedProjectile = projectile.copy(
                hitEnemyIds = projectile.hitEnemyIds + hitEnemy.id,
                pierceRemaining = projectile.pierceRemaining - 1
            )
            if (updatedProjectile.pierceRemaining >= 0) {
                survivingProjectiles += updatedProjectile
            }
        }

        return Triple(survivingProjectiles, damageEvents, splashDebuffs)
    }

    /** Enemy-vs-player damage from both contact and Imp's ranged shots, plus Golem's root — respects one shared invulnerability window. */
    private fun resolvePlayerDamage(
        player: PlayerState,
        enemies: List<EnemyInstance>,
        enemyProjectiles: List<EnemyProjectile>,
        nowMs: Long
    ): Pair<PlayerState, List<EnemyProjectile>> {
        val touchingEnemy = enemies
            .filter { enemy -> !enemy.disguisedAsPickup && enemy.position.distanceTo(player.position) <= enemy.radius + player.radius }
            .maxByOrNull { enemy -> enemy.damage }

        val (hittingProjectiles, survivingProjectiles) = enemyProjectiles.partition { projectile ->
            projectile.position.distanceTo(player.position) <= player.radius + ENEMY_PROJECTILE_HIT_RADIUS
        }
        val hardestProjectileHit = hittingProjectiles.maxByOrNull { it.damage }

        if (player.isInvulnerableAt(nowMs) || (touchingEnemy == null && hardestProjectileHit == null)) {
            return player to survivingProjectiles
        }

        val damage = maxOf(touchingEnemy?.damage ?: 0, hardestProjectileHit?.damage ?: 0)
        val newHp = (player.hp - damage).coerceAtLeast(0)
        val rootedUntilMs = if (touchingEnemy?.kind?.rootsPlayerOnHit == true) {
            nowMs + touchingEnemy.kind.rootDurationMs
        } else {
            player.rootedUntilMs
        }

        val updatedPlayer = player.copy(
            hp = newHp,
            invulnerableUntilMs = nowMs + GameTunables.PLAYER_INVULNERABILITY_MS,
            rootedUntilMs = rootedUntilMs
        )
        return updatedPlayer to survivingProjectiles
    }

    private const val PROJECTILE_HIT_RADIUS = 6f
    private const val ENEMY_PROJECTILE_HIT_RADIUS = 6f
    private const val HIT_FLASH_MS = 120L
}
