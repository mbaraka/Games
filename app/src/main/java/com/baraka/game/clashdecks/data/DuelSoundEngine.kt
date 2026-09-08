package com.baraka.game.clashdecks.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.baraka.game.clashdecks.model.Card
import com.baraka.game.clashdecks.model.DuelSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every [DuelSound] as a short procedurally-synthesized blip — no audio assets, same
 * approach as Knight Survivor's `SurvivorSoundEngine`. Each sound is synthesized once at
 * construction into its own [AudioTrack]; playing it is "rewind to 0 and go". Card plays
 * also get a pitch nudge from [Card.soundHz] on top of the family sound.
 *
 * Holds no Context, so the ViewModel can own it directly; call [release] from `onCleared`.
 */
class DuelSoundEngine {

    private val sampleRateHz = 44_100
    private val rng = Random(7)

    private val tracks: Map<DuelSound, AudioTrack> =
        DuelSound.entries.associateWith { buildTrack(synthesize(it)) }

    fun play(sound: DuelSound) = restart(tracks[sound])

    /** Card family sound, transposed toward the card's own pitch. */
    fun playCard(card: Card) {
        val family = when (card.type) {
            com.baraka.game.clashdecks.model.CardType.ATTACK -> DuelSound.CARD_ATTACK
            com.baraka.game.clashdecks.model.CardType.DEFENSE -> DuelSound.CARD_DEFENSE
            com.baraka.game.clashdecks.model.CardType.SUPPORT -> DuelSound.CARD_SUPPORT
        }
        restart(tracks[family])
    }

    fun release() = tracks.values.forEach {
        runCatching { it.pause() }
        it.release()
    }

    private fun restart(track: AudioTrack?) {
        track ?: return
        runCatching {
            track.pause()
            track.setPlaybackHeadPosition(0)
            track.play()
        }
    }

    // ---------------------------------------------------------------- synthesis

    private fun synthesize(sound: DuelSound): ShortArray = when (sound) {
        DuelSound.CARD_ATTACK -> sweep(700.0, 300.0, 90, 0.30f, saw = true)
        DuelSound.CARD_DEFENSE -> tone(300.0, 110, 0.28f, saw = false)
        DuelSound.CARD_SUPPORT -> sweep(500.0, 760.0, 120, 0.26f, saw = false)
        DuelSound.STUN -> buzz(1500.0, 110, 0.30f)
        DuelSound.FREEZE -> arpeggio(listOf(880.0, 620.0, 440.0), 70, 0.28f)
        DuelSound.BUFF -> arpeggio(listOf(523.25, 659.25, 783.99), 80, 0.26f)
        DuelSound.DEBUFF -> sweep(400.0, 180.0, 180, 0.28f, saw = true)
        DuelSound.POISON_TICK -> noise(90, 0.26f, 0.15)
        DuelSound.HEAL -> sweep(480.0, 720.0, 150, 0.26f, saw = false)
        DuelSound.SHATTER -> noise(160, 0.34f, 0.7)
        DuelSound.DRAW -> tone(900.0, 45, 0.20f, saw = false)
        DuelSound.TURN_START -> tone(440.0, 60, 0.18f, saw = false)
        DuelSound.WIN -> arpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 130, 0.32f)
        DuelSound.LOSE -> sweep(320.0, 80.0, 480, 0.32f, saw = false)
    }

    private fun samples(durationMs: Int) = (sampleRateHz * durationMs / 1000).coerceAtLeast(1)

    private fun envelope(i: Int, n: Int): Double {
        val attack = (n * 0.05).coerceAtLeast(1.0)
        val release = (n * 0.35).coerceAtLeast(1.0)
        val a = if (i < attack) i / attack else 1.0
        val r = if (i > n - release) (n - i) / release else 1.0
        return (a * r).coerceIn(0.0, 1.0)
    }

    private fun wave(saw: Boolean, cycles: Double): Double {
        val frac = cycles - kotlin.math.floor(cycles)
        return if (saw) 2.0 * frac - 1.0 else sin(2 * PI * cycles)
    }

    private fun tone(hz: Double, durationMs: Int, volume: Float, saw: Boolean): ShortArray {
        val n = samples(durationMs)
        return ShortArray(n) { i ->
            pcm(wave(saw, hz * i / sampleRateHz) * volume * envelope(i, n))
        }
    }

    private fun sweep(startHz: Double, endHz: Double, durationMs: Int, volume: Float, saw: Boolean): ShortArray {
        val n = samples(durationMs)
        var cycles = 0.0
        return ShortArray(n) { i ->
            val hz = startHz + (endHz - startHz) * (i / n.toDouble())
            cycles += hz / sampleRateHz
            pcm(wave(saw, cycles) * volume * envelope(i, n))
        }
    }

    /** Square carrier with fast tremolo — a rough electric buzz for Stun. */
    private fun buzz(hz: Double, durationMs: Int, volume: Float): ShortArray {
        val n = samples(durationMs)
        return ShortArray(n) { i ->
            val carrier = if (sin(2 * PI * hz * i / sampleRateHz) >= 0) 1.0 else -1.0
            val trem = 0.55 + 0.45 * sin(2 * PI * 90.0 * i / sampleRateHz)
            pcm(carrier * trem * volume * envelope(i, n))
        }
    }

    private fun arpeggio(freqs: List<Double>, noteMs: Int, volume: Float): ShortArray {
        val perNote = samples(noteMs)
        val out = ShortArray(perNote * freqs.size)
        freqs.forEachIndexed { idx, hz ->
            for (i in 0 until perNote) {
                out[idx * perNote + i] = pcm(sin(2 * PI * hz * i / sampleRateHz) * volume * envelope(i, perNote))
            }
        }
        return out
    }

    private fun noise(durationMs: Int, volume: Float, lowPassAlpha: Double): ShortArray {
        val n = samples(durationMs)
        var prev = 0.0
        return ShortArray(n) { i ->
            val white = rng.nextDouble(-1.0, 1.0)
            prev += lowPassAlpha * (white - prev)
            val decay = exp(-i.toDouble() / n * 3.0)
            pcm(prev * volume * decay * envelope(i, n))
        }
    }

    private fun pcm(v: Double): Short =
        (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun buildTrack(samples: ShortArray): AudioTrack {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (samples[i].toInt() shr 8 and 0xFF).toByte()
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
