package com.baraka.game.knightsurvivor.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.baraka.game.knightsurvivor.model.SoundCue
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Plays every [SoundCue] as a short procedurally-synthesized tone — no audio asset files
 * at all, the same "everything is generated code, not a binary asset" approach this game
 * already uses for its icons. Each cue's waveform is synthesized once at construction and
 * held in its own dedicated [AudioTrack], so playing it is just "restart from position 0."
 *
 * Lives in data/ alongside SurvivorStatsRepository.kt: both are the Android-dependent side
 * of this game, kept out of model/systems so those stay platform-free.
 */
class SurvivorSoundEngine {

    private val sampleRateHz = 44_100
    private val tracks: Map<SoundCue, AudioTrack> = SoundCue.entries.associateWith { cue -> buildTrack(synthesize(cue)) }

    /** Restarts the given cue's track from the beginning — safe to call again before a previous play finished. */
    fun play(cue: SoundCue) {
        val track = tracks[cue] ?: return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.play()
    }

    /** Releases every underlying [AudioTrack]. Call once when the game screen leaves composition. */
    fun release() {
        tracks.values.forEach { it.release() }
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

    // --- Synthesis: one short waveform per cue, each with an attack/release envelope so it doesn't click. ---

    private fun synthesize(cue: SoundCue): ShortArray = when (cue) {
        SoundCue.WEAPON_FIRE -> tone(frequencyHz = 740.0, durationMs = 70, waveform = Waveform.SQUARE, volume = 0.22f)
        SoundCue.ENEMY_HIT -> tone(frequencyHz = 180.0, durationMs = 45, waveform = Waveform.SQUARE, volume = 0.28f)
        SoundCue.ENEMY_DEATH -> sweep(startHz = 420.0, endHz = 120.0, durationMs = 160, waveform = Waveform.SAWTOOTH, volume = 0.28f)
        SoundCue.PLAYER_HURT -> tone(frequencyHz = 140.0, durationMs = 140, waveform = Waveform.SQUARE, volume = 0.32f)
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

    /** A tone that glides from [startHz] to [endHz] over its duration — used for a death "thud" and the game-over dirge. */
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

    /** Concatenated notes — a tiny musical sting for level-up/victory rather than a single beep. */
    private fun arpeggio(frequenciesHz: List<Double>, noteDurationMs: Int, waveform: Waveform, volume: Float): ShortArray =
        frequenciesHz
            .map { frequencyHz -> tone(frequencyHz, noteDurationMs, waveform, volume) }
            .reduce { concatenated, note -> concatenated + note }

    private fun millisToSampleCount(durationMs: Int): Int = (sampleRateHz * durationMs / 1000.0).roundToInt().coerceAtLeast(1)

    /** Linear fade-in over the first 10% and fade-out over the last 30% of the buffer, to avoid audible clicks at the edges. */
    private fun envelopeAt(sampleIndex: Int, totalSamples: Int): Float {
        val attackSamples = (totalSamples * 0.1f).toInt().coerceAtLeast(1)
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

    private enum class Waveform { SINE, SQUARE, SAWTOOTH }
}
