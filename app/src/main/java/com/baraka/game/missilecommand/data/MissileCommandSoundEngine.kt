package com.baraka.game.missilecommand.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class MissileCommandSoundCue {
    LAUNCH, DETONATE, ENEMY_DESTROYED, CITY_LOST, SILO_LOST, WAVE_CLEAR, GAME_OVER
}

/**
 * Plays every [MissileCommandSoundCue] as a short procedurally-synthesized effect — same
 * approach as breakout's/spaceinvaders' sound engines (no audio asset files, everything
 * generated once at construction into its own [AudioTrack]).
 */
class MissileCommandSoundEngine {

    private val sampleRateHz = 44_100
    private val noiseRandom = Random(0)
    private val cueTracks: Map<MissileCommandSoundCue, AudioTrack> =
        MissileCommandSoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    fun play(cue: MissileCommandSoundCue) {
        val track = cueTracks[cue] ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    fun release() = cueTracks.values.forEach { it.release() }

    private fun synthesize(cue: MissileCommandSoundCue): ShortArray = when (cue) {
        // A soft upward whoosh — an interceptor leaving the silo.
        MissileCommandSoundCue.LAUNCH -> sweep(260.0, 420.0, 110, 0.18f)
        // A deep noise thump plus a falling tone — a blast, not a click.
        MissileCommandSoundCue.DETONATE -> mix(
            noiseBurst(durationMs = 180, volume = 0.3f, lowPassAlpha = 0.3),
            sweep(260.0, 70.0, 220, 0.26f)
        )
        // A bright quick pop — an enemy missile just died.
        MissileCommandSoundCue.ENEMY_DESTROYED -> sweep(700.0, 1100.0, 50, 0.2f)
        // A low rumble — a city just fell.
        MissileCommandSoundCue.CITY_LOST -> mix(
            noiseBurst(durationMs = 260, volume = 0.3f, lowPassAlpha = 0.18),
            sweep(180.0, 50.0, 320, 0.26f)
        )
        // Similar but shorter/sharper — a silo falling is a smaller loss than a city.
        MissileCommandSoundCue.SILO_LOST -> mix(
            noiseBurst(durationMs = 160, volume = 0.26f, lowPassAlpha = 0.25),
            sweep(220.0, 80.0, 180, 0.22f)
        )
        // Bright ascending run — a wave just cleared.
        MissileCommandSoundCue.WAVE_CLEAR -> arpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 100, 0.3f)
        // Slow downward sweep — the run winding down.
        MissileCommandSoundCue.GAME_OVER -> sweep(300.0, 60.0, 600, 0.32f)
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
