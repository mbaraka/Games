package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.XpGem

private val XP_GEM_COLOR = Color(0xFF7CFC00)

/** Draws every XP gem lying in the arena as a small diamond — the classic "gem" silhouette. */
fun DrawScope.drawXpGems(gems: List<XpGem>) {
    gems.forEach { gem ->
        drawDiamond(center = gem.position.toOffset(), halfWidth = 5f, halfHeight = 7f, color = XP_GEM_COLOR)
    }
}
