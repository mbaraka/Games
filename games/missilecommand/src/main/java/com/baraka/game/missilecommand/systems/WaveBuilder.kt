package com.baraka.game.missilecommand.systems

import com.baraka.game.missilecommand.model.City
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.CITY_COUNT
import com.baraka.game.missilecommand.model.MissileCommandState.Companion.SILO_COUNT
import com.baraka.game.missilecommand.model.Silo

/**
 * Fixed ground layout: 3 silos interleaved with 6 cities across the width, silos spaced out
 * so losing one doesn't strand a whole side of the map. Positions never change between
 * waves — only who's still standing on them does.
 */
object WaveBuilder {

    /** Silos at roughly 1/6, 1/2, 5/6 of the width; cities fill the gaps between and around them. */
    val siloPositions: List<Float> = listOf(0.17f, 0.5f, 0.83f)
    val cityPositions: List<Float> = listOf(0.06f, 0.28f, 0.39f, 0.61f, 0.72f, 0.94f)

    fun freshSilos(): List<Silo> = siloPositions.mapIndexed { i, x -> Silo(id = i, x = x) }
    fun freshCities(): List<City> = cityPositions.mapIndexed { i, x -> City(id = i, x = x) }

    init {
        require(siloPositions.size == SILO_COUNT)
        require(cityPositions.size == CITY_COUNT)
    }

    /** More incoming missiles each wave, capped so late waves stay frantic rather than impossible. */
    fun missileCountForWave(wave: Int): Int = (6 + wave * 2).coerceAtMost(26)
}
