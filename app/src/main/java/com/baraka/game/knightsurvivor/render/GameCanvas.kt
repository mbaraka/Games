package com.baraka.game.knightsurvivor.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.KnightDesign

/**
 * Renders one frame of [state], layering arena -> gems -> weapon effects -> enemies ->
 * projectiles -> hit/death particles -> player(s). Reports its measured pixel size via
 * [onSizeMeasured] so ui/KnightSurvivorScreen.kt can keep `GameState.arenaSize` in sync
 * with actual layout — this file only draws; it never reads input or mutates game state.
 *
 * [knightDesign] and [partnerKnightDesign] are passed in separately from [state] rather
 * than living on [GameState]: they're purely cosmetic (see model/KnightDesign.kt's KDoc)
 * and never need to reach systems/, so they stay UI-level state in
 * ui/KnightSurvivorScreen.kt instead of polluting the gameplay model. [partnerKnightDesign]
 * is only drawn when [GameState.partner] is non-null (a co-op run).
 */
@Composable
fun GameCanvas(
    state: GameState,
    knightDesign: KnightDesign,
    partnerKnightDesign: KnightDesign = KnightDesign.SWORN_BLADE,
    onSizeMeasured: (widthPx: Float, heightPx: Float) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size -> onSizeMeasured(size.width.toFloat(), size.height.toFloat()) }
    ) {
        drawArena(state.arenaSize)
        drawXpGems(state.xpGems)
        drawHealthPickups(state.healthPickups)
        drawWeaponActivationEffects(state.recentWeaponActivations, state.elapsedMs)
        drawEnemies(state.enemies, state.elapsedMs)
        drawProjectiles(state.projectiles)
        drawEnemyProjectiles(state.enemyProjectiles)
        drawParticles(state.particles)
        drawPlayers(state.player, state.partner, knightDesign, partnerKnightDesign, state.elapsedMs)
    }
}
