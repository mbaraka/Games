package com.baraka.game.knightsurvivor.model

/** The lifecycle of a single run. The UI/rendering layer branches on this to decide which overlay (if any) to show. */
enum class GamePhase {
    RUNNING,
    LEVEL_UP_PAUSE,
    GAME_OVER,
    VICTORY
}

/** The bounded play area. Enemies spawn just outside it and the player is clamped inside it. */
data class ArenaSize(val width: Float, val height: Float)

/**
 * The single immutable root of all gameplay state, threaded through
 * systems/GameLoopOrchestrator.kt's `tick()` one copy at a time — the same "big
 * immutable state + pure update()" shape every other game in this app uses, just split
 * across many small systems instead of one big `update()` method.
 */
data class GameState(
    val player: PlayerState,
    val enemies: List<EnemyInstance> = emptyList(),
    val projectiles: List<Projectile> = emptyList(),
    /** Fired by enemies (currently just the Imp) at the player — see systems/EnemyRangedAttackSystem.kt. */
    val enemyProjectiles: List<EnemyProjectile> = emptyList(),
    val xpGems: List<XpGem> = emptyList(),
    val healthPickups: List<HealthPickup> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val recentWeaponActivations: List<WeaponActivationEvent> = emptyList(),
    /** Sounds to play for what just happened this tick. Drained and cleared by ui/KnightSurvivorScreen.kt every tick — never accumulates. */
    val pendingSoundCues: List<SoundCue> = emptyList(),
    /** Which weapon(s) fired this tick, so ui/KnightSurvivorScreen.kt can play that weapon's own fire sound. Drained and cleared every tick, same as [pendingSoundCues]. */
    val pendingWeaponFireSounds: List<WeaponKind> = emptyList(),
    val elapsedMs: Long = 0L,
    val phase: GamePhase = GamePhase.RUNNING,
    val pendingUpgradeChoices: List<UpgradeOption> = emptyList(),
    val arenaSize: ArenaSize,
    val joystickInput: Vec2 = Vec2.ZERO,
    val killCount: Int = 0,
    /** Fixed for the whole run — chosen from settings when the run starts, see ui/KnightSurvivorScreen.kt. */
    val difficulty: Difficulty = Difficulty.NORMAL,
    /** Weapon kinds the player chose as eligible to appear as "New Weapon" upgrade offers this run — see ui/WeaponDeckPickerScreen.kt and systems/UpgradeChoiceSystem.kt. */
    val weaponDeck: Set<WeaponKind> = WeaponKind.entries.toSet(),
    /** Once true for a run, stays true — whether any boss has died this run, for the BOSS_SLAYER achievement. */
    val bossDefeatedThisRun: Boolean = false,
    /** The best survival time persisted from previous runs, loaded once at game start purely for HUD/end-screen display and comparison — this state layer never writes to storage itself, see data/SurvivorStatsRepository.kt. */
    val bestSurvivalTimeMsAtStart: Long = 0L,
    /**
     * The co-op partner's own character, simulated host-side alongside [player] in the same
     * arena — null for a solo run. Each player keeps independent weapons/level/XP (see
     * knightsurvivor/coop/KnightCoopSession.kt); only the arena (enemies, projectiles,
     * pickups) is shared. Only ever non-null on the host device — the guest device never
     * runs GameLoopOrchestrator at all, it just renders whatever snapshot the host last sent.
     */
    val partner: PlayerState? = null,
    /** The partner's current joystick input, relayed from the guest device over Bluetooth. */
    val partnerJoystickInput: Vec2 = Vec2.ZERO,
    /** The partner's own chosen weapon deck (see ui/WeaponDeckPickerScreen.kt) — only meaningful while [partner] is non-null. */
    val partnerWeaponDeck: Set<WeaponKind> = emptySet(),
    /**
     * Upgrade choices rolled for the partner after their own level-up. Deliberately does
     * NOT pause [phase] the way the host's own [pendingUpgradeChoices] does — pausing the
     * whole shared arena because the *other* player leveled up would freeze the host's
     * screen on an overlay with nothing to pick. The partner picks on their own device while
     * the world keeps running; see GameLoopOrchestrator.applyPartnerUpgradeChoiceAndResume.
     */
    val pendingPartnerUpgradeChoices: List<UpgradeOption> = emptyList()
) {
    val survivalTimeMs: Long get() = elapsedMs
    val isNewBestTime: Boolean get() = survivalTimeMs > bestSurvivalTimeMsAtStart
}
