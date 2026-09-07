package com.baraka.game.knightsurvivor.systems

import com.baraka.game.knightsurvivor.balance.GameTunables
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Vec2

/**
 * Moves the player from the current joystick input and keeps them inside the arena.
 * A pure function of its inputs — doesn't know about combat, XP, or anything else.
 */
object PlayerMovementSystem {

    /** [joystickInput] is expected pre-normalized to length <= 1 by ui/VirtualJoystick.kt. */
    fun step(
        player: PlayerState,
        joystickInput: Vec2,
        arenaSize: ArenaSize,
        dtSeconds: Float
    ): PlayerState {
        val speed = GameTunables.PLAYER_BASE_SPEED_PX_PER_SEC * player.moveSpeedMultiplier
        val newPosition = (player.position + joystickInput * speed * dtSeconds)
            .clampToArena(arenaSize.width, arenaSize.height, player.radius)

        return player.copy(position = newPosition)
    }
}
