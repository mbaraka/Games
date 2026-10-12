package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.DamageEvent
import com.baraka.game.knightsurvivor.model.EnemyDebuff
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.EntityIdGenerator
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Projectile
import com.baraka.game.knightsurvivor.model.Vec2
import com.baraka.game.knightsurvivor.model.WeaponActivationEvent
import com.baraka.game.knightsurvivor.model.WeaponInstance
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.model.WeaponTargeting
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Everything one tick of weapon activity produced, for systems/GameLoopOrchestrator.kt to fold into the next GameState. */
data class WeaponFiringResult(
    val weapons: List<WeaponInstance>,
    val newProjectiles: List<Projectile>,
    val directDamageEvents: List<DamageEvent>,
    val newActivationEvents: List<WeaponActivationEvent>,
    /** Which weapon kind(s) actually activated this tick — lets a UI layer play that weapon's own fire sound. Excludes ORBIT, which ticks too often to sound a fire cue without spamming. */
    val firedWeaponKinds: List<WeaponKind> = emptyList(),
    /** Enemy id -> its new position after Magnet Pulse yanks it closer. Applied by GameLoopOrchestrator once collision has resolved survivors. */
    val enemyPulls: Map<Long, Vec2> = emptyMap(),
    /** Enemy id -> stun/slow to apply — currently just Frying Pan's stun chance. */
    val enemyDebuffs: Map<Long, EnemyDebuff> = emptyMap()
)

/**
 * Ticks every owned weapon's cooldown and resolves what happens when it hits zero.
 *
 * All three [WeaponTargeting] shapes share one activation clock (see WeaponInstance's
 * KDoc). This system only decides *who gets hit*: for AREA_AROUND_PLAYER/ORBIT weapons
 * that means emitting [DamageEvent]s directly (there's no projectile to collide), while
 * NEAREST_ENEMY weapons emit a [Projectile] and let systems/CollisionSystem.kt resolve the
 * hit once it travels there. Either way damage is always *applied* — enemy HP reduced,
 * deaths handled, XP dropped — in exactly one place: CollisionSystem.
 */
object WeaponFiringSystem {

    fun step(
        player: PlayerState,
        enemies: List<EnemyInstance>,
        nowMs: Long,
        dtMs: Long,
        random: Random = Random.Default,
        ownerIsPartner: Boolean = false
    ): WeaponFiringResult {
        val updatedWeapons = mutableListOf<WeaponInstance>()
        val newProjectiles = mutableListOf<Projectile>()
        val damageEvents = mutableListOf<DamageEvent>()
        val activationEvents = mutableListOf<WeaponActivationEvent>()
        val firedWeaponKinds = mutableListOf<WeaponKind>()
        val enemyPulls = mutableMapOf<Long, Vec2>()
        val enemyDebuffs = mutableMapOf<Long, EnemyDebuff>()

        for (weapon in player.weapons) {
            val spinningWeapon = if (weapon.kind.targeting == WeaponTargeting.ORBIT) {
                weapon.copy(orbitAngleRadians = weapon.orbitAngleRadians + ORBIT_ANGULAR_SPEED * (dtMs / 1000f))
            } else {
                weapon
            }

            val remainingCooldownMs = spinningWeapon.cooldownRemainingMs - dtMs
            if (remainingCooldownMs > 0L) {
                updatedWeapons += spinningWeapon.copy(cooldownRemainingMs = remainingCooldownMs)
                continue
            }

            when (spinningWeapon.kind.targeting) {
                WeaponTargeting.NEAREST_ENEMY -> {
                    val shotCount = spinningWeapon.kind.projectileCountAtLevel(spinningWeapon.level)
                    val targets = nearestEnemiesWithinRange(player.position, enemies, shotCount)
                    targets.forEach { target -> newProjectiles += fireProjectileAt(player.position, target, spinningWeapon, ownerIsPartner) }
                    if (targets.isNotEmpty()) firedWeaponKinds += spinningWeapon.kind
                }

                WeaponTargeting.AREA_AROUND_PLAYER -> {
                    val radius = spinningWeapon.kind.radiusAtLevel(spinningWeapon.level)
                    val damage = spinningWeapon.kind.damageAtLevel(spinningWeapon.level)
                    val hitEnemies = enemies.filter { enemy -> enemy.position.distanceTo(player.position) <= radius + enemy.radius }
                    hitEnemies.forEach { enemy ->
                        damageEvents += DamageEvent(enemy.id, damage, spinningWeapon.kind)

                        if (spinningWeapon.kind.stunChance > 0f && random.nextFloat() < spinningWeapon.kind.stunChance) {
                            enemyDebuffs[enemy.id] = EnemyDebuff(stunUntilMs = nowMs + spinningWeapon.kind.stunDurationMs)
                        }

                        if (spinningWeapon.kind.pullStrengthPx > 0f) {
                            val towardPlayer = (player.position - enemy.position).normalized()
                            val pullDistance = spinningWeapon.kind.pullStrengthPx.coerceAtMost(enemy.position.distanceTo(player.position))
                            enemyPulls[enemy.id] = enemy.position + towardPlayer * pullDistance
                        }
                    }
                    activationEvents += WeaponActivationEvent(spinningWeapon.kind, player.position, radius, nowMs)
                    firedWeaponKinds += spinningWeapon.kind
                }

                WeaponTargeting.ORBIT -> {
                    val radius = spinningWeapon.kind.radiusAtLevel(spinningWeapon.level)
                    val damage = spinningWeapon.kind.damageAtLevel(spinningWeapon.level)
                    val orbCount = spinningWeapon.kind.orbCountAtLevel(spinningWeapon.level)
                    val orbPositions = orbitPositions(player.position, radius, spinningWeapon.orbitAngleRadians, orbCount)
                    enemies.forEach { enemy ->
                        val isHit = orbPositions.any { orb -> orb.distanceTo(enemy.position) <= ORBIT_HIT_RADIUS + enemy.radius }
                        if (isHit) damageEvents += DamageEvent(enemy.id, damage, spinningWeapon.kind)
                    }
                }
            }

            val freshCooldownMs = (spinningWeapon.kind.cooldownMsAtLevel(spinningWeapon.level) * player.cooldownMultiplier)
                .toLong()
                .coerceAtLeast(1L)
            updatedWeapons += spinningWeapon.copy(cooldownRemainingMs = freshCooldownMs)
        }

        return WeaponFiringResult(updatedWeapons, newProjectiles, damageEvents, activationEvents, firedWeaponKinds, enemyPulls, enemyDebuffs)
    }

    /**
     * Recomputes where an ORBIT weapon's spinning instances currently are. Also called by
     * render/PlayerRenderer.kt so the drawn shields always match hit detection exactly.
     */
    fun orbitPositions(center: Vec2, radius: Float, angleRadians: Float, count: Int): List<Vec2> {
        if (count <= 0) return emptyList()
        val angleStep = (2.0 * Math.PI / count).toFloat()
        return (0 until count).map { index ->
            val angle = angleRadians + index * angleStep
            center + Vec2(cos(angle) * radius, sin(angle) * radius)
        }
    }

    /** The [count] nearest enemies within range, closest first — so a multi-shot weapon spreads across distinct targets instead of dogpiling one. */
    private fun nearestEnemiesWithinRange(playerPosition: Vec2, enemies: List<EnemyInstance>, count: Int): List<EnemyInstance> =
        enemies
            .map { enemy -> enemy to enemy.position.distanceTo(playerPosition) }
            .filter { (_, distance) -> distance <= GameTunables.WEAPON_TARGETING_RANGE }
            .sortedBy { (_, distance) -> distance }
            .take(count)
            .map { (enemy, _) -> enemy }

    private fun fireProjectileAt(from: Vec2, target: EnemyInstance, weapon: WeaponInstance, ownerIsPartner: Boolean): Projectile {
        val direction = (target.position - from).normalized()
        return Projectile(
            id = EntityIdGenerator.next(),
            position = from,
            velocity = direction * weapon.kind.projectileSpeed,
            damage = weapon.kind.damageAtLevel(weapon.level),
            sourceWeapon = weapon.kind,
            pierceRemaining = weapon.kind.pierceAtLevel(weapon.level),
            ownerIsPartner = ownerIsPartner
        )
    }

    private const val ORBIT_ANGULAR_SPEED = 3.2f // radians/sec
    private const val ORBIT_HIT_RADIUS = 10f
}
