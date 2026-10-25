package com.baraka.game.dreadnought.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import com.baraka.game.dreadnought.systems.DreadCue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Everything the player hears and feels: synthesized effects (one static [AudioTrack] per cue, no asset
 * files), a looping sea-and-engines ambience, and vibration. Effects and ambience have their own volume.
 * The sounds are built on a background thread, so [play] quietly does nothing until they are ready.
 */
class DreadAudio(context: Context) {
    private val rate = 22_050
    private val noise = Random(11)

    @Volatile private var tracks: Array<AudioTrack?> = arrayOfNulls(DreadCue.entries.size)
    @Volatile private var ambience: AudioTrack? = null
    @Volatile private var released = false
    private val lastPlayed = LongArray(DreadCue.entries.size)
    private var ambienceOn = false
    private var engine = 0f

    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private var lastVibe = 0L

    var sfxVolume = 0.9f
    var ambienceVolume = 0.5f
        set(value) { field = value.coerceIn(0f, 1f); applyAmbience() }
    var vibration = true

    init {
        Thread({ buildAll() }, "dreadnought-audio-synth").apply { isDaemon = true; start() }
    }

    fun play(cue: DreadCue, volume: Float) {
        val v = (volume * sfxVolume).coerceIn(0f, 1f)
        if (v < 0.02f) return
        val now = System.currentTimeMillis()
        if (now - lastPlayed[cue.ordinal] < cue.minGapMs) return
        val track = tracks[cue.ordinal] ?: return
        lastPlayed[cue.ordinal] = now
        runCatching {
            track.pause()
            track.setPlaybackHeadPosition(0)
            track.setVolume(v)
            track.play()
        }
        if (vibration) {
            when (cue) {
                DreadCue.HIT_PLAYER -> pulse(30, 150, 140)
                DreadCue.BOOM -> if (volume > 0.6f) pulse(70, 230, 260)
                DreadCue.GAME_OVER -> pulse(260, 255, 800)
                DreadCue.MAIN_GUN -> pulse(18, 90, 120)
                else -> {}
            }
        }
    }

    /** Engines and sea: louder the faster the ship is going. [throttle] is 0..1. */
    fun setEngine(throttle: Float) {
        engine = throttle.coerceIn(0f, 1f)
        applyAmbience()
    }

    fun startAmbience() {
        ambienceOn = true
        applyAmbience()
        runCatching { ambience?.play() }
    }

    fun pause() {
        runCatching { ambience?.pause() }
    }

    fun resume() {
        if (ambienceOn) runCatching { ambience?.play() }
    }

    fun release() {
        released = true
        ambienceOn = false
        runCatching { ambience?.release() }
        tracks.forEach { runCatching { it?.release() } }
    }

    private fun applyAmbience() {
        val a = ambience ?: return
        runCatching { a.setVolume(ambienceVolume * (0.3f + 0.7f * engine)) }
    }

    private fun pulse(durationMs: Long, amplitude: Int, minGapMs: Long) {
        val v = vibrator ?: return
        val now = System.currentTimeMillis()
        if (now - lastVibe < minGapMs) return
        lastVibe = now
        runCatching { v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude)) }
    }

    // --- synthesis ---------------------------------------------------------------------------------

    private fun buildAll() {
        val built = arrayOfNulls<AudioTrack>(DreadCue.entries.size)
        for (cue in DreadCue.entries) {
            if (released) return
            built[cue.ordinal] = build(synth(cue), loop = false)
        }
        tracks = built
        if (released) return
        val amb = build(seaLoop(), loop = true)
        ambience = amb
        applyAmbience()
        if (ambienceOn) runCatching { amb.play() }
    }

    private fun synth(cue: DreadCue): ShortArray = when (cue) {
        DreadCue.MAIN_GUN -> mix(burst(420, 0.55f, 0.1), sweep(130.0, 32.0, 420, 0.5f))
        DreadCue.SEC_GUN -> mix(burst(90, 0.25f, 0.35), sweep(520.0, 200.0, 90, 0.12f))
        DreadCue.SHELL_HIT -> mix(burst(240, 0.42f, 0.2), sweep(190.0, 60.0, 240, 0.28f))
        DreadCue.SPLASH -> burst(320, 0.3f, 0.06)
        DreadCue.BOOM -> mix(burst(950, 0.6f, 0.07), sweep(95.0, 24.0, 950, 0.5f))
        DreadCue.TORPEDO -> mix(sweep(320.0, 130.0, 520, 0.18f), burst(520, 0.12f, 0.1))
        DreadCue.HIT_PLAYER -> mix(burst(280, 0.5f, 0.14), sweep(115.0, 42.0, 280, 0.42f))
        DreadCue.REPAIR -> arpeggio(listOf(659.0, 784.0, 988.0), 90, 0.2f)
        DreadCue.WAVE_CLEAR -> arpeggio(listOf(523.0, 659.0, 784.0, 1047.0), 110, 0.22f)
        DreadCue.GAME_OVER -> sweep(300.0, 38.0, 1300, 0.32f)
        DreadCue.UI -> arpeggio(listOf(880.0), 40, 0.14f)
    }

    private fun n(ms: Int) = (rate * ms / 1000.0).roundToInt().coerceAtLeast(1)

    private fun env(i: Int, total: Int): Float {
        val attack = (total * 0.03f).toInt().coerceAtLeast(1)
        val rel = total - (total * 0.75f).toInt().coerceAtLeast(1)
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

    /** Three seconds of low rumble with slow swell, built to loop without a click. */
    private fun seaLoop(): ShortArray {
        val total = rate * 3
        var prev = 0.0
        var prev2 = 0.0
        val out = ShortArray(total)
        for (i in 0 until total) {
            prev += 0.035 * (noise.nextDouble(-1.0, 1.0) - prev)
            prev2 += 0.004 * (noise.nextDouble(-1.0, 1.0) - prev2)
            val swell = 0.65 + 0.35 * sin(2 * PI * i / total)
            out[i] = pcm((prev * 1.6 + prev2 * 9.0) * swell * 0.55)
        }
        // cross-fade the seam
        val fade = rate / 10
        for (i in 0 until fade) {
            val t = i / fade.toDouble()
            out[i] = (out[i] * t + out[total - fade + i] * (1 - t)).toInt().toShort()
        }
        return out.copyOf(total - fade)
    }

    private fun build(samples: ShortArray, loop: Boolean): AudioTrack {
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
        if (loop) track.setLoopPoints(0, samples.size, -1)
        return track
    }
}
