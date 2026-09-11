package com.baraka.game.roadrush.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class RoadSoundCue {
    SWERVE,
    CRASH,
    DODGE,
    GAME_OVER
}

/**
 * Plays every [RoadSoundCue] as a short procedurally-synthesized effect — same "no audio
 * asset files, everything is generated code" approach as knightsurvivor's
 * SurvivorSoundEngine, just a much smaller cue set since this game only needs a swerve
 * blip, a crash thud, a little dodge chime, and a game-over sweep. Each sound is
 * synthesized once at construction and held in its own [AudioTrack], so playing it is just
 * "restart from position 0."
 */
class RoadRushSoundEngine {

    private val sampleRateHz = 44_100
    private val noiseRandom = Random(0) // fixed seed: synthesis happens once at construction, not per-play, so this doesn't need to vary
    private val cueTracks: Map<RoadSoundCue, AudioTrack> = RoadSoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    /** Restarts the given cue's track from the beginning — safe to call again before a previous play finished. */
    fun play(cue: RoadSoundCue) {
        val track = cueTracks[cue] ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    /** Releases every underlying [AudioTrack]. Call once when the game screen leaves composition. */
    fun release() {
        cueTracks.values.forEach { it.release() }
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

    private fun synthesize(cue: RoadSoundCue): ShortArray = when (cue) {
        // Quick upward blip — a light steering "swish", not a harsh beep.
        RoadSoundCue.SWERVE -> sweep(startHz = 480.0, endHz = 720.0, durationMs = 70, volume = 0.22f)
        // Low noise thud plus a falling tone — an impact, not a click.
        RoadSoundCue.CRASH -> mix(
            noiseBurst(durationMs = 140, volume = 0.35f, lowPassAlpha = 0.35),
            sweep(startHz = 220.0, endHz = 70.0, durationMs = 180, volume = 0.3f)
        )
        // Two quick ascending notes — a friendly "nice dodge" chime.
        RoadSoundCue.DODGE -> arpeggio(listOf(660.0, 990.0), noteDurationMs = 45, volume = 0.18f)
        // Slow downward sweep — the run winding down.
        RoadSoundCue.GAME_OVER -> sweep(startHz = 320.0, endHz = 90.0, durationMs = 500, volume = 0.3f)
    }

    private fun sweep(startHz: Double, endHz: Double, durationMs: Int, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        var phaseCycles = 0.0
        return ShortArray(sampleCount) { i ->
            val progress = i / sampleCount.toDouble()
            val instantaneousHz = startHz + (endHz - startHz) * progress
            phaseCycles += instantaneousHz / sampleRateHz
            toPcm16(sin(2 * PI * phaseCycles) * volume * envelopeAt(i, sampleCount))
        }
    }

    /** White noise smoothed by a 1-pole low-pass ([lowPassAlpha], lower = duller/deeper) — a thud instead of a harsh hiss. */
    private fun noiseBurst(durationMs: Int, volume: Float, lowPassAlpha: Double): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        val raw = DoubleArray(sampleCount) { noiseRandom.nextDouble(-1.0, 1.0) }
        val filtered = DoubleArray(sampleCount)
        var previous = 0.0
        for (i in raw.indices) {
            previous += lowPassAlpha * (raw[i] - previous)
            filtered[i] = previous
        }
        val peak = filtered.maxOf { kotlin.math.abs(it) }.coerceAtLeast(0.0001)
        return ShortArray(sampleCount) { i -> toPcm16(filtered[i] / peak * volume * envelopeAt(i, sampleCount, attackFraction = 0.01f)) }
    }

    /** Concatenated short notes — a tiny musical sting rather than a single beep. */
    private fun arpeggio(frequenciesHz: List<Double>, noteDurationMs: Int, volume: Float): ShortArray =
        frequenciesHz
            .map { frequencyHz -> tone(frequencyHz, noteDurationMs, volume) }
            .reduce { concatenated, note -> concatenated + note }

    private fun tone(frequencyHz: Double, durationMs: Int, volume: Float): ShortArray {
        val sampleCount = millisToSampleCount(durationMs)
        return ShortArray(sampleCount) { i ->
            val phaseCycles = frequencyHz * i / sampleRateHz
            toPcm16(sin(2 * PI * phaseCycles) * volume * envelopeAt(i, sampleCount))
        }
    }

    /** Sums two buffers sample-by-sample (shorter one is zero-padded), for layering the crash thud with its falling tone. */
    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val length = maxOf(a.size, b.size)
        return ShortArray(length) { i ->
            val sum = (a.getOrElse(i) { 0 }).toInt() + (b.getOrElse(i) { 0 }).toInt()
            sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
}
