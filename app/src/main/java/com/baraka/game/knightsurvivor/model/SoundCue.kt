package com.baraka.game.knightsurvivor.model

/**
 * "Play a sound for this" — decided each tick by systems/GameLoopOrchestrator.kt purely
 * from what happened (an enemy was hit/died, the player took damage, a level-up or the
 * run's end), collected on [GameState.pendingSoundCues], and drained by
 * ui/KnightSurvivorScreen.kt into data/SurvivorSoundEngine.kt's actual playback. The
 * decision of WHEN a sound should play is pure and testable; only the playback is
 * Android-dependent.
 *
 * A weapon firing is NOT one of these cues: each [WeaponKind] carries its own pitch/
 * timbre ([WeaponKind.fireSoundHz]/[WeaponKind.fireSoundWaveform]) instead, so a fired
 * weapon is tracked separately on [GameState.pendingWeaponFireSounds] — a fixed cue list
 * would need a new case added for every future weapon.
 */
enum class SoundCue {
    ENEMY_HIT,
    ENEMY_DEATH,
    PLAYER_HURT,
    LEVEL_UP,
    GAME_OVER,
    VICTORY
}
