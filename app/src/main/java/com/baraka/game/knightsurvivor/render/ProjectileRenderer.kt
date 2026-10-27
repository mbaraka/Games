package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.Projectile
import com.baraka.game.ui.icons.drawVectorIcon
import kotlin.math.atan2

private const val PROJECTILE_RADIUS = 11f

/** Draws every travelling projectile using its source weapon's own [com.baraka.game.ui.icons.VectorIcon], oriented along its direction of travel. */
fun DrawScope.drawProjectiles(projectiles: List<Projectile>) {
    projectiles.forEach { projectile ->
        val color = Color(projectile.sourceWeapon.colorArgb)
        val center = projectile.position.toOffset()
        val headingDegrees = Math.toDegrees(
            atan2(projectile.velocity.y.toDouble(), projectile.velocity.x.toDouble())
        ).toFloat() + 90f

        rotate(degrees = headingDegrees, pivot = center) {
            drawVectorIcon(icon = projectile.sourceWeapon.icon, center = center, radius = PROJECTILE_RADIUS * GameTunables.VISUAL_SCALE, color = color)
        }
    }
}
