package com.baraka.game.knightsurvivor.coop

import com.baraka.game.coop.CoopLink
import com.baraka.game.coop.SimulatedPartner
import com.baraka.game.knightsurvivor.model.ArenaSize
import com.baraka.game.knightsurvivor.model.GameState
import com.baraka.game.knightsurvivor.model.HealthPickup
import com.baraka.game.knightsurvivor.model.KnightDesign
import com.baraka.game.knightsurvivor.model.PlayerState
import com.baraka.game.knightsurvivor.model.Vec2
import com.baraka.game.knightsurvivor.model.WeaponKind
import com.baraka.game.knightsurvivor.model.XpGem
import com.baraka.game.knightsurvivor.systems.GameLoopOrchestrator

/** What the host knows about its co-op partner's connection — read by the HUD. */
class PartnerConnectionState {
    var connected = true
    /** The partner runs a different build of the game, so some of what we send may be ignored on their side. */
    var versionMismatch = false
}

/**
 * Host-side network glue: the host is the only device that ever calls
 * systems/GameLoopOrchestrator.kt. This class applies incoming messages onto [GameState]
 * (creating `GameState.partner` once the guest announces its weapon deck, routing its
 * joystick input and level-up picks) and streams outgoing [KnightCoopMsg.Snapshot]s so the
 * guest has something to render. ui/KnightSurvivorScreen.kt owns the actual [GameState] and
 * calls [receive] before each [GameLoopOrchestrator.tick] and [send] after it.
 */
class KnightCoopHostSession(
    private val link: CoopLink<KnightCoopMsg>,
    private val hostKnightDesign: KnightDesign,
    /** Builds the partner's starting [PlayerState] from their chosen deck — supplied by the caller since it already knows how to build the host's own starting player (see ui/KnightSurvivorScreen.kt#createInitialGameState). */
    private val createPartnerPlayer: (weaponDeck: Set<WeaponKind>) -> PlayerState
) {
    val partnerConnection = PartnerConnectionState()
    var partnerKnightDesign: KnightDesign = KnightDesign.SWORN_BLADE
        private set

    private var sendTimer = 0f
    private var lastSentChoiceCount = 0

    init {
        link.send(KnightCoopMsg.Hello(KNIGHT_COOP_PROTOCOL, hostKnightDesign))
    }

    /** Applies every message received since the last call onto [state]. Call once per UI frame, before ticking the simulation. */
    fun receive(state: GameState): GameState {
        var current = state
        var msg = link.poll()
        while (msg != null) {
            current = handle(current, msg)
            msg = link.poll()
        }
        if (partnerConnection.connected && !link.connected) {
            partnerConnection.connected = false
        }
        return current
    }

    /** Streams a snapshot (throttled to [SNAPSHOT_INTERVAL_SECONDS]) and any newly-pending level-up choices. Call once per UI frame, after ticking the simulation. */
    fun send(state: GameState, dtSeconds: Float) {
        link.tick(dtSeconds)

        if (state.pendingPartnerUpgradeChoices.size != lastSentChoiceCount) {
            link.send(KnightCoopMsg.LevelUpChoices(state.pendingPartnerUpgradeChoices.map { it.toDto() }))
            lastSentChoiceCount = state.pendingPartnerUpgradeChoices.size
        }

        sendTimer += dtSeconds
        if (sendTimer < SNAPSHOT_INTERVAL_SECONDS) return
        sendTimer = 0f
        link.send(state.toSnapshotMsg())
    }

    fun close() {
        link.close()
    }

    private fun handle(state: GameState, msg: KnightCoopMsg): GameState = when (msg) {
        is KnightCoopMsg.Hello -> {
            partnerKnightDesign = msg.knightDesign
            if (msg.version != KNIGHT_COOP_PROTOCOL) partnerConnection.versionMismatch = true
            state
        }
        is KnightCoopMsg.GuestReady -> state.copy(
            partner = createPartnerPlayer(msg.weaponDeck),
            partnerWeaponDeck = msg.weaponDeck
        )
        is KnightCoopMsg.Input -> state.copy(partnerJoystickInput = msg.direction)
        is KnightCoopMsg.ChoiceSelected -> {
            val choice = state.pendingPartnerUpgradeChoices.getOrNull(msg.index)
            if (choice != null) GameLoopOrchestrator.applyPartnerUpgradeChoice(state, choice) else state
        }
        KnightCoopMsg.Bye -> {
            partnerConnection.connected = false
            state
        }
        is KnightCoopMsg.Snapshot, is KnightCoopMsg.LevelUpChoices -> state // host -> guest only, never received here
    }

    private companion object {
        const val SNAPSHOT_INTERVAL_SECONDS = 0.1f
    }
}

/**
 * Guest-side network glue: the guest never simulates anything — it only renders the most
 * recent [KnightCoopMsg.Snapshot] from the host (via [latestRenderState], reconstructed
 * into a plain [GameState] so ui/KnightSurvivorScreen.kt can hand it to render/GameCanvas.kt
 * exactly like a normal run) and sends its own joystick input / level-up picks back.
 */
class KnightCoopGuestSession(
    private val link: CoopLink<KnightCoopMsg>,
    private val ownKnightDesign: KnightDesign,
    ownWeaponDeck: Set<WeaponKind>
) {
    var hostKnightDesign: KnightDesign = KnightDesign.SWORN_BLADE
        private set
    var versionMismatch = false
        private set
    var latestRenderState: GameState? = null
        private set
    var pendingChoices: List<UpgradeOptionDto> = emptyList()
        private set
    val connected: Boolean get() = link.connected

    init {
        link.send(KnightCoopMsg.Hello(KNIGHT_COOP_PROTOCOL, ownKnightDesign))
        link.send(KnightCoopMsg.GuestReady(ownWeaponDeck))
    }

    /** Call once per UI frame: applies any messages received since the last call. [arenaSize] is this device's own measured canvas size, exactly like a solo run uses. */
    fun poll(arenaSize: ArenaSize) {
        var msg = link.poll()
        while (msg != null) {
            handle(msg, arenaSize)
            msg = link.poll()
        }
    }

    fun sendInput(direction: Vec2) {
        link.send(KnightCoopMsg.Input(direction))
    }

    /** Sends the pick and dismisses the local overlay immediately — optimistic, since waiting for the host to echo back "choice applied" would just add a round-trip's worth of visible lag on a screen with nothing else to show. */
    fun selectChoice(index: Int) {
        link.send(KnightCoopMsg.ChoiceSelected(index))
        pendingChoices = emptyList()
    }

    fun close() {
        link.send(KnightCoopMsg.Bye)
        link.close()
    }

    private fun handle(msg: KnightCoopMsg, arenaSize: ArenaSize) {
        when (msg) {
            is KnightCoopMsg.Hello -> {
                hostKnightDesign = msg.knightDesign
                if (msg.version != KNIGHT_COOP_PROTOCOL) versionMismatch = true
            }
            is KnightCoopMsg.Snapshot -> msg.toGameState(arenaSize)?.let { latestRenderState = it }
            is KnightCoopMsg.LevelUpChoices -> pendingChoices = msg.options
            is KnightCoopMsg.GuestReady, is KnightCoopMsg.Input, is KnightCoopMsg.ChoiceSelected -> {} // guest -> host only, never received here
            KnightCoopMsg.Bye -> {} // link.connected already reflects a closed socket; nothing else to do
        }
    }
}

private fun GameState.toSnapshotMsg(): KnightCoopMsg.Snapshot = KnightCoopMsg.Snapshot(
    elapsedMs = elapsedMs,
    phase = phase,
    killCount = killCount,
    bestSurvivalTimeMs = bestSurvivalTimeMsAtStart,
    host = player.toSnapshot(),
    partner = partner?.toSnapshot(),
    enemies = enemies.map { it.toSnapshot() },
    projectiles = projectiles.map { it.toSnapshot() },
    enemyProjectiles = enemyProjectiles.map { it.toSnapshot() },
    xpGemPositions = xpGems.map { it.position },
    healthPickupPositions = healthPickups.map { it.position }
)

/** Null until the host has actually created our PlayerState (a brief window right after connecting, before GuestReady is processed) — the caller shows a "connecting" state until then. */
private fun KnightCoopMsg.Snapshot.toGameState(arenaSize: ArenaSize): GameState? {
    val ownPlayer = partner?.toPlayerState() ?: return null
    return GameState(
        player = ownPlayer,
        partner = host.toPlayerState(),
        enemies = enemies.map { it.toEnemyInstance() },
        projectiles = projectiles.map { it.toProjectile() },
        enemyProjectiles = enemyProjectiles.map { it.toEnemyProjectile() },
        xpGems = xpGemPositions.mapIndexed { index, position -> XpGem(id = index.toLong(), position = position, value = 0) },
        healthPickups = healthPickupPositions.mapIndexed { index, position -> HealthPickup(id = index.toLong(), position = position, healAmount = 0) },
        elapsedMs = elapsedMs,
        phase = phase,
        arenaSize = arenaSize,
        killCount = killCount,
        bestSurvivalTimeMsAtStart = bestSurvivalTimeMs
    )
}

/**
 * A pretend partner for trying co-op on one phone: a simple bot that charges the nearest
 * enemy and auto-picks the first level-up option after a short "thinking" delay. Reacts to
 * whatever [KnightCoopMsg.Snapshot] the host sends it, same as a real guest would.
 */
class SimulatedKnightPartner(weaponDeck: Set<WeaponKind> = WeaponKind.entries.toSet()) : SimulatedPartner<KnightCoopMsg>() {
    private var latestSnapshot: KnightCoopMsg.Snapshot? = null
    private var chooseDelay = 0f

    init {
        deliver(KnightCoopMsg.Hello(KNIGHT_COOP_PROTOCOL, KnightDesign.BERSERKER))
        deliver(KnightCoopMsg.GuestReady(weaponDeck))
    }

    override fun send(msg: KnightCoopMsg) {
        when (msg) {
            is KnightCoopMsg.Snapshot -> latestSnapshot = msg
            is KnightCoopMsg.LevelUpChoices -> if (msg.options.isNotEmpty()) chooseDelay = 0.4f
            else -> {}
        }
    }

    override fun tick(dt: Float) {
        if (!connected) return

        val snapshot = latestSnapshot
        val partner = snapshot?.partner
        if (snapshot != null && partner != null) {
            val nearestEnemy = snapshot.enemies.minByOrNull { it.position.distanceTo(partner.position) }
            val direction = nearestEnemy?.let { (it.position - partner.position).normalized() } ?: Vec2.ZERO
            deliver(KnightCoopMsg.Input(direction))
        }

        if (chooseDelay > 0f) {
            chooseDelay -= dt
            if (chooseDelay <= 0f) deliver(KnightCoopMsg.ChoiceSelected(0))
        }
    }
}
