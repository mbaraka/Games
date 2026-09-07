package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.Particle

/** Draws every active hit-spark/death-burst particle, fading and shrinking as it ages. */
fun DrawScope.drawParticles(particles: List<Particle>) {
    particles.forEach { particle ->
        val lifeFraction = (1f - particle.ageMs.toFloat() / particle.lifeMs).coerceIn(0f, 1f)
        if (lifeFraction <= 0f) return@forEach

        drawCircle(
            color = Color(particle.colorArgb).copy(alpha = lifeFraction),
            radius = particle.size * lifeFraction,
            center = particle.position.toOffset()
        )
    }
}
