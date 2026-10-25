package com.baraka.game.trainsurvivor.systems

import com.baraka.game.trainsurvivor.model.Biome
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.Livery
import com.baraka.game.trainsurvivor.model.MAX_SLOTS
import com.baraka.game.trainsurvivor.model.TrainShape
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
    /** This train gave the partner some of its run coins. */
    class CoinsSent(val amount: Int) : CoopSignal()
    /** This train is locked by a co-op hazard; the partner can clear it. */
    class HelpNeeded(val kind: Int) : CoopSignal()
    /** We cleared the partner's lock. */
    object HelpGiven : CoopSignal()
    object TowSent : CoopSignal()
    /** A stranded survivor appeared on this train's track. */
    object RescueCall : CoopSignal()
    /** The stranded survivor was rescued: both trains share [bonus] coins. */
    class RescueDone(val bonus: Int) : CoopSignal()
    object TrainDestroyed : CoopSignal()
}

/**
 * What this phone knows about the partner's train, enough to draw it on a second track (see render/GhostTrain.kt):
 * its shape and paint, each wagon's health (index 0 is the locomotive; 0 = destroyed) and a flash per gun that just fired.
 */
class GhostTrain {
    var active = false
    var shape: TrainShape = TrainShape.STEAM
    var livery: Livery = Livery.CLASSIC
    var count = 1
    val hp = FloatArray(MAX_SLOTS) { 1f }
    val flash = FloatArray(MAX_SLOTS + 1)
    /** Seconds since the partner last sent anything. */
    var silence = 0f
    val away get() = silence > 4f

    fun apply(shapeIndex: Int, liveryIndex: Int, hpPercent: IntArray, shotMask: Int) {
        active = true
        silence = 0f
        shape = TrainShape.entries.getOrElse(shapeIndex) { TrainShape.STEAM }
        livery = Livery.entries.getOrElse(liveryIndex) { Livery.CLASSIC }
        count = hpPercent.size.coerceIn(1, MAX_SLOTS)
        for (i in 0 until count) hp[i] = hpPercent[i] / 100f
        for (i in 0 until count) if (shotMask and (1 shl i) != 0) flash[i] = 1f
    }

    fun update(dt: Float) {
        silence += dt
        for (i in flash.indices) if (flash[i] > 0f) flash[i] = (flash[i] - dt * 6f).coerceAtLeast(0f)
    }
}
