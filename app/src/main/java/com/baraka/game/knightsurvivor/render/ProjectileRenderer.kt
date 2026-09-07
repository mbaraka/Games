package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.baraka.game.knightsurvivor.model.Projectile
import kotlin.math.atan2

/** Draws every travelling projectile as a small dagger-like diamond, oriented along its direction of travel. */
fun DrawScope.drawProjectiles(projectiles: List<Projectile>) {
    projectiles.forEach { projectile ->
        val color = Color(projectile.sourceWeapon.colorArgb)
        val center = projectile.position.toOffset()
        val headingDegrees = Math.toDegrees(
            atan2(projectile.velocity.y.toDouble(), projectile.velocity.x.toDouble())
        ).toFloat() + 90f

        rotate(degrees = headingDegrees, pivot = center) {
            drawDiamond(center = center, halfWidth = 4f, halfHeight = 11f, color = color)
        }
    }
}
