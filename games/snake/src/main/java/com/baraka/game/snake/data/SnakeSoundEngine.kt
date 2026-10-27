package com.baraka.game.snake.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class SnakeSoundCue { EAT, GAME_OVER }

/**
 * Plays every [SnakeSoundCue] as a short procedurally-synthesized effect — same approach as
 * the other Atari-style games' sound engines (no audio asset files, everything generated
 * once at construction into its own [AudioTrack]). Snake only needs two cues, so this is the
 * smallest of the four.
 */
class SnakeSoundEngine {

    private val sampleRateHz = 44_100
    private val cueTracks: Map<SnakeSoundCue, AudioTrack> =
        SnakeSoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    fun play(cue: SnakeSoundCue) {
        val track = cueTracks[cue] ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    fun release() = cueTracks.values.forEach { it.release() }

    private fun synthesize(cue: SnakeSoundCue): ShortArray = when (cue) {
        // A bright two-note "nom" — eating a pellet.
        SnakeSoundCue.EAT -> arpeggio(listOf(660.0, 990.0), 40, 0.22f)
        // Slow downward sweep — the run winding down.
        SnakeSoundCue.GAME_OVER -> sweep(320.0, 70.0, 500, 0.3f)
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

    private fun arpeggio(frequenciesHz: List<Double>, noteDurationMs: Int, volume: Float): ShortArray =
        frequenciesHz.map { tone(it, noteDurationMs, volume) }.reduce { a, b -> a + b }

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
