package com.baraka.game.spaceinvaders.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class SpaceInvadersSoundCue {
    PLAYER_SHOT, ALIEN_KILLED, SHIELD_HIT, PLAYER_HIT, MARCH_STEP, WAVE_CLEAR, GAME_OVER
}

/**
 * Plays every [SpaceInvadersSoundCue] as a short procedurally-synthesized effect — same
 * approach as breakout's BreakoutSoundEngine (no audio asset files, everything generated
 * once at construction into its own [AudioTrack]).
 */
class SpaceInvadersSoundEngine {

    private val sampleRateHz = 44_100
    private val noiseRandom = Random(0)
    private val cueTracks: Map<SpaceInvadersSoundCue, AudioTrack> =
        SpaceInvadersSoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    fun play(cue: SpaceInvadersSoundCue) {
        val track = cueTracks[cue] ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    fun release() = cueTracks.values.forEach { it.release() }

    private fun synthesize(cue: SpaceInvadersSoundCue): ShortArray = when (cue) {
        // A quick laser-ish upward chirp.
        SpaceInvadersSoundCue.PLAYER_SHOT -> sweep(900.0, 1500.0, 40, 0.16f)
        // A short noise pop — one alien down.
        SpaceInvadersSoundCue.ALIEN_KILLED -> mix(
            noiseBurst(durationMs = 70, volume = 0.24f, lowPassAlpha = 0.45),
            sweep(600.0, 200.0, 70, 0.18f)
        )
        // A dull thud — the bunker just absorbed a hit.
        SpaceInvadersSoundCue.SHIELD_HIT -> noiseBurst(durationMs = 55, volume = 0.22f, lowPassAlpha = 0.2)
        // Low harsh buzz — the player got hit.
        SpaceInvadersSoundCue.PLAYER_HIT -> mix(
            tone(110.0, 160, 0.3f),
            noiseBurst(durationMs = 160, volume = 0.2f, lowPassAlpha = 0.3)
        )
        // A very quiet low blip for each formation step — four alternating pitches like the classic march.
        SpaceInvadersSoundCue.MARCH_STEP -> tone(90.0, 60, 0.14f)
        // Bright ascending run — a wave just cleared.
        SpaceInvadersSoundCue.WAVE_CLEAR -> arpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 100, 0.3f)
        // Slow downward sweep — the run winding down.
        SpaceInvadersSoundCue.GAME_OVER -> sweep(300.0, 70.0, 550, 0.3f)
    }

    private fun tone(frequencyHz: Double, durationMs: Int, volume: Float): ShortArray {
        val n = millisToSampleCount(durationMs)
        return ShortArray(n) { i -> toPcm16(sin(2 * PI * frequencyHz * i / sampleRateHz) * volume * envelopeAt(i, n)) }
    }

    private fun sweep(startHz: Double, endHz: Double, durationMs: Int, volume: Float): ShortArray {
        val n = millisToSampleCount(durationMs)
        var phaseCycles = 0.0
        return ShortArray(n) { i ->
            val progress = i / n.toDouble()
            val hz = startHz + (endHz - startHz) * progress
            phaseCycles += hz / sampleRateHz
            toPcm16(sin(2 * PI * phaseCycles) * volume * envelopeAt(i, n))
        }
    }

    private fun noiseBurst(durationMs: Int, volume: Float, lowPassAlpha: Double): ShortArray {
        val n = millisToSampleCount(durationMs)
        val raw = DoubleArray(n) { noiseRandom.nextDouble(-1.0, 1.0) }
        val filtered = DoubleArray(n)
        var previous = 0.0
        for (i in raw.indices) {
            previous += lowPassAlpha * (raw[i] - previous)
            filtered[i] = previous
        }
        val peak = filtered.maxOf { abs(it) }.coerceAtLeast(0.0001)
        return ShortArray(n) { i -> toPcm16(filtered[i] / peak * volume * envelopeAt(i, n, attackFraction = 0.01f)) }
    }

    private fun arpeggio(frequenciesHz: List<Double>, noteDurationMs: Int, volume: Float): ShortArray =
        frequenciesHz.map { tone(it, noteDurationMs, volume) }.reduce { a, b -> a + b }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val length = maxOf(a.size, b.size)
        return ShortArray(length) { i ->
            val sum = a.getOrElse(i) { 0 }.toInt() + b.getOrElse(i) { 0 }.toInt()
            sum.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun millisToSampleCount(durationMs: Int): Int = (sampleRateHz * durationMs / 1000.0).roundToInt().coerceAtLeast(1)

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

    private fun buildTrack(samples: ShortArray): AudioTrack {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
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
}
