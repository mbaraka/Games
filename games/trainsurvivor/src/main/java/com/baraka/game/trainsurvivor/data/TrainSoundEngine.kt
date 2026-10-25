package com.baraka.game.trainsurvivor.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class TrainSoundCue(val minGapMs: Long) {
    PISTOL(70), SMG(60), SHOTGUN(120), RIFLE(120), CANNON(150),
    ZOMBIE_DIE(60), COIN(80), TRAIN_HIT(120), DYNAMITE(200), BOSS(500), UPGRADE(80), GAME_OVER(500),
    FLAK(120), ZAP(150), HONK(200), ROCKET(150), LASER(70),
    MORTAR(200), OVERDRIVE(400), REPAIR(400), HORDE(900), CRATE(300)
}

/** Procedurally synthesized effects, one static [AudioTrack] per cue (no asset files), like the other games' engines. */
class TrainSoundEngine {
    private val rate = 44_100
    private val noise = Random(7)
    private val tracks: Map<TrainSoundCue, AudioTrack> = TrainSoundCue.entries.associateWith { build(synth(it)) }
    private val lastPlayed = HashMap<TrainSoundCue, Long>()

    /** Effects volume 0..1, applied to every cue as it plays. */
    var volume = 1f

    fun play(cue: TrainSoundCue) {
        val now = System.currentTimeMillis()
        if (now - (lastPlayed[cue] ?: 0L) < cue.minGapMs) return
        lastPlayed[cue] = now
        val track = tracks[cue] ?: return
        if (volume <= 0.01f) return
        track.pause()
        track.setPlaybackHeadPosition(0)
        track.setVolume(volume)
        track.play()
    }

    fun release() = tracks.values.forEach { it.release() }

    private fun synth(cue: TrainSoundCue): ShortArray = when (cue) {
        TrainSoundCue.PISTOL -> mix(burst(60, 0.22f, 0.5), sweep(700.0, 250.0, 60, 0.12f))
        TrainSoundCue.SMG -> mix(burst(40, 0.16f, 0.55), sweep(900.0, 400.0, 40, 0.08f))
        TrainSoundCue.SHOTGUN -> mix(burst(150, 0.34f, 0.3), sweep(300.0, 90.0, 150, 0.2f))
        TrainSoundCue.RIFLE -> mix(burst(110, 0.3f, 0.4), sweep(1200.0, 200.0, 110, 0.2f))
        TrainSoundCue.CANNON -> mix(burst(320, 0.4f, 0.18), sweep(160.0, 40.0, 330, 0.35f))
        TrainSoundCue.ZOMBIE_DIE -> sweep(260.0, 90.0, 130, 0.14f)
        TrainSoundCue.COIN -> arpeggio(listOf(988.0, 1319.0), 55, 0.16f)
        TrainSoundCue.TRAIN_HIT -> mix(burst(90, 0.28f, 0.22), sweep(140.0, 70.0, 90, 0.2f))
        TrainSoundCue.DYNAMITE -> mix(burst(550, 0.5f, 0.12), sweep(120.0, 30.0, 550, 0.45f))
        TrainSoundCue.BOSS -> sweep(110.0, 55.0, 900, 0.4f)
        TrainSoundCue.UPGRADE -> arpeggio(listOf(523.0, 659.0, 784.0, 1047.0), 70, 0.22f)
        TrainSoundCue.GAME_OVER -> sweep(320.0, 50.0, 800, 0.3f)
        TrainSoundCue.FLAK -> mix(burst(120, 0.3f, 0.3), sweep(420.0, 140.0, 120, 0.2f))
        TrainSoundCue.ZAP -> mix(burst(180, 0.3f, 0.9), sweep(1800.0, 300.0, 180, 0.15f))
        TrainSoundCue.HONK -> mix(sweep(380.0, 380.0, 110, 0.25f), sweep(300.0, 300.0, 110, 0.2f))
        TrainSoundCue.ROCKET -> mix(burst(260, 0.35f, 0.15), sweep(500.0, 90.0, 260, 0.25f))
        TrainSoundCue.LASER -> sweep(1700.0, 500.0, 90, 0.18f)
        TrainSoundCue.MORTAR -> mix(burst(200, 0.3f, 0.2), sweep(260.0, 70.0, 200, 0.25f))
        TrainSoundCue.OVERDRIVE -> arpeggio(listOf(392.0, 523.0, 659.0, 784.0, 988.0), 45, 0.2f)
        TrainSoundCue.REPAIR -> arpeggio(listOf(659.0, 784.0, 988.0), 90, 0.2f)
        TrainSoundCue.HORDE -> mix(sweep(90.0, 60.0, 700, 0.35f), sweep(135.0, 90.0, 700, 0.2f))
        TrainSoundCue.CRATE -> arpeggio(listOf(784.0, 988.0, 1319.0), 60, 0.18f)
    }

    private fun n(ms: Int) = (rate * ms / 1000.0).roundToInt().coerceAtLeast(1)

    private fun env(i: Int, total: Int): Float {
        val attack = (total * 0.05f).toInt().coerceAtLeast(1)
        val rel = total - (total * 0.6f).toInt().coerceAtLeast(1)
        return when {
            i < attack -> i / attack.toFloat()
            i > rel -> (total - i) / (total - rel).toFloat()
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    private fun pcm(v: Double): Short = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

    private fun sweep(from: Double, to: Double, ms: Int, vol: Float): ShortArray {
        val total = n(ms)
        var phase = 0.0
        return ShortArray(total) { i ->
            phase += (from + (to - from) * i / total) / rate
            pcm(sin(2 * PI * phase) * vol * env(i, total))
        }
    }

    private fun burst(ms: Int, vol: Float, alpha: Double): ShortArray {
        val total = n(ms)
        val raw = DoubleArray(total) { noise.nextDouble(-1.0, 1.0) }
        var prev = 0.0
        val filtered = DoubleArray(total) { i -> prev += alpha * (raw[i] - prev); prev }
        val peak = filtered.maxOf { abs(it) }.coerceAtLeast(0.0001)
        return ShortArray(total) { i -> pcm(filtered[i] / peak * vol * env(i, total)) }
    }

    private fun arpeggio(freqs: List<Double>, noteMs: Int, vol: Float): ShortArray =
        freqs.map { f ->
            val t = n(noteMs)
            ShortArray(t) { i -> pcm(sin(2 * PI * f * i / rate) * vol * env(i, t)) }
        }.reduce { a, b -> a + b }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray = ShortArray(maxOf(a.size, b.size)) { i ->
        (a.getOrElse(i) { 0 }.toInt() + b.getOrElse(i) { 0 }.toInt())
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    private fun build(samples: ShortArray): AudioTrack {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((samples[i].toInt() shr 8) and 0xFF).toByte()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(bytes.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(bytes, 0, bytes.size)
        return track
    }
}
