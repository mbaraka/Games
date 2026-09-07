package com.baraka.game.knightsurvivor.model

/**
 * "Play a sound for this" — decided each tick by systems/GameLoopOrchestrator.kt purely
 * from what happened (a weapon fired, an enemy was hit/died, the player took damage, a
 * level-up or the run's end), collected on [GameState.pendingSoundCues], and drained by
 * ui/KnightSurvivorScreen.kt into data/SurvivorSoundEngine.kt's actual playback. The
 * decision of WHEN a sound should play is pure and testable; only the playback is
 * Android-dependent.
 */
enum class SoundCue {
    WEAPON_FIRE,
    ENEMY_HIT,
    ENEMY_DEATH,
    PLAYER_HURT,
    LEVEL_UP,
    GAME_OVER,
    VICTORY
}
