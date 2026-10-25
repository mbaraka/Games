package com.baraka.game.basedefender.model

import kotlin.math.max

/** All wave pacing and enemy scaling in one place. Tuning balance means editing only this file and `Content.kt`. */
object Waves {
    fun enemyCount(wave: Int): Int = 6 + wave * 2
    fun spawnInterval(wave: Int): Int = max(24, 70 - wave * 2)
    fun baseHealth(wave: Int): Float = 20f + wave * 10f
    fun baseSpeed(wave: Int): Float = 0.8f + wave * 0.09f
    fun baseReward(wave: Int): Int = 5 + wave * 2

    /** Breather between waves, in ticks, and the cash bonus for clearing one. */
    const val BREAK_TICKS = 150
    fun clearBonus(wave: Int): Int = wave * 10

    /** A boss arrives halfway through every fifth wave. */
    fun bossFor(wave: Int): EnemyType? = if (wave % 5 == 0) EnemyType.BOSS else null

    /** Weighted pick among the types unlocked by [wave]; [roll] is a uniform 0..1 number. No allocation. */
    fun pick(wave: Int, roll: Float): EnemyType {
        var total = 0
        val types = EnemyType.entries
        for (i in types.indices) {
            val t = types[i]
            if (t.weight > 0 && t.minWave <= wave) total += t.weight
        }
        var target = roll * total
        for (i in types.indices) {
            val t = types[i]
            if (t.weight > 0 && t.minWave <= wave) {
                target -= t.weight
                if (target < 0f) return t
            }
        }
        return EnemyType.BASIC
    }

    /** Banner text, derived from the data: boss waves, then "new enemy" waves, then a plain number. */
    fun title(wave: Int): String {
        val boss = bossFor(wave)
        if (boss != null) return "BOSS WAVE: ${boss.label}"
        val types = EnemyType.entries
        for (i in types.indices) {
            val t = types[i]
            if (t.weight > 0 && t.minWave == wave) return "Wave $wave: ${t.label}s appear!"
        }
        return "Wave $wave"
    }
}
