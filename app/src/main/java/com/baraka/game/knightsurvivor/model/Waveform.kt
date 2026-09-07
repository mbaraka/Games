package com.baraka.game.knightsurvivor.model

/**
 * A basic oscillator shape. Pure identifier — no synthesis math here (see
 * data/SurvivorSoundEngine.kt for that) — so [WeaponKind] and other model types can carry
 * one as part of their own data (like they already carry a color or an icon) without
 * pulling any Android audio API into model/.
 */
enum class Waveform { SINE, SQUARE, SAWTOOTH }
