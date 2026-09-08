package com.baraka.game.knightsurvivor.model

/** A heal drop, same magnet/collect behavior as [XpGem] but restores HP instead of granting XP. */
data class HealthPickup(
    val id: Long,
    val position: Vec2,
    val healAmount: Int,
    val magnetized: Boolean = false
)
