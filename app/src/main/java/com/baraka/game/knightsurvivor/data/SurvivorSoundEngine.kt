package com.baraka.game.knightsurvivor.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.baraka.game.knightsurvivor.model.SoundCue
import com.baraka.game.knightsurvivor.model.Waveform
import com.baraka.game.knightsurvivor.model.WeaponFireSoundStyle
import com.baraka.game.knightsurvivor.model.WeaponKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Plays every [SoundCue] — plus a distinct fire sound per [WeaponKind] — as a short
 * procedurally-synthesized effect. No audio asset files at all, the same "everything is
 * generated code, not a binary asset" approach this game already uses for its icons. Each
 * sound is synthesized once at construction and held in its own dedicated [AudioTrack], so
 * playing it is just "restart from position 0."
 *
 * Pure oscillators alone (see [tone]/[sweep]) all end up sounding like a beep at a
 * different pitch. [WeaponFireSoundStyle] picks a richer synthesis recipe per weapon —
 * inharmonic/harmonic partial stacks for metal-on-metal vs. magical chimes, filtered noise
 * for pops/whooshes, tremolo for a quack or a throbbing pulse — so weapons read as
 * distinct *kinds* of sound, not just distinct pitches, while staying fully procedural.
 *
 * Lives in data/ alongside SurvivorStatsRepository.kt: both are the Android-dependent side
 * of this game, kept out of model/systems so those stay platform-free.
 */
class SurvivorSoundEngine {

    private val sampleRateHz = 44_100
    private val noiseRandom = Random(0) // fixed seed: synthesis happens once at construction, not per-play, so this doesn't need to vary
    private val cueTracks: Map<SoundCue, AudioTrack> = SoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }
    private val weaponFireTracks: Map<WeaponKind, AudioTrack> =
        WeaponKind.entries.associateWith { kind -> buildTrack(weaponFireSamples(kind)) }

    private fun weaponFireSamples(kind: WeaponKind): ShortArray {
        val durationMs = kind.fireSoundDurationMs ?: WEAPON_FIRE_DURATION_MS
        return when (kind.fireSoundStyle) {
            WeaponFireSoundStyle.TONE -> tone(kind.fireSoundHz, durationMs, kind.fireSoundWaveform, WEAPON_FIRE_VOLUME)
            WeaponFireSoundStyle.SWEEP -> sweep(kind.fireSoundHz, kind.fireSoundEndHz ?: kind.fireSoundHz, durationMs, kind.fireSoundWaveform, WEAPON_FIRE_VOLUME)
            WeaponFireSoundStyle.METALLIC -> metallic(kind.fireSoundHz, durationMs, WEAPON_FIRE_VOLUME, inharmonic = true)
            WeaponFireSoundStyle.METALLIC_IMPACT -> mix(
                metallic(kind.fireSoundHz, durationMs, WEAPON_FIRE_VOLUME, inharmonic = true),
                noiseBurst(durationMs = (durationMs * 0.35).toInt().coerceAtLeast(15), volume = WEAPON_FIRE_VOLUME * 1.1f, lowPassAlpha = 0.5)
            )
            WeaponFireSoundStyle.QUACK -> quack(kind.fireSoundHz, kind.fireSoundEndHz ?: kind.fireSoundHz * 0.5, durationMs, WEAPON_FIRE_VOLUME)
            WeaponFireSoundStyle.CHIME -> metallic(kind.fireSoundHz, durationMs, WEAPON_FIRE_VOLUME, inharmonic = false)
            WeaponFireSoundStyle.NOISE_POP -> noiseBurst(durationMs, WEAPON_FIRE_VOLUME, lowPassAlpha = 0.35)
            WeaponFireSoundStyle.WHOOSH -> whoosh(durationMs, WEAPON_FIRE_VOLUME)
            WeaponFireSoundStyle.PULSE -> pulse(kind.fireSoundHz, durationMs, kind.fireSoundWaveform, WEAPON_FIRE_VOLUME)
        }
    }

    /** Restarts the given cue's track from the beginning — safe to call again before a previous play finished. */
    fun play(cue: SoundCue) = restart(cueTracks[cue])

    /** Restarts [kind]'s own fire-sound track — each weapon has a distinct pitch/timbre, see [WeaponKind.fireSoundHz]. */
    fun playWeaponFire(kind: WeaponKind) = restart(weaponFireTracks[kind])

    private fun restart(track: AudioTrack?) {
        track ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    /** Releases every underlying [AudioTrack]. Call once when the game screen leaves composition. */
    fun release() {
        cueTracks.values.forEach { it.release() }
        weaponFireTracks.values.forEach { it.release() }
    }

    private fun buildTrack(samples: ShortArray): AudioTrack {
        val bytes = samples.toLittleEndianBytes()
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bytes.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(bytes, 0, bytes.size)
        return track
    }

    // --- Synthesis: one short waveform per cue/weapon, each with an attack/release envelope so it doesn't click. ---

    private fun synthesize(cue: SoundCue): ShortArray = when (cue) {
        SoundCue.ENEMY_HIT -> tone(frequencyHz = 180.0, durationMs = 45, waveform = Waveform.SQUARE, volume = 0.28f)
        SoundCue.ENEMY_DEATH -> sweep(startHz = 420.0, endHz = 120.0, durationMs = 160, waveform = Waveform.SAWTOOTH, volume = 0.28f)
        SoundCue.PLAYER_HURT -> tone(frequencyHz = 140.0, durationMs = 140, waveform = Waveform.SQUARE, volume = 0.32f)
        SoundCue.HEALTH_PICKUP -> sweep(startHz = 500.0, endHz = 800.0, durationMs = 150, waveform = Waveform.SINE, volume = 0.28f)
        SoundCue.LEVEL_UP -> arpeggio(frequenciesHz = listOf(523.25, 659.25, 783.99), noteDurationMs = 90, waveform = Waveform.SINE, volume = 0.28f)
        SoundCue.GAME_OVER -> sweep(startHz = 300.0, endHz = 80.0, durationMs = 500, waveform = Waveform.SINE, volume = 0.32f)
        SoundCue.VICTORY -> arpeggio(frequenciesHz = listOf(523.25, 659.25, 783.99, 1046.5), noteDurationMs = 130, waveform = Waveform.SINE, volume = 0.32f)
    }

    private fun tone(frequencyHz: Double, durationMs: Int, waveform: Waveform, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        return ShortArray(sampleCount) { i ->
            val phaseCycles = frequencyHz * i / sampleRateHz
            toPcm16(waveformSample(waveform, phaseCycles) * volume * envelopeAt(i, sampleCount))
        }
    }

    /** A tone that glides from [startHz] to [endHz] over its duration — used for a death "thud", the game-over dirge, and a quick blade "shing". */
    private fun sweep(startHz: Double, endHz: Double, durationMs: Int, waveform: Waveform, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        var phaseCycles = 0.0
        return ShortArray(sampleCount) { i ->
            val progress = i / sampleCount.toDouble()
            val instantaneousHz = startHz + (endHz - startHz) * progress
            phaseCycles += instantaneousHz / sampleRateHz
            toPcm16(waveformSample(waveform, phaseCycles) * volume * envelopeAt(i, sampleCount))
        }
    }

    /**
     * A stack of sine partials around [fundamentalHz], each ringing out at its own decay
     * rate — a resonant object (bell, shell, blade) rather than a flat beep. [inharmonic]
     * picks non-integer ratios for a metallic clang (Sword/Axe/Shield/Pan); harmonic
     * (integer) ratios instead give a warm, pleasant chime (Sunburst).
     */
    private fun metallic(fundamentalHz: Double, durationMs: Int, volume: Float, inharmonic: Boolean): ShortArray {
        val ratios = if (inharmonic) listOf(1.0, 1.62, 2.31, 3.08, 4.15) else listOf(1.0, 2.0, 3.0, 4.0)
        val sampleCount = millisToSampleCount(durationMs)
        val mixed = DoubleArray(sampleCount)
        ratios.forEachIndexed { index, ratio ->
            val partialAmp = 1.0 / (index + 1)
            val decayRate = 3.0 + index * (if (inharmonic) 1.6 else 0.7) // higher partials die out faster
            val partialFreq = fundamentalHz * ratio
            for (i in 0 until sampleCount) {
                val decay = exp(-i.toDouble() / sampleCount * decayRate)
                val phaseCycles = partialFreq * i / sampleRateHz
                mixed[i] += sin(2 * PI * phaseCycles) * partialAmp * decay
            }
        }
        return normalizeToPcm16(mixed, volume, attackFraction = 0.02f)
    }

    /**
     * A downward-swept buzzy carrier with fast amplitude tremolo — the closest a plain
     * oscillator gets to an actual duck quack (real quacks are a rapid glottal pulse train,
     * which tremolo-over-a-buzzy-wave approximates far better than a smooth pitch slide).
     */
    private fun quack(startHz: Double, endHz: Double, durationMs: Int, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        var phaseCycles = 0.0
        val tremoloHz = 140.0
        return ShortArray(sampleCount) { i ->
            val progress = i / sampleCount.toDouble()
            val instantaneousHz = startHz + (endHz - startHz) * progress
            phaseCycles += instantaneousHz / sampleRateHz
            val carrier = waveformSample(Waveform.SAWTOOTH, phaseCycles)
            val tremolo = 0.6 + 0.4 * sin(2 * PI * tremoloHz * i / sampleRateHz)
            toPcm16(carrier * tremolo * volume * envelopeAt(i, sampleCount))
        }
    }

    /** White noise smoothed by a 1-pole low-pass ([lowPassAlpha], lower = duller/deeper) — a pop, splat, or crackle instead of a harsh hiss. */
    private fun noiseBurst(durationMs: Int, volume: Float, lowPassAlpha: Double): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        val raw = DoubleArray(sampleCount) { noiseRandom.nextDouble(-1.0, 1.0) }
        val filtered = lowPass(raw, lowPassAlpha)
        return normalizeToPcm16(filtered, volume, attackFraction = 0.01f)
    }

    /** Heavily-filtered noise with a swell-then-fade envelope (rather than the usual attack/decay) — an air-movement whoosh. */
    private fun whoosh(durationMs: Int, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        val raw = DoubleArray(sampleCount) { noiseRandom.nextDouble(-1.0, 1.0) }
        val filtered = lowPass(raw, alpha = 0.08)
        return ShortArray(sampleCount) { i ->
            val progress = i / sampleCount.toDouble()
            val swell = sin(PI * progress) // 0 -> 1 -> 0, peaking mid-way
            toPcm16(filtered[i] * swell * volume)
        }
    }

    /** A tone with slow amplitude tremolo — a throbbing pulse rather than a flat hum (Magnet Pulse). */
    private fun pulse(frequencyHz: Double, durationMs: Int, waveform: Waveform, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        val tremoloHz = 18.0
        return ShortArray(sampleCount) { i ->
            val phaseCycles = frequencyHz * i / sampleRateHz
            val tremolo = 0.5 + 0.5 * sin(2 * PI * tremoloHz * i / sampleRateHz)
            toPcm16(waveformSample(waveform, phaseCycles) * tremolo * volume * envelopeAt(i, sampleCount))
        }
    }

    /** Concatenated notes — a tiny musical sting for level-up/victory rather than a single beep. */
    private fun arpeggio(frequenciesHz: List<Double>, noteDurationMs: Int, waveform: Waveform, volume: Float): ShortArray =
        frequenciesHz
            .map { frequencyHz -> tone(frequencyHz, noteDurationMs, waveform, volume) }
            .reduce { concatenated, note -> concatenated + note }

    /** Sums two buffers sample-by-sample (shorter one is zero-padded), for layering e.g. a metallic ring with an impact thwack. */
    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val length = maxOf(a.size, b.size)
        return ShortArray(length) { i ->
            val sum = (a.getOrElse(i) { 0 }).toInt() + (b.getOrElse(i) { 0 }).toInt()
            sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    /** A simple 1-pole low-pass filter: lower [alpha] = more smoothing = duller/deeper. */
    private fun lowPass(samples: DoubleArray, alpha: Double): DoubleArray {
        val output = DoubleArray(samples.size)
        var previous = 0.0
        for (i in samples.indices) {
            previous += alpha * (samples[i] - previous)
            output[i] = previous
        }
        return output
    }

    /** Normalizes a raw double buffer to peak at [volume], applying the usual click-free envelope. */
    private fun normalizeToPcm16(samples: DoubleArray, volume: Float, attackFraction: Float): ShortArray {
        val peak = samples.maxOf { abs(it) }.coerceAtLeast(0.0001)
        val sampleCount = samples.size
        return ShortArray(sampleCount) { i ->
            toPcm16(samples[i] / peak * volume * envelopeAt(i, sampleCount, attackFraction))
        }
    }

    private fun millisToSampleCount(durationMs: Int): Int = (sampleRateHz * durationMs / 1000.0).roundToInt().coerceAtLeast(1)

    /** Fade-in over the first [attackFraction] and fade-out over the last 30% of the buffer, to avoid audible clicks at the edges. */
    private fun envelopeAt(sampleIndex: Int, totalSamples: Int, attackFraction: Float = 0.1f): Float {
        val attackSamples = (totalSamples * attackFraction).toInt().coerceAtLeast(1)
        val releaseStart = totalSamples - (totalSamples * 0.3f).toInt().coerceAtLeast(1)
        return when {
            sampleIndex < attackSamples -> sampleIndex / attackSamples.toFloat()
            sampleIndex > releaseStart -> (totalSamples - sampleIndex) / (totalSamples - releaseStart).toFloat()
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    private fun waveformSample(waveform: Waveform, phaseCycles: Double): Double {
        val fractionalPhase = phaseCycles - floor(phaseCycles) // 0..1, wraps every cycle
        return when (waveform) {
            Waveform.SINE -> sin(2 * PI * fractionalPhase)
            Waveform.SQUARE -> if (fractionalPhase < 0.5) 1.0 else -1.0
            Waveform.SAWTOOTH -> 2.0 * fractionalPhase - 1.0
        }
    }

    private fun toPcm16(normalizedValue: Double): Short =
        (normalizedValue.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun ShortArray.toLittleEndianBytes(): ByteArray {
        val bytes = ByteArray(size * 2)
        for (i in indices) {
            val value = this[i].toInt()
            bytes[i * 2] = (value and 0xFF).toByte()
            bytes[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private companion object {
        const val WEAPON_FIRE_DURATION_MS = 70
        const val WEAPON_FIRE_VOLUME = 0.22f
    }
}
