package com.baraka.game.memorymatch

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MemorySoundCue {
    FLIP,
    MATCH,
    MISMATCH,
    WIN
}

/**
 * Plays every [MemorySoundCue] as a short procedurally-synthesized effect — same
 * "no audio asset files, everything is generated code" approach as roadrush's
 * RoadRushSoundEngine. Each sound is synthesized once at construction and held in
 * its own [AudioTrack], so playing it is just "restart from position 0."
 */
class MemoryMatchSoundEngine {

    private val sampleRateHz = 44_100
    private val cueTracks: Map<MemorySoundCue, AudioTrack> = MemorySoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    /** Restarts the given cue's track from the beginning — safe to call again before a previous play finished. */
    fun play(cue: MemorySoundCue) {
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

    private fun synthesize(cue: MemorySoundCue): ShortArray = when (cue) {
        // Tiny high blip — a light card-flick tick.
        MemorySoundCue.FLIP -> tone(frequencyHz = 520.0, durationMs = 40, volume = 0.16f)
        // Two quick ascending notes — a satisfying "found it" chime.
        MemorySoundCue.MATCH -> arpeggio(listOf(660.0, 880.0, 1100.0), noteDurationMs = 55, volume = 0.2f)
        // Short descending buzz — a gentle "nope" without being harsh.
        MemorySoundCue.MISMATCH -> sweep(startHz = 260.0, endHz = 150.0, durationMs = 150, volume = 0.18f)
        // Rising fanfare arpeggio — the whole board is cleared.
        MemorySoundCue.WIN -> arpeggio(listOf(523.0, 659.0, 784.0, 1046.0), noteDurationMs = 110, volume = 0.24f)
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
