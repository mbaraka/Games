package com.baraka.game.knightsurvivor.model

/**
 * A transient "this weapon just activated here" marker, kept around only long enough for
 * render/PlayerRenderer.kt to draw a fading ring/slash effect. Pruned each tick by
 * systems/GameLoopOrchestrator.kt once older than balance/GameTunables.weaponEffectVisibleMs.
 */
data class WeaponActivationEvent(
    val kind: WeaponKind,
    val position: Vec2,
    val radius: Float,
    val activatedAtMs: Long
)
