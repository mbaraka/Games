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
    val bestSurvivalTimeMsAtStart: Long = 0L
) {
    val survivalTimeMs: Long get() = elapsedMs
    val isNewBestTime: Boolean get() = survivalTimeMs > bestSurvivalTimeMsAtStart
}
