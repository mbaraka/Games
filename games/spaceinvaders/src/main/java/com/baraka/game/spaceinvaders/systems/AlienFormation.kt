package com.baraka.game.spaceinvaders.systems

import com.baraka.game.spaceinvaders.model.Alien
import com.baraka.game.spaceinvaders.model.Shield
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.COLS
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.ROWS
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.SHIELD_COUNT
import com.baraka.game.spaceinvaders.model.SpaceInvadersState.Companion.SHIELD_MAX_HP

/** Builds the starting grid for a wave, and the row's point value — the only things that ever change between waves is speed/fire-rate (handled in AlienWaveLoop), not the grid shape. */
object AlienFormation {

    fun build(): List<Alien> {
        val aliens = mutableListOf<Alien>()
        var id = 0
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                aliens += Alien(id = id++, row = row, col = col)
            }
        }
        return aliens
    }

    fun pointsForRow(row: Int): Int = (ROWS - row) * 10

    fun freshShields(): List<Shield> = (0 until SHIELD_COUNT).map { i ->
        Shield(id = i, x = (i + 1f) / (SHIELD_COUNT + 1f), hp = SHIELD_MAX_HP)
    }
}
