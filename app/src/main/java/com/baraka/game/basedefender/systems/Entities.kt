package com.baraka.game.basedefender.systems

import com.baraka.game.basedefender.model.EnemyType
import com.baraka.game.basedefender.model.PowerUpType
import com.baraka.game.basedefender.model.TurretRole
import com.baraka.game.basedefender.model.WeaponType

/** Minimal object pool so the update loop never allocates once the pools are warm. */
class Pool<T>(private val factory: () -> T) {
    private val free = ArrayList<T>()
    fun obtain(): T = if (free.isEmpty()) factory() else free.removeAt(free.size - 1)
    fun release(item: T) { free.add(item) }
}

/** Cached number strings, so floating damage numbers do not build a new String per hit. */
object NumberStrings {
    private val cache = arrayOfNulls<String>(2000)
    fun of(n: Int): String {
        if (n < 0 || n >= cache.size) return n.toString()
        return cache[n] ?: n.toString().also { cache[n] = it }
    }
}

/** Positions are centres, in world units (the world is always 400 wide). */
class Enemy {
    var type = EnemyType.BASIC
    var x = 0f
    var y = 0f
    var hp = 1f
    var maxHp = 1f
    var speed = 1f
    var reward = 0
    var size = 28f
    var burn = 0
    var freeze = 0
    var flash = 0
    var phase = 0f
}

class Bullet {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var damage = 0f
    var size = 4f
    var weapon = WeaponType.BASIC
    var pierceLeft = 0
    var mega = false
    var hitCount = 0
    val hits = arrayOfNulls<Enemy>(6)
}

class Pickup {
    var type = PowerUpType.RAPID_FIRE
    var x = 0f
    var y = 0f
    var phase = 0f
}

class Turret(val role: TurretRole) {
    var x = 0f
    var angle = 0f
    var cooldown = 0f
    var recoil = 0f
    var flash = 0
}
