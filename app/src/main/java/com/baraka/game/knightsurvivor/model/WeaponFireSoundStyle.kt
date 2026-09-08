package com.baraka.game.knightsurvivor.model

/**
 * How a weapon's fire sound is synthesized — see data/SurvivorSoundEngine.kt for the actual
 * math. Pure identifier, same reasoning as [Waveform]: keeps synthesis details out of model/.
 */
enum class WeaponFireSoundStyle {
    /** A single flat pitch — reserved for anything that's genuinely just a plain beep-like cue. */
    TONE,

    /** Glides from [WeaponKind.fireSoundHz] to [WeaponKind.fireSoundEndHz] — a quick blade "shing" or similar slide. */
    SWEEP,

    /** Several inharmonic sine partials ringing out at different decay rates — a bell/clang instead of a flat beep. */
    METALLIC,

    /** [METALLIC] plus a short noise "thwack" mixed into the attack — a solid-object impact, like a pan bonk. */
    METALLIC_IMPACT,

    /** A downward-swept buzzy carrier with fast amplitude tremolo — the closest a plain oscillator gets to a duck's quack. */
    QUACK,

    /** Harmonic (not inharmonic) sine partials decaying slowly — a warm chime/shimmer instead of a metallic clang. */
    CHIME,

    /** Filtered noise with a fast decay — a crackle/pop, e.g. a firecracker or a wet splat. */
    NOISE_POP,

    /** Heavily-filtered (duller) noise with a swell-then-fade envelope — an air-movement whoosh. */
    WHOOSH,

    /** A tone with slow amplitude tremolo — a throbbing pulse rather than a flat hum. */
    PULSE
}
