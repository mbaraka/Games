package com.baraka.game.numbercorridor.model

enum class GamePhase {
    RUNNING,
    GAME_OVER
}

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/GameLoopOrchestrator.kt's `tick()` one copy at a time — same shape as
 * knightsurvivor's GameState, just for a lane-runner instead of an arena.
 */
data class GameState(
    val player: PlayerState = PlayerState.initial(),
    val rows: List<WallRow> = emptyList(),
    val bullets: List<Bullet> = emptyList(),
    val score: Int = 0,
    val elapsedSeconds: Float = 0f,
    /** How fast rows advance toward the player, in progress/second. Ramps up with [elapsedSeconds]. */
    val rowSpeed: Float = BASE_ROW_SPEED,
    /** Countdown to the next row spawn. */
    val nextRowInSeconds: Float = 0f,
    val phase: GamePhase = GamePhase.RUNNING,
    val avatarDesign: AvatarDesign = AvatarDesign.RANGER,
    val bestScoreAtStart: Int = 0,
    /** Monotonically increasing id source for spawned rows, threaded through state to keep tick() a pure function. */
    val nextRowId: Long = 0L
) {
    val isNewBest: Boolean get() = score > bestScoreAtStart

    companion object {
        const val BASE_ROW_SPEED = 0.14f
        const val MAX_ROW_SPEED = 0.42f
        const val BULLET_SPEED = 0.9f
        const val ROW_SPAWN_INTERVAL_SECONDS = 1.6f
    }
}
