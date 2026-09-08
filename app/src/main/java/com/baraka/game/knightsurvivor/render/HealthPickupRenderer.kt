package com.baraka.game.knightsurvivor.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.baraka.game.knightsurvivor.model.HealthPickup

private val HEALTH_PICKUP_COLOR = Color(0xFFE53935)

fun DrawScope.drawHealthPickups(pickups: List<HealthPickup>) {
    pickups.forEach { pickup -> drawHeart(center = pickup.position.toOffset(), radius = 8f, color = HEALTH_PICKUP_COLOR) }
}
