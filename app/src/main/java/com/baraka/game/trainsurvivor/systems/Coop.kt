package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.ZombieType

/** How this phone takes part in a co-op run. Solo play never leaves [SOLO]. */
enum class CoopRole {
    SOLO,
    /** Runs the shared timers (route junctions, boss arrivals) and tells the guest. */
    HOST,
    /** Follows the host for junctions and boss arrivals. */
    GUEST
}

/**
 * What the engine tells the co-op session (see the `coop` package). The engine never talks to the network
 * itself: it only emits these, and receives the partner's actions through its `remote...` functions.
 */
sealed class CoopSignal {
    class JunctionOpened(val a: Biome, val b: Biome) : CoopSignal()
    /** The guest tapped a route: the host decides. */
    class RouteRequested(val choice: Biome) : CoopSignal()
    class RouteChosen(val choice: Biome) : CoopSignal()
    class BossSpawned(val type: ZombieType) : CoopSignal()
    /** This train lent a passenger to the partner's train. */
    class PassengerSent(val survivor: Int, val slot: CrewSlot, val seconds: Float) : CoopSignal()
    /** This train used Blast; the partner's train gets one too. */
    object Blast : CoopSignal()
    /** This train sent a repair to the partner (a fraction of their train's health). */
    class RepairSent(val fraction: Float) : CoopSignal()
    object TrainDestroyed : CoopSignal()
}
