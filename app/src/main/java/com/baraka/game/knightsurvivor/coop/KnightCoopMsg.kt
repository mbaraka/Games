package com.baraka.game.knightsurvivor.coop

import com.baraka.game.coop.CoopCodec
import com.baraka.game.coop.CoopSpec
import com.baraka.game.knightsurvivor.model.EnemyInstance
import com.baraka.game.knightsurvivor.model.EnemyKind
import com.baraka.game.knightsurvivor.model.EnemyProjectile
import com.baraka.game.knightsurvivor.model.GamePhase
import com.baraka.game.knightsurvivor.model.KnightDesign
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Projectile
import com.baraka.game.knightsurvivor.model.UpgradeDisplay
import com.baraka.game.knightsurvivor.model.UpgradeOption
import com.baraka.game.knightsurvivor.model.Vec2
import com.baraka.game.knightsurvivor.model.VectorIcon
import com.baraka.game.knightsurvivor.model.WeaponInstance
import com.baraka.game.knightsurvivor.model.WeaponKind
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID

const val KNIGHT_COOP_PROTOCOL = 1

/**
 * Only the host ever runs systems/GameLoopOrchestrator.kt — the guest is a pure thin
 * client: it sends its joystick input and upgrade picks, and renders whatever [Snapshot]
 * the host last sent using the exact same render/GameCanvas.kt the host uses for itself
 * (see KnightCoopSession.kt). A snapshot therefore only needs to carry what rendering
 * actually reads — HP bars and weapon icons, not internal bookkeeping like pierce counts
 * or cooldown timers — which keeps each message small and the codec short.
 *
 * Known v1 simplification: particles, enemy stun/slow visual indicators, and the guest's
 * own sound reactions to world events are not synced — only the two players' own hurt/
 * level-up cues matter gameplay-wise, and those already happen locally on each device.
 */
sealed class KnightCoopMsg {
    /** Sent by BOTH sides right after connecting: protocol version (mismatch is surfaced, not acted on) and the sender's own chosen knight look, so the other side knows how to render it. */
    class Hello(val version: Int, val knightDesign: KnightDesign) : KnightCoopMsg()
    /** Guest -> host, right after its own [Hello]: which weapons the guest chose for itself (see ui/WeaponDeckPickerScreen.kt) — this is what lets the host create GameState.partner. */
    class GuestReady(val weaponDeck: Set<WeaponKind>) : KnightCoopMsg()
    /** Guest -> host, sent whenever the joystick vector changes. */
    class Input(val direction: Vec2) : KnightCoopMsg()
    /** Host -> guest, ~8x/second: everything needed to render one frame. */
    class Snapshot(
        val elapsedMs: Long,
        val phase: GamePhase,
        val killCount: Int,
        val bestSurvivalTimeMs: Long,
        val host: PlayerSnapshot,
        val partner: PlayerSnapshot?,
        val enemies: List<EnemySnapshot>,
        val projectiles: List<ProjectileSnapshot>,
        val enemyProjectiles: List<EnemyProjectileSnapshot>,
        val xpGemPositions: List<Vec2>,
        val healthPickupPositions: List<Vec2>
    ) : KnightCoopMsg()
    /** Host -> guest: the guest's own rolled level-up choices (host already rolled these into GameState.pendingPartnerUpgradeChoices). Empty list means "no choice pending anymore". */
    class LevelUpChoices(val options: List<UpgradeOptionDto>) : KnightCoopMsg()
    /** Guest -> host: index picked from the most recent [LevelUpChoices]. */
    class ChoiceSelected(val index: Int) : KnightCoopMsg()
    object Bye : KnightCoopMsg()
}

/** Just what render/GameCanvas.kt's drawPlayers/Hud actually read — see [KnightCoopMsg]'s KDoc. */
data class PlayerSnapshot(
    val position: Vec2,
    val hp: Int,
    val maxHp: Int,
    val level: Int,
    val xp: Int,
    val xpToNextLevel: Int,
    val invulnerableUntilMs: Long,
    val weapons: List<WeaponSnapshot>
)

data class WeaponSnapshot(val kind: WeaponKind, val level: Int, val orbitAngleRadians: Float)
data class EnemySnapshot(val id: Long, val kind: EnemyKind, val position: Vec2, val hp: Int, val maxHp: Int, val disguisedAsPickup: Boolean)
data class ProjectileSnapshot(val position: Vec2, val velocity: Vec2, val sourceWeapon: WeaponKind)
data class EnemyProjectileSnapshot(val position: Vec2, val colorArgb: Long)

/** A lightweight, over-the-wire stand-in for [UpgradeOption] — the guest only needs to display title/description/color (see UpgradeOption's KDoc), and the host maps [ChoiceSelected]'s index straight back to its own real GameState.pendingPartnerUpgradeChoices entry, so neither side needs to reconstruct the real sealed type from this DTO. */
data class UpgradeOptionDto(
    override val title: String,
    override val description: String,
    override val colorArgb: Long
) : UpgradeDisplay {
    override val icon: VectorIcon? = null
}

fun UpgradeOption.toDto(): UpgradeOptionDto = UpgradeOptionDto(title, description, colorArgb)

/** Builds a [PlayerSnapshot] from the real simulated [PlayerState] — host-side only. */
fun PlayerState.toSnapshot(): PlayerSnapshot = PlayerSnapshot(
    position = position,
    hp = hp,
    maxHp = effectiveMaxHp,
    level = level,
    xp = xp,
    xpToNextLevel = xpToNextLevel,
    invulnerableUntilMs = invulnerableUntilMs,
    weapons = weapons.map { WeaponSnapshot(it.kind, it.level, it.orbitAngleRadians) }
)

/** Rebuilds a renderable (but not simulation-accurate) [PlayerState] from a received snapshot — guest-side only, feeds render/GameCanvas.kt directly. */
fun PlayerSnapshot.toPlayerState(): PlayerState = PlayerState(
    position = position,
    hp = hp,
    baseMaxHp = maxHp,
    xp = xp,
    level = level,
    xpToNextLevel = xpToNextLevel,
    invulnerableUntilMs = invulnerableUntilMs,
    weapons = weapons.map { WeaponInstance(it.kind, it.level, orbitAngleRadians = it.orbitAngleRadians) }
)

fun EnemyInstance.toSnapshot(): EnemySnapshot = EnemySnapshot(id, kind, position, hp, maxHp, disguisedAsPickup)

fun EnemySnapshot.toEnemyInstance(): EnemyInstance = EnemyInstance(
    id = id, kind = kind, position = position, hp = hp, maxHp = maxHp, speed = 0f, damage = 0, disguisedAsPickup = disguisedAsPickup
)

fun Projectile.toSnapshot(): ProjectileSnapshot = ProjectileSnapshot(position, velocity, sourceWeapon)

fun ProjectileSnapshot.toProjectile(): Projectile = Projectile(
    id = 0L, position = position, velocity = velocity, damage = 0, sourceWeapon = sourceWeapon
)

fun EnemyProjectile.toSnapshot(): EnemyProjectileSnapshot = EnemyProjectileSnapshot(position, colorArgb)

fun EnemyProjectileSnapshot.toEnemyProjectile(): EnemyProjectile = EnemyProjectile(id = 0L, position = position, velocity = Vec2.ZERO, damage = 0, colorArgb = colorArgb)

private fun DataOutputStream.writeVec2(v: Vec2) { writeFloat(v.x); writeFloat(v.y) }
private fun DataInputStream.readVec2(): Vec2 = Vec2(readFloat(), readFloat())

private fun DataOutputStream.writePlayerSnapshot(p: PlayerSnapshot) {
    writeVec2(p.position)
    writeInt(p.hp)
    writeInt(p.maxHp)
    writeInt(p.level)
    writeInt(p.xp)
    writeInt(p.xpToNextLevel)
    writeLong(p.invulnerableUntilMs)
    writeByte(p.weapons.size)
    p.weapons.forEach { w ->
        writeByte(w.kind.ordinal)
        writeByte(w.level)
        writeFloat(w.orbitAngleRadians)
    }
}

private fun DataInputStream.readPlayerSnapshot(): PlayerSnapshot {
    val position = readVec2()
    val hp = readInt()
    val maxHp = readInt()
    val level = readInt()
    val xp = readInt()
    val xpToNextLevel = readInt()
    val invulnerableUntilMs = readLong()
    val weaponCount = readUnsignedByte()
    val weapons = (0 until weaponCount).map {
        val kind = WeaponKind.entries[readUnsignedByte()]
        val weaponLevel = readUnsignedByte()
        val orbitAngle = readFloat()
        WeaponSnapshot(kind, weaponLevel, orbitAngle)
    }
    return PlayerSnapshot(position, hp, maxHp, level, xp, xpToNextLevel, invulnerableUntilMs, weapons)
}

/** Turns [KnightCoopMsg]s into bytes and back for the shared Bluetooth layer (see com/baraka/game/coop/CoopLink.kt). */
object KnightCoopCodec : CoopCodec<KnightCoopMsg> {

    override fun write(msg: KnightCoopMsg, out: DataOutputStream) {
        when (msg) {
            is KnightCoopMsg.Hello -> {
                out.writeByte(1)
                out.writeInt(msg.version)
                out.writeByte(msg.knightDesign.ordinal)
            }
            is KnightCoopMsg.GuestReady -> {
                out.writeByte(2)
                out.writeByte(msg.weaponDeck.size)
                msg.weaponDeck.forEach { out.writeByte(it.ordinal) }
            }
            is KnightCoopMsg.Input -> {
                out.writeByte(3)
                out.writeVec2(msg.direction)
            }
            is KnightCoopMsg.Snapshot -> {
                out.writeByte(4)
                out.writeLong(msg.elapsedMs)
                out.writeByte(msg.phase.ordinal)
                out.writeInt(msg.killCount)
                out.writeLong(msg.bestSurvivalTimeMs)
                out.writePlayerSnapshot(msg.host)
                out.writeBoolean(msg.partner != null)
                msg.partner?.let { out.writePlayerSnapshot(it) }

                out.writeShort(msg.enemies.size)
                msg.enemies.forEach { e ->
                    out.writeLong(e.id)
                    out.writeByte(e.kind.ordinal)
                    out.writeVec2(e.position)
                    out.writeInt(e.hp)
                    out.writeInt(e.maxHp)
                    out.writeBoolean(e.disguisedAsPickup)
                }

                out.writeShort(msg.projectiles.size)
                msg.projectiles.forEach { p ->
                    out.writeVec2(p.position)
                    out.writeVec2(p.velocity)
                    out.writeByte(p.sourceWeapon.ordinal)
                }

                out.writeShort(msg.enemyProjectiles.size)
                msg.enemyProjectiles.forEach { p ->
                    out.writeVec2(p.position)
                    out.writeLong(p.colorArgb)
                }

                out.writeShort(msg.xpGemPositions.size)
                msg.xpGemPositions.forEach { out.writeVec2(it) }

                out.writeShort(msg.healthPickupPositions.size)
                msg.healthPickupPositions.forEach { out.writeVec2(it) }
            }
            is KnightCoopMsg.LevelUpChoices -> {
                out.writeByte(5)
                out.writeByte(msg.options.size)
                msg.options.forEach { option ->
                    out.writeUTF(option.title)
                    out.writeUTF(option.description)
                    out.writeLong(option.colorArgb)
                }
            }
            is KnightCoopMsg.ChoiceSelected -> {
                out.writeByte(6)
                out.writeByte(msg.index)
            }
            KnightCoopMsg.Bye -> out.writeByte(7)
        }
    }

    override fun read(input: DataInputStream): KnightCoopMsg? = when (input.readUnsignedByte()) {
        1 -> KnightCoopMsg.Hello(input.readInt(), KnightDesign.entries[input.readUnsignedByte()])
        2 -> {
            val count = input.readUnsignedByte()
            val deck = (0 until count).map { WeaponKind.entries[input.readUnsignedByte()] }.toSet()
            KnightCoopMsg.GuestReady(deck)
        }
        3 -> KnightCoopMsg.Input(input.readVec2())
        4 -> {
            val elapsedMs = input.readLong()
            val phase = GamePhase.entries[input.readUnsignedByte()]
            val killCount = input.readInt()
            val bestSurvivalTimeMs = input.readLong()
            val host = input.readPlayerSnapshot()
            val hasPartner = input.readBoolean()
            val partner = if (hasPartner) input.readPlayerSnapshot() else null

            val enemyCount = input.readUnsignedShort()
            val enemies = (0 until enemyCount).map {
                val id = input.readLong()
                val kind = EnemyKind.entries[input.readUnsignedByte()]
                val position = input.readVec2()
                val hp = input.readInt()
                val maxHp = input.readInt()
                val disguised = input.readBoolean()
                EnemySnapshot(id, kind, position, hp, maxHp, disguised)
            }

            val projectileCount = input.readUnsignedShort()
            val projectiles = (0 until projectileCount).map {
                val position = input.readVec2()
                val velocity = input.readVec2()
                val weapon = WeaponKind.entries[input.readUnsignedByte()]
                ProjectileSnapshot(position, velocity, weapon)
            }

            val enemyProjectileCount = input.readUnsignedShort()
            val enemyProjectiles = (0 until enemyProjectileCount).map {
                val position = input.readVec2()
                val colorArgb = input.readLong()
                EnemyProjectileSnapshot(position, colorArgb)
            }

            val xpGemCount = input.readUnsignedShort()
            val xpGemPositions = (0 until xpGemCount).map { input.readVec2() }

            val healthPickupCount = input.readUnsignedShort()
            val healthPickupPositions = (0 until healthPickupCount).map { input.readVec2() }

            KnightCoopMsg.Snapshot(elapsedMs, phase, killCount, bestSurvivalTimeMs, host, partner, enemies, projectiles, enemyProjectiles, xpGemPositions, healthPickupPositions)
        }
        5 -> {
            val count = input.readUnsignedByte()
            val options = (0 until count).map {
                UpgradeOptionDto(input.readUTF(), input.readUTF(), input.readLong())
            }
            KnightCoopMsg.LevelUpChoices(options)
        }
        6 -> KnightCoopMsg.ChoiceSelected(input.readUnsignedByte())
        7 -> KnightCoopMsg.Bye
        else -> null
    }
}

/** Knight Survivor's co-op channel on the shared layer (see AGENTS.md, "Co-op for any game") — its own service id so other games can't connect to it. */
val KNIGHT_COOP: CoopSpec<KnightCoopMsg> = CoopSpec(
    serviceId = UUID.fromString("8f3c1a2e-6d4b-4e9a-9c7f-2b5e8a1d4f60"),
    serviceName = "KnightSurvivorCoop",
    codec = KnightCoopCodec,
    bye = KnightCoopMsg.Bye
)
