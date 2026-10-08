package com.baraka.game.trainsurvivor.data

/** Every player-facing option of Train Survivor. Volumes are 0..1. */
data class GameSettings(
    val musicVolume: Float = 0.6f,
    val sfxVolume: Float = 0.9f,
    val ambienceVolume: Float = 0.5f,
    val vibration: Boolean = true,
    val damageNumbers: Boolean = true,
    val screenShake: Boolean = true,
    val showFps: Boolean = false
)
