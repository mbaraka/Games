package com.baraka.game.basedefender.systems

import androidx.compose.ui.graphics.Color
import com.baraka.game.basedefender.data.BdSettings
import com.baraka.game.basedefender.model.EnemyType
import com.baraka.game.basedefender.model.GameStatus
import com.baraka.game.basedefender.model.PowerUpType
import com.baraka.game.basedefender.model.Tuning
import com.baraka.game.basedefender.model.TurretRole
import com.baraka.game.basedefender.model.UpgradeType
import com.baraka.game.basedefender.model.Waves
import com.baraka.game.basedefender.model.WeaponType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** What the Compose HUD shows. Rebuilt only when [BaseDefenderEngine.hudDirty] is set, never per frame. */
data class BdHud(
    val money: Int,
    val lives: Int,
    val wave: Int,
    val score: Int,
    val bases: Int,
    val weapon: WeaponType,
    val powerMask: Int,
    val megaCharges: Int,
    val status: GameStatus
)

/**
 * The simulation. Pure Kotlin state: it never touches Compose, audio or the network, and all positions are in
 * a world that is [Tuning.WORLD_W] wide whatever the screen is. The UI feeds it time ([advance]) and pointer
 * events, the renderer reads its public lists. Per-frame code uses index loops and pools (see AGENTS.md).
 */
class BaseDefenderEngine(var settings: BdSettings = BdSettings(), private val rng: Random = Random.Default) {
    // ---- world / view ----
    var worldH = 700f
        private set
    /** Screen pixels per world unit. */
    var viewScale = 1f
        private set
    val groundY: Float get() = worldH - Tuning.GROUND_H

    fun resize(pxW: Float, pxH: Float) {
        if (pxW <= 0f || pxH <= 0f) return
        viewScale = pxW / Tuning.WORLD_W
        val h = pxH / viewScale
        if (h != worldH) {
            worldH = h
            layoutTurrets()
        }
    }

    // ---- entities ----
    val enemies = ArrayList<Enemy>(Tuning.MAX_ENEMIES)
    val bullets = ArrayList<Bullet>(Tuning.MAX_BULLETS)
    val pickups = ArrayList<Pickup>(Tuning.MAX_PICKUPS)
    val turrets = ArrayList<Turret>(Tuning.MAX_BASES)
    val particles = Particles()
    val texts = FloatTexts()
    private val enemyPool = Pool { Enemy() }
    private val bulletPool = Pool { Bullet() }
    private val pickupPool = Pool { Pickup() }

    // ---- run state (HUD-relevant values mark [hudDirty]) ----
    var status = GameStatus.PLAYING
        private set(v) { if (field != v) { field = v; hudDirty = true } }
    var money = Tuning.START_MONEY
        private set(v) { if (field != v) { field = v; hudDirty = true } }
    var lives = Tuning.START_LIVES
        private set(v) { if (field != v) { field = v; hudDirty = true } }
    var wave = 1
        private set(v) { if (field != v) { field = v; hudDirty = true } }
    var score = 0
        private set(v) { if (field != v) { field = v; hudDirty = true } }
    var weapon = WeaponType.BASIC
        set(v) { if (field != v) { field = v; hudDirty = true } }
    var megaCharges = 0
        private set(v) { if (field != v) { field = v; hudDirty = true } }
    var time = 0
        private set
    var kills = 0
        private set
    var moneyEarned = 0
        private set
    var shotsFired = 0
        private set
    var hudDirty = true

    // power-up timers (ticks left)
    var rapidTicks = 0
        private set
    var shieldTicks = 0
        private set
    var slowTicks = 0
        private set
    private var lastPowerMask = 0

    // effects
    var shake = 0f
        private set
    var groundFlash = 0
        private set
    var banner = ""
        private set
    var bannerTicks = 0
        private set
    /** Counted up on big moments; the UI turns each into a haptic pulse and resets it. */
    var hapticPulses = 0

    // debug
    var godMode = false
    var timeScale = 1f

    // upgrades
    private val levels = IntArray(UpgradeType.entries.size)
    private var damage = Tuning.BASE_DAMAGE
    private var bulletSpeed = Tuning.BASE_BULLET_SPEED
    private var fireInterval = Tuning.BASE_FIRE_INTERVAL

    // waves
    private var toSpawn = 0
    private var spawnTimer = 0
    private var bossPending = false
    private var waveActive = false
    private var breakTicks = 60

    // aiming
    var aimX = 0f
        private set
    var aimY = 0f
        private set
    var aimTicks = 0
        private set
    private var touching = false
    private var acc = 0f

    init {
        newGame()
    }

    // ------------------------------------------------------------------ public API

    fun newGame() {
        for (i in enemies.indices) enemyPool.release(enemies[i])
        for (i in bullets.indices) bulletPool.release(bullets[i])
        for (i in pickups.indices) pickupPool.release(pickups[i])
        enemies.clear(); bullets.clear(); pickups.clear(); turrets.clear()
        particles.clear(); texts.clear()
        levels.fill(0)
        status = GameStatus.PLAYING
        money = Tuning.START_MONEY
        lives = Tuning.START_LIVES
        wave = 1
        score = 0
        weapon = WeaponType.BASIC
        megaCharges = 0
        time = 0; kills = 0; moneyEarned = 0; shotsFired = 0
        rapidTicks = 0; shieldTicks = 0; slowTicks = 0
        shake = 0f; groundFlash = 0; hapticPulses = 0
        toSpawn = 0; spawnTimer = 0; bossPending = false; waveActive = false; breakTicks = 60
        aimTicks = 0; touching = false; acc = 0f
        banner = Waves.title(1); bannerTicks = 120
        turrets.add(Turret(TurretRole.forSlot(0)))
        recomputeStats()
        layoutTurrets()
        hudDirty = true
    }

    /** Feed real elapsed seconds; runs as many fixed 60 Hz steps as that covers (at most 5, so a hitch cannot spiral). */
    fun advance(seconds: Float) {
        acc += min(seconds, 0.1f) * timeScale
        var steps = 0
        val stepLen = 1f / Tuning.TICKS_PER_SECOND
        while (acc >= stepLen && steps < 5) {
            acc -= stepLen
            step()
            steps++
        }
        if (steps == 5) acc = 0f
    }

    fun pointerDown(wx: Float, wy: Float) {
        if (status != GameStatus.PLAYING) return
        touching = true
        aimX = wx; aimY = wy; aimTicks = 30
        // Tapping a falling power-up grabs it.
        for (i in pickups.indices.reversed()) {
            val p = pickups[i]
            val dx = p.x - wx
            val dy = p.y - wy
            if (dx * dx + dy * dy < 34f * 34f) {
                collect(p)
                removePickup(i)
                break
            }
        }
    }

    fun pointerMove(wx: Float, wy: Float) {
        aimX = wx; aimY = wy; aimTicks = 30
    }

    fun pointerUp() {
        touching = false
    }

    fun level(type: UpgradeType): Int = levels[type.ordinal]

    fun cost(type: UpgradeType): Int = (type.baseCost * type.growth.pow(levels[type.ordinal])).toInt()

    fun canBuy(type: UpgradeType): Boolean {
        if (status != GameStatus.PLAYING) return false
        if (levels[type.ordinal] >= type.maxLevel) return false
        if (type == UpgradeType.REPAIR && lives >= Tuning.MAX_LIVES) return false
        return money >= cost(type)
    }

    fun buy(type: UpgradeType): Boolean {
        if (!canBuy(type)) return false
        money -= cost(type)
        applyUpgrade(type)
        return true
    }

    fun makeHud(): BdHud {
        hudDirty = false
        return BdHud(money, lives, wave, score, turrets.size, weapon, powerMask(), megaCharges, status)
    }

    // ------------------------------------------------------------------ debug hooks (real code paths)

    fun debugAddMoney(amount: Int) { money += amount }

    fun debugMaxUpgrades() {
        for (t in UpgradeType.entries) {
            while (levels[t.ordinal] < t.maxLevel) {
                if (t == UpgradeType.REPAIR) break
                applyUpgrade(t)
            }
        }
        lives = Tuning.MAX_LIVES
    }

    fun debugHeal() { lives = Tuning.MAX_LIVES }

    fun debugSpawn(type: EnemyType) { spawnEnemy(type) }

    fun debugSpawnPickup(type: PowerUpType) { spawnPickup(type, rng.nextFloat() * (Tuning.WORLD_W - 60f) + 30f, 10f) }

    fun debugKillAll() {
        for (i in enemies.indices) enemies[i].hp = 0f
        reap()
    }

    fun debugJumpToWave(target: Int) {
        wave = target.coerceIn(1, Tuning.TOTAL_WAVES)
        for (i in enemies.indices) enemyPool.release(enemies[i])
        enemies.clear()
        waveActive = false
        toSpawn = 0
        breakTicks = 30
        banner = Waves.title(wave); bannerTicks = 120
    }

    fun debugSkipWave() { debugJumpToWave(wave + 1) }

    // ------------------------------------------------------------------ simulation

    private fun step() {
        if (status != GameStatus.PLAYING) {
            particles.update(); texts.update()
            return
        }
        time++
        updateWaves()
        updateTurrets()
        updateBullets()
        reap()
        updateEnemies()
        updatePickups()
        tickPowerUps()
        particles.update()
        texts.update()
        shake *= 0.9f
        if (shake < 0.05f) shake = 0f
        if (groundFlash > 0) groundFlash--
        if (bannerTicks > 0) bannerTicks--
        if (aimTicks > 0 && !touching) aimTicks--
    }

    private fun updateWaves() {
        if (breakTicks > 0) {
            breakTicks--
            if (breakTicks == 0) beginWave()
            return
        }
        if (!waveActive) return
        if (toSpawn > 0 || bossPending) {
            val half = Waves.enemyCount(wave) / 2
            if (bossPending && toSpawn <= half) {
                Waves.bossFor(wave)?.let { spawnEnemy(it) }
                bossPending = false
            }
            spawnTimer--
            if (toSpawn > 0 && spawnTimer <= 0) {
                spawnEnemy(Waves.pick(wave, rng.nextFloat()))
                toSpawn--
                spawnTimer = Waves.spawnInterval(wave)
            }
        } else if (enemies.isEmpty()) {
            completeWave()
        }
    }

    private fun beginWave() {
        waveActive = true
        toSpawn = Waves.enemyCount(wave)
        spawnTimer = 20
        bossPending = Waves.bossFor(wave) != null
        banner = Waves.title(wave)
        bannerTicks = 110
    }

    private fun completeWave() {
        waveActive = false
        val bonus = Waves.clearBonus(wave)
        money += bonus
        moneyEarned += bonus
        score += bonus
        if (wave >= Tuning.TOTAL_WAVES) {
            status = GameStatus.VICTORY
            return
        }
        wave++
        breakTicks = Waves.BREAK_TICKS
        banner = "Wave cleared!  +$$bonus"
        bannerTicks = 110
    }

    private fun spawnEnemy(type: EnemyType) {
        if (enemies.size >= Tuning.MAX_ENEMIES) return
        val e = enemyPool.obtain()
        e.type = type
        e.size = type.size
        e.x = e.size / 2f + rng.nextFloat() * (Tuning.WORLD_W - e.size)
        e.y = -e.size
        e.maxHp = Waves.baseHealth(wave) * type.hpMult
        e.hp = e.maxHp
        e.speed = Waves.baseSpeed(wave) * type.speedMult
        e.reward = (Waves.baseReward(wave) * type.rewardMult).toInt()
        e.burn = 0; e.freeze = 0; e.flash = 0
        e.phase = rng.nextFloat() * 6.28f
        enemies.add(e)
    }

    // ---- turrets

    private fun layoutTurrets() {
        val n = turrets.size
        for (i in 0 until n) turrets[i].x = Tuning.WORLD_W * (i + 1) / (n + 1)
    }

    /** Pivot of every barrel: the top of the bunker. */
    val pivotY: Float get() = groundY - 16f

    private fun updateTurrets() {
        val manual = aimTicks > 0
        for (i in turrets.indices) {
            val t = turrets[i]
            if (t.cooldown > 0f) t.cooldown -= 1f
            t.recoil *= 0.8f
            if (t.flash > 0) t.flash--

            var has = false
            var tx = 0f
            var ty = 0f
            if (manual) {
                has = true; tx = aimX; ty = aimY
            } else if (settings.autoAim) {
                val target = pickTarget(t)
                if (target != null) {
                    has = true
                    val dx = target.x - t.x
                    val dy = target.y - pivotY
                    val flight = sqrt(dx * dx + dy * dy) / max(1f, effectiveBulletSpeed(t))
                    tx = target.x
                    ty = target.y + target.speed * flight
                }
            }
            if (has) {
                var desired = atan2(tx - t.x, pivotY - ty)
                desired = desired.coerceIn(-1.45f, 1.45f)
                t.angle += (desired - t.angle) * 0.3f
                if (t.cooldown <= 0f && kotlin.math.abs(desired - t.angle) < 0.1f) fire(t)
            } else {
                t.angle *= 0.92f
            }
        }
    }

    private fun effectiveBulletSpeed(t: Turret): Float = bulletSpeed * weapon.speedMult

    /** Nearest threat, with a bonus for enemies already low on the screen; each base's own x makes bases choose differently. */
    private fun pickTarget(t: Turret): Enemy? {
        var best: Enemy? = null
        var bestScore = Float.MAX_VALUE
        for (i in enemies.indices) {
            val e = enemies[i]
            if (e.y < 0f || e.hp <= 0f) continue
            val dx = e.x - t.x
            val dy = e.y - pivotY
            val s = sqrt(dx * dx + dy * dy) - e.y * 0.6f
            if (s < bestScore) { bestScore = s; best = e }
        }
        return best
    }

    private fun fire(t: Turret) {
        val role = t.role
        val mega = megaCharges > 0
        val pellets = if (mega) 1 else role.pellets
        val speed = bulletSpeed * weapon.speedMult
        val dmg = damage * weapon.damageMult * role.damageMult * (if (mega) 3f else 1f)
        for (p in 0 until pellets) {
            if (bullets.size >= Tuning.MAX_BULLETS) break
            var a = t.angle + (p - (pellets - 1) * 0.5f) * role.spread
            if (role.jitter > 0f) a += (rng.nextFloat() - 0.5f) * role.jitter
            val dx = sin(a)
            val dy = -cos(a)
            val b = bulletPool.obtain()
            b.x = t.x + dx * role.barrel
            b.y = pivotY + dy * role.barrel
            b.vx = dx * speed
            b.vy = dy * speed
            b.damage = dmg
            b.weapon = weapon
            b.mega = mega
            b.size = if (mega) 7f else 3.5f + min(2.5f, dmg / 60f)
            b.pierceLeft = role.pierce + (if (mega) 3 else 0)
            b.hitCount = 0
            bullets.add(b)
            particles.spark(b.x, b.y, dx, dy, weapon.color)
        }
        if (mega) megaCharges--
        t.cooldown = fireIntervalFor(role)
        t.recoil = 6f
        t.flash = 5
        shotsFired += pellets
    }

    private fun fireIntervalFor(role: TurretRole): Float {
        val rapid = if (rapidTicks > 0) 0.4f else 1f
        return max(Tuning.MIN_FIRE_INTERVAL, fireInterval * role.fireMult * rapid)
    }

    // ---- bullets & enemies

    private fun updateBullets() {
        var i = 0
        while (i < bullets.size) {
            val b = bullets[i]
            b.x += b.vx
            b.y += b.vy
            var remove = b.y < -30f || b.x < -30f || b.x > Tuning.WORLD_W + 30f || b.y > worldH
            if (!remove) remove = collideBullet(b)
            if (remove) {
                bulletPool.release(b)
                val last = bullets.size - 1
                bullets[i] = bullets[last]
                bullets.removeAt(last)
            } else {
                i++
            }
        }
    }

    /** @return true if the bullet is spent. */
    private fun collideBullet(b: Bullet): Boolean {
        for (j in pickups.indices) {
            val p = pickups[j]
            val dx = p.x - b.x
            val dy = p.y - b.y
            if (dx * dx + dy * dy < 20f * 20f) {
                collect(p)
                removePickup(j)
                return false
            }
        }
        for (j in enemies.indices) {
            val e = enemies[j]
            if (e.hp <= 0f) continue
            val dx = e.x - b.x
            val dy = e.y - b.y
            val r = e.size * 0.5f + b.size
            if (dx * dx + dy * dy > r * r) continue
            if (alreadyHit(b, e)) continue
            hitEnemy(e, b.damage, b.weapon, b.x, b.y)
            if (b.weapon.splash > 0f) explode(b, e)
            if (b.pierceLeft > 0) {
                b.pierceLeft--
                if (b.hitCount < b.hits.size) b.hits[b.hitCount++] = e
                continue
            }
            return true
        }
        return false
    }

    private fun alreadyHit(b: Bullet, e: Enemy): Boolean {
        for (k in 0 until b.hitCount) if (b.hits[k] === e) return true
        return false
    }

    private fun explode(b: Bullet, centre: Enemy) {
        val r = b.weapon.splash
        particles.ring(b.x, b.y, b.weapon.color, r)
        particles.burst(b.x, b.y, b.weapon.color, if (settings.reducedEffects) 4 else 9, 2.6f, 3f)
        for (j in enemies.indices) {
            val e = enemies[j]
            if (e === centre || e.hp <= 0f) continue
            val dx = e.x - b.x
            val dy = e.y - b.y
            if (dx * dx + dy * dy < (r + e.size * 0.5f) * (r + e.size * 0.5f)) {
                hitEnemy(e, b.damage * 0.6f, b.weapon, e.x, e.y)
            }
        }
    }

    private fun hitEnemy(e: Enemy, dmg: Float, w: WeaponType, hx: Float, hy: Float) {
        e.hp -= dmg
        e.flash = 4
        if (w.burnTicks > 0) e.burn = w.burnTicks
        if (w.freezeTicks > 0) e.freeze = w.freezeTicks
        particles.ring(hx, hy, w.color, 9f, 10)
        if (settings.damageNumbers) texts.add(NumberStrings.of(dmg.toInt()), e.x, e.y - e.size * 0.5f, Color.White)
    }

    /** Removes every enemy at 0 HP, paying out and spawning effects. Swap-remove keeps it O(n). */
    private fun reap() {
        var i = 0
        while (i < enemies.size) {
            val e = enemies[i]
            if (e.hp <= 0f) {
                onKilled(e)
                enemyPool.release(e)
                val last = enemies.size - 1
                enemies[i] = enemies[last]
                enemies.removeAt(last)
            } else {
                i++
            }
        }
    }

    private fun onKilled(e: Enemy) {
        kills++
        val t = e.type
        score += (10 * t.rewardMult).toInt()
        money += e.reward
        moneyEarned += e.reward
        val boss = t.leakDamage > 1
        particles.burst(e.x, e.y, t.color, if (settings.reducedEffects) 6 else (if (boss) 34 else 14), if (boss) 4.5f else 3f, if (boss) 5f else 3.5f)
        particles.ring(e.x, e.y, t.color, e.size * 0.9f, 22)
        texts.add("+$" + NumberStrings.of(e.reward), e.x, e.y, Color(0xFFFFD54F), boss)
        if (boss) {
            shake = min(10f, shake + 8f)
            hapticPulses++
        }
        if (boss || rng.nextFloat() < 0.07f) {
            spawnPickup(PowerUpType.entries[rng.nextInt(PowerUpType.entries.size)], e.x, e.y)
        }
    }

    private fun updateEnemies() {
        val slow = slowTicks > 0
        var i = 0
        while (i < enemies.size) {
            val e = enemies[i]
            var spd = e.speed
            if (slow) spd *= 0.35f
            if (e.freeze > 0) { spd *= 0.4f; e.freeze-- }
            if (e.burn > 0) {
                e.burn--
                if (e.burn % 30 == 0) e.hp -= 5f
                if (e.burn % 6 == 0) particles.spark(e.x + (rng.nextFloat() - 0.5f) * e.size * 0.6f, e.y - e.size * 0.3f, 0f, -0.8f, Color(0xFFFFB74D))
            }
            if (e.flash > 0) e.flash--
            e.y += spd
            e.x += sin((time + e.phase * 10f) * 0.06f) * e.type.sway
            val half = e.size * 0.5f
            e.x = e.x.coerceIn(half, Tuning.WORLD_W - half)
            if (e.y + half >= groundY) {
                leak(e)
                enemyPool.release(e)
                val last = enemies.size - 1
                enemies[i] = enemies[last]
                enemies.removeAt(last)
            } else {
                i++
            }
        }
        if (lives <= 0 && status == GameStatus.PLAYING) status = GameStatus.GAME_OVER
    }

    private fun leak(e: Enemy) {
        particles.burst(e.x, groundY, Color(0xFFFF5252), 10, 3f, 3f)
        groundFlash = 14
        if (shieldTicks > 0) {
            particles.ring(e.x, groundY, Color(0xFF66BB6A), 30f)
            return
        }
        if (!godMode) lives = max(0, lives - e.type.leakDamage)
        shake = min(10f, shake + 6f)
        hapticPulses++
    }

    // ---- pickups & power-ups

    private fun spawnPickup(type: PowerUpType, x: Float, y: Float) {
        if (pickups.size >= Tuning.MAX_PICKUPS) return
        val p = pickupPool.obtain()
        p.type = type
        p.x = x.coerceIn(20f, Tuning.WORLD_W - 20f)
        p.y = y
        p.phase = rng.nextFloat() * 6.28f
        pickups.add(p)
    }

    private fun removePickup(index: Int) {
        pickupPool.release(pickups[index])
        val last = pickups.size - 1
        pickups[index] = pickups[last]
        pickups.removeAt(last)
    }

    private fun updatePickups() {
        var i = 0
        while (i < pickups.size) {
            val p = pickups[i]
            p.y += 0.7f
            if (p.y > groundY) removePickup(i) else i++
        }
    }

    private fun collect(p: Pickup) {
        val t = p.type
        particles.ring(p.x, p.y, t.color, 26f)
        particles.burst(p.x, p.y, t.color, 8, 2.5f, 3f)
        texts.add(t.label + "!", p.x, p.y, t.color, true)
        when (t) {
            PowerUpType.RAPID_FIRE -> rapidTicks = t.duration
            PowerUpType.SHIELD -> shieldTicks = t.duration
            PowerUpType.TIME_SLOW -> slowTicks = t.duration
            PowerUpType.MEGA_SHOT -> megaCharges = turrets.size
        }
    }

    private fun tickPowerUps() {
        if (rapidTicks > 0) rapidTicks--
        if (shieldTicks > 0) shieldTicks--
        if (slowTicks > 0) slowTicks--
        val mask = powerMask()
        if (mask != lastPowerMask) { lastPowerMask = mask; hudDirty = true }
    }

    /** Bit per [PowerUpType] ordinal that is currently active. */
    private fun powerMask(): Int {
        var m = 0
        if (rapidTicks > 0) m = m or (1 shl PowerUpType.RAPID_FIRE.ordinal)
        if (shieldTicks > 0) m = m or (1 shl PowerUpType.SHIELD.ordinal)
        if (slowTicks > 0) m = m or (1 shl PowerUpType.TIME_SLOW.ordinal)
        if (megaCharges > 0) m = m or (1 shl PowerUpType.MEGA_SHOT.ordinal)
        return m
    }

    // ---- upgrades

    private fun applyUpgrade(type: UpgradeType) {
        levels[type.ordinal]++
        when (type) {
            UpgradeType.EXTRA_BASE -> {
                turrets.add(Turret(TurretRole.forSlot(turrets.size)))
                layoutTurrets()
                val t = turrets[turrets.size - 1]
                particles.ring(t.x, groundY - 10f, t.role.color, 36f, 26)
            }
            UpgradeType.REPAIR -> lives = min(Tuning.MAX_LIVES, lives + 1)
            else -> Unit
        }
        recomputeStats()
        hudDirty = true
    }

    private fun recomputeStats() {
        damage = Tuning.BASE_DAMAGE + levels[UpgradeType.DAMAGE.ordinal] * Tuning.DAMAGE_PER_LEVEL
        bulletSpeed = Tuning.BASE_BULLET_SPEED + levels[UpgradeType.BULLET_SPEED.ordinal] * Tuning.SPEED_PER_LEVEL
        fireInterval = Tuning.BASE_FIRE_INTERVAL * Tuning.FIRE_RATE_FACTOR.pow(levels[UpgradeType.FIRE_RATE.ordinal])
    }
}
