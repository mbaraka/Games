package com.baraka.game.knightsurvivor.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.baraka.game.knightsurvivor.model.GameState

/**
 * Renders one frame of [state], layering arena -> gems -> weapon effects -> enemies ->
 * projectiles -> hit/death particles -> player. Reports its measured pixel size via
 * [onSizeMeasured] so ui/KnightSurvivorScreen.kt can keep `GameState.arenaSize` in sync
 * with actual layout — this file only draws; it never reads input or mutates game state.
 */
@Composable
fun GameCanvas(state: GameState, onSizeMeasured: (widthPx: Float, heightPx: Float) -> Unit) {
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
        drawPlayer(state.player, state.elapsedMs)
    }
}
