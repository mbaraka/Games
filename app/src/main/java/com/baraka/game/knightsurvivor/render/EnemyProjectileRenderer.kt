package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.EnemyProjectile
import com.baraka.game.ui.icons.drawDiamond

private const val ENEMY_PROJECTILE_RADIUS = 7f

/** Draws enemy-fired projectiles (currently just the Imp) as a small diamond in the firing enemy's own color — no VectorIcon of their own, unlike player weapon projectiles. */
fun DrawScope.drawEnemyProjectiles(projectiles: List<EnemyProjectile>) {
    projectiles.forEach { projectile ->
        drawDiamond(
            center = projectile.position.toOffset(),
            halfWidth = ENEMY_PROJECTILE_RADIUS,
            halfHeight = ENEMY_PROJECTILE_RADIUS * 1.4f,
            color = Color(projectile.colorArgb)
        )
    }
}
