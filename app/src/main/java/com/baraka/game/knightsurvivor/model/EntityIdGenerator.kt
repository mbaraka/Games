package com.baraka.game.knightsurvivor.model

import java.util.concurrent.atomic.AtomicLong

/**
 * Generates unique ids for spawned enemies/projectiles/xp gems.
 *
 * This is the one deliberate bit of shared mutable state in an otherwise pure-data model
 * layer: ids only need to be unique within a single run (for bookkeeping like "which
 * enemies has this projectile already hit"), not deterministic or replayable, so a plain
 * atomic counter is simpler than threading a counter field through every spawn function.
 */
object EntityIdGenerator {
    private val counter = AtomicLong(1L)
    fun next(): Long = counter.getAndIncrement()
}
