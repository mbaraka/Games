package com.baraka.game.breakout.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class BreakoutSoundCue {
    WALL_BOUNCE, PADDLE_BOUNCE, BRICK_HIT, BRICK_BREAK, POWER_UP, BALL_LOST, LEVEL_CLEAR, GAME_OVER
}

/**
 * Plays every [BreakoutSoundCue] as a short procedurally-synthesized effect — same approach
 * as roadrush's RoadRushSoundEngine (no audio asset files, everything generated once at
 * construction into its own [AudioTrack]).
 */
class BreakoutSoundEngine {

    private val sampleRateHz = 44_100
    private val noiseRandom = Random(0)
    private val cueTracks: Map<BreakoutSoundCue, AudioTrack> =
        BreakoutSoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    fun play(cue: BreakoutSoundCue) {
        val track = cueTracks[cue] ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    fun release() = cueTracks.values.forEach { it.release() }

    private fun synthesize(cue: BreakoutSoundCue): ShortArray = when (cue) {
        // A bright, quick blip for bouncing off a wall.
        BreakoutSoundCue.WALL_BOUNCE -> tone(520.0, 45, 0.18f)
        // A slightly lower, punchier blip for the paddle — the satisfying "thwip".
        BreakoutSoundCue.PADDLE_BOUNCE -> sweep(340.0, 460.0, 55, 0.24f)
        // A short mid tone — chipped, not broken.
        BreakoutSoundCue.BRICK_HIT -> tone(700.0, 40, 0.2f)
        // Noise pop plus a quick upward sweep — a satisfying "pop" of a brick actually breaking.
        BreakoutSoundCue.BRICK_BREAK -> mix(
            noiseBurst(durationMs = 60, volume = 0.26f, lowPassAlpha = 0.5),
            sweep(500.0, 900.0, 60, 0.2f)
        )
        // Rising three-note chime — something good just dropped.
        BreakoutSoundCue.POWER_UP -> arpeggio(listOf(523.25, 659.25, 880.0), 55, 0.22f)
        // Low falling thud — a ball just fell past the paddle.
        BreakoutSoundCue.BALL_LOST -> sweep(300.0, 90.0, 220, 0.3f)
        // Bright ascending run — a level just cleared.
        BreakoutSoundCue.LEVEL_CLEAR -> arpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 100, 0.3f)
        // Slow downward sweep — the run winding down.
        BreakoutSoundCue.GAME_OVER -> sweep(320.0, 80.0, 500, 0.3f)
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
