package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.model.EntityIdGenerator
import com.baraka.game.knightsurvivor.model.Particle
import com.baraka.game.knightsurvivor.model.Vec2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Moves/ages/prunes decorative [Particle]s, and knows how to build the two burst shapes
 * this game currently uses: a small spark for a hit, a bigger one for a death. Both are
 * "shoot particles outward in random directions" — only the count/speed/size/lifetime
 * differ — so a future third burst type (e.g. a level-up sparkle) is just another small
 * function here, not a new concept.
 */
object ParticleSystem {

    fun step(particles: List<Particle>, dtMs: Long, dtSeconds: Float): List<Particle> =
        particles.mapNotNull { particle ->
            val newAgeMs = particle.ageMs + dtMs
            if (newAgeMs >= particle.lifeMs) {
                null
            } else {
                particle.copy(position = particle.position + particle.velocity * dtSeconds, ageMs = newAgeMs)
            }
        }

    /** A handful of quick, short-lived sparks — feedback for a weapon landing a hit. */
    fun hitSpark(position: Vec2, colorArgb: Long, random: Random = Random.Default): List<Particle> =
        burst(position, colorArgb, count = 5, speedRange = 60f..140f, lifeMs = 220L, sizeRange = 2f..4f, random = random)

    /** A bigger, longer-lived burst — an enemy's death deserves more than a hit does. */
    fun deathBurst(position: Vec2, colorArgb: Long, random: Random = Random.Default): List<Particle> =
        burst(position, colorArgb, count = 10, speedRange = 80f..220f, lifeMs = 380L, sizeRange = 3f..6f, random = random)

    private fun burst(
        position: Vec2,
        colorArgb: Long,
        count: Int,
        speedRange: ClosedFloatingPointRange<Float>,
        lifeMs: Long,
        sizeRange: ClosedFloatingPointRange<Float>,
        random: Random
    ): List<Particle> = (0 until count).map {
        val angle = random.nextFloat() * (2f * Math.PI.toFloat())
        val speed = lerp(speedRange, random.nextFloat())
        val velocity = Vec2(cos(angle) * speed, sin(angle) * speed)

        Particle(
            id = EntityIdGenerator.next(),
            position = position,
            velocity = velocity,
            colorArgb = colorArgb,
            size = lerp(sizeRange, random.nextFloat()),
            lifeMs = lifeMs
        )
    }

    private fun lerp(range: ClosedFloatingPointRange<Float>, fraction: Float): Float =
        range.start + fraction * (range.endInclusive - range.start)
}
