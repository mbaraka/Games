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
import com.baraka.game.knightsurvivor.model.WeaponTargeting
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

/** Everything one tick of world-vs-enemy damage resolution produced — the half of [CollisionResult] that doesn't involve any one specific player. See [CollisionSystem.resolveWorldDamage]. */
data class WorldDamageResult(
    val enemies: List<EnemyInstance>,
    val projectiles: List<Projectile>,
    val newXpGems: List<XpGem>,
    val newHealthPickups: List<HealthPickup>,
    val killsThisTick: Int,
    val hitFeedback: List<HitFeedback> = emptyList(),
    val deathFeedback: List<HitFeedback> = emptyList(),
    val newEnemies: List<EnemyInstance> = emptyList(),
    val enemyDebuffs: Map<Long, EnemyDebuff> = emptyMap()
)

/**
 * The one place damage is actually applied. Split into two halves so co-op can share the
 * first and run the second once per player:
 *
 * - [resolveWorldDamage]: projectile-vs-enemy hits (including Firecracker String's splash
 *   damage, Egg Toss's splash slow, Static Zapper's stun-on-hit), plus any [DamageEvent]s
 *   handed over by systems/WeaponFiringSystem.kt for AoE/orbit weapons. Enemies whose HP
 *   drops to zero die here (spawning Slime's split offspring, dropping XP/heals). Run
 *   exactly ONCE per tick on the combined projectile/damage-event list from every player —
 *   projectile-vs-enemy hits don't depend on which player fired them.
 * - [resolvePlayerDamage]: enemy-vs-ONE-player damage, both contact and ranged
 *   [EnemyProjectile]s, respecting that player's invulnerability window and applying
 *   Golem's root. Run once per player, threading the enemy list and surviving
 *   [EnemyProjectile]s from one call to the next so a projectile that hits player A isn't
 *   also available to hit player B the same tick.
 *
 * [resolve] composes both for the common solo-player case.
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
        val world = resolveWorldDamage(enemies, projectiles, externalDamageEvents, nowMs, random)
        val (updatedPlayer, survivingEnemyProjectiles) = resolvePlayerDamage(player, world.enemies, enemyProjectiles, nowMs)

        return CollisionResult(
            enemies = world.enemies,
            projectiles = world.projectiles,
            newXpGems = world.newXpGems,
            newHealthPickups = world.newHealthPickups,
            killsThisTick = world.killsThisTick,
            player = updatedPlayer,
            enemyProjectiles = survivingEnemyProjectiles,
            hitFeedback = world.hitFeedback,
            deathFeedback = world.deathFeedback,
            newEnemies = world.newEnemies,
            enemyDebuffs = world.enemyDebuffs
        )
    }

    fun resolveWorldDamage(
        enemies: List<EnemyInstance>,
        projectiles: List<Projectile>,
        externalDamageEvents: List<DamageEvent>,
        nowMs: Long,
        random: Random = Random.Default
    ): WorldDamageResult {
        val (survivingProjectiles, projectileDamageEvents, splashDebuffs) = resolveProjectileHits(projectiles, enemies, nowMs, random)

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

        return WorldDamageResult(
            enemies = survivingEnemies,
            projectiles = survivingProjectiles,
            newXpGems = newXpGems,
            newHealthPickups = newHealthPickups,
            killsThisTick = kills,
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

    /** Resolves projectile-vs-enemy hits, including Firecracker String's splash damage, Egg Toss's splash slow, and Static Zapper's stun-on-hit chance. */
    private fun resolveProjectileHits(
        projectiles: List<Projectile>,
        enemies: List<EnemyInstance>,
        nowMs: Long,
        random: Random
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

            val stunChance = projectile.sourceWeapon.stunChance
            if (stunChance > 0f && random.nextFloat() < stunChance) {
                val existing = splashDebuffs[hitEnemy.id]
                splashDebuffs[hitEnemy.id] = (existing ?: EnemyDebuff()).copy(stunUntilMs = nowMs + projectile.sourceWeapon.stunDurationMs)
            }

            val splashRadius = projectile.sourceWeapon.splashRadius
            if (splashRadius > 0f) {
                val splashDamage = (projectile.damage * projectile.sourceWeapon.splashDamageFraction).toInt()
                enemies
                    .filter { other -> other.id != hitEnemy.id && other.position.distanceTo(hitEnemy.position) <= splashRadius }
                    .forEach { other ->
                        if (splashDamage > 0) damageEvents += DamageEvent(other.id, splashDamage, projectile.sourceWeapon)
                        if (projectile.sourceWeapon.splashSlowDurationMs > 0L) {
                            val existing = splashDebuffs[other.id]
                            splashDebuffs[other.id] = (existing ?: EnemyDebuff()).copy(
                                slowUntilMs = nowMs + projectile.sourceWeapon.splashSlowDurationMs,
                                slowMultiplier = projectile.sourceWeapon.splashSlowMultiplier
                            )
                        }
                    }
                if (projectile.sourceWeapon.splashSlowDurationMs > 0L) {
                    val existing = splashDebuffs[hitEnemy.id]
                    splashDebuffs[hitEnemy.id] = (existing ?: EnemyDebuff()).copy(
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

    /**
     * Enemy-vs-ONE-player damage from both contact and ranged shots, plus Golem's root —
     * respects that player's own invulnerability window. Call once per player in a co-op
     * run, threading [enemies] and [enemyProjectiles] from one call to the next (see this
     * object's class-level KDoc).
     *
     * Any ORBIT weapon (today, just Orbit Shield) physically blocks an enemy projectile that
     * touches one of its orbiting instances before it ever gets a chance to hit the player -
     * the shield is a real obstacle, not just a melee weapon, which is also what makes it
     * mechanically distinct from the AREA_AROUND_PLAYER weapons (see model/WeaponKind.kt).
     */
    fun resolvePlayerDamage(
        player: PlayerState,
        enemies: List<EnemyInstance>,
        enemyProjectiles: List<EnemyProjectile>,
        nowMs: Long
    ): Pair<PlayerState, List<EnemyProjectile>> {
        val shieldPositions = orbitWeaponPositions(player)
        val projectilesPastShield = if (shieldPositions.isEmpty()) {
            enemyProjectiles
        } else {
            enemyProjectiles.filterNot { projectile ->
                shieldPositions.any { shieldPosition -> projectile.position.distanceTo(shieldPosition) <= ORBIT_SHIELD_BLOCK_RADIUS }
            }
        }

        val touchingEnemy = enemies
            .filter { enemy -> !enemy.disguisedAsPickup && enemy.position.distanceTo(player.position) <= enemy.radius + player.radius }
            .maxByOrNull { enemy -> enemy.damage }

        val (hittingProjectiles, survivingProjectiles) = projectilesPastShield.partition { projectile ->
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

    /** Live positions of every instance of every ORBIT-targeting weapon the player owns (see [resolvePlayerDamage]'s KDoc). */
    private fun orbitWeaponPositions(player: PlayerState): List<Vec2> =
        player.weapons
            .filter { weapon -> weapon.kind.targeting == WeaponTargeting.ORBIT }
            .flatMap { weapon ->
                WeaponFiringSystem.orbitPositions(
                    center = player.position,
                    radius = weapon.kind.radiusAtLevel(weapon.level),
                    angleRadians = weapon.orbitAngleRadians,
                    count = weapon.kind.orbCountAtLevel(weapon.level)
                )
            }

    private const val PROJECTILE_HIT_RADIUS = 6f
    private const val ENEMY_PROJECTILE_HIT_RADIUS = 6f
    /** How close an enemy projectile must get to an orbiting shield instance to be blocked by it. */
    private const val ORBIT_SHIELD_BLOCK_RADIUS = 20f
    private const val HIT_FLASH_MS = 120L
}
