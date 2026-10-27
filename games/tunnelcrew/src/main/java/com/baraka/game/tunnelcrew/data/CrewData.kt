package com.baraka.game.tunnelcrew.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import com.baraka.game.tunnelcrew.model.LEVELS
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Every player-facing option of Tunnel Crew. Volume is 0..1. */
data class CrewSettings(
    val sfxVolume: Float = 0.8f,
    val vibration: Boolean = true,
    val showFps: Boolean = false
)

/** Settings plus level progress, stored in the `tunnel_crew` SharedPreferences file. Missing keys fall back to defaults. */
class CrewRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tunnel_crew", Context.MODE_PRIVATE)

    fun loadSettings(): CrewSettings {
        val d = CrewSettings()
        return CrewSettings(
            sfxVolume = prefs.getFloat("sfx", d.sfxVolume),
            vibration = prefs.getBoolean("vibration", d.vibration),
            showFps = prefs.getBoolean("fps", d.showFps)
        )
    }

    fun saveSettings(s: CrewSettings) {
        prefs.edit().putFloat("sfx", s.sfxVolume).putBoolean("vibration", s.vibration).putBoolean("fps", s.showFps).apply()
    }

    /** How many levels (from the first) the player may open. */
    fun unlockedCount(): Int = prefs.getInt("unlocked", 1).coerceIn(1, LEVELS.size)

    /** Most crew members saved in [level]; 0 when never completed. */
    fun best(level: Int): Int = prefs.getInt("best_$level", 0)

    /** Stores a finished level: keeps the best result and unlocks the next level when [saved] meets the target. */
    fun recordResult(level: Int, saved: Int) {
        val won = saved >= LEVELS[level].needed
        val e = prefs.edit()
        if (saved > best(level)) e.putInt("best_$level", saved)
        if (won) e.putInt("unlocked", max(unlockedCount(), min(LEVELS.size, level + 2)))
        e.apply()
    }

    fun unlockAll() {
        prefs.edit().putInt("unlocked", LEVELS.size).apply()
    }

    fun completeAll() {
        val e = prefs.edit().putInt("unlocked", LEVELS.size)
        for (i in LEVELS.indices) e.putInt("best_$i", LEVELS[i].total)
        e.apply()
    }

    fun resetProgress() {
        val e = prefs.edit().putInt("unlocked", 1)
        for (i in LEVELS.indices) e.remove("best_$i")
        e.apply()
    }
}

enum class Cue { ASSIGN, REFUSE, HATCH, SAVE, SPLAT, BOOM, CLINK, WIN, LOSE, FLAME, WARN }

/** Procedurally synthesised sound effects (no asset files), one preloaded [AudioTrack] per [Cue]. */
class CrewAudio {
    private val rate = 44_100
    private val noise = Random(7)
    var volume = 0.8f
    private val tracks: Map<Cue, AudioTrack> = Cue.entries.associateWith { buildTrack(synth(it)) }

    fun play(cue: Cue) {
        if (volume <= 0.01f) return
        val t = tracks[cue] ?: return
        t.setVolume(volume)
        t.pause()
        t.setPlaybackHeadPosition(0)
        t.play()
    }

    fun release() = tracks.values.forEach { it.release() }

    private fun synth(cue: Cue): ShortArray = when (cue) {
        Cue.ASSIGN -> sweep(700.0, 1100.0, 60, 0.3f)
        Cue.REFUSE -> sweep(220.0, 150.0, 90, 0.3f)
        Cue.HATCH -> sweep(420.0, 300.0, 40, 0.14f)
        Cue.SAVE -> notes(listOf(660.0, 990.0), 70, 0.3f)
        Cue.SPLAT -> mix(noiseBurst(90, 0.3f, 0.4), sweep(320.0, 90.0, 110, 0.25f))
        Cue.BOOM -> mix(noiseBurst(260, 0.42f, 0.25), sweep(200.0, 50.0, 300, 0.35f))
        Cue.CLINK -> sweep(1500.0, 1200.0, 45, 0.28f)
        Cue.WIN -> notes(listOf(523.25, 659.25, 783.99, 1046.5), 120, 0.35f)
        Cue.LOSE -> sweep(320.0, 70.0, 600, 0.35f)
        Cue.FLAME -> mix(noiseBurst(300, 0.4f, 0.5), sweep(180.0, 90.0, 300, 0.2f))
        Cue.WARN -> notes(listOf(1250.0, 1250.0), 45, 0.22f)
    }

    private fun sweep(startHz: Double, endHz: Double, ms: Int, vol: Float): ShortArray {
        val n = count(ms)
        var phase = 0.0
        return ShortArray(n) { i ->
            phase += (startHz + (endHz - startHz) * i / n) / rate
            pcm(sin(2 * PI * phase) * vol * envelope(i, n))
        }
    }

    private fun notes(hz: List<Double>, ms: Int, vol: Float): ShortArray =
        hz.map { f ->
            val n = count(ms)
            ShortArray(n) { i -> pcm(sin(2 * PI * f * i / rate) * vol * envelope(i, n)) }
        }.reduce { a, b -> a + b }

    private fun noiseBurst(ms: Int, vol: Float, alpha: Double): ShortArray {
        val n = count(ms)
        val raw = DoubleArray(n) { noise.nextDouble(-1.0, 1.0) }
        var prev = 0.0
        for (i in raw.indices) {
            prev += alpha * (raw[i] - prev)
            raw[i] = prev
        }
        val peak = raw.maxOf { abs(it) }.coerceAtLeast(0.0001)
        return ShortArray(n) { i -> pcm(raw[i] / peak * vol * envelope(i, n)) }
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray =
        ShortArray(max(a.size, b.size)) { i ->
            (a.getOrElse(i) { 0 }.toInt() + b.getOrElse(i) { 0 }.toInt())
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

    private fun count(ms: Int) = (rate * ms / 1000.0).roundToInt().coerceAtLeast(1)

    private fun envelope(i: Int, n: Int): Float {
        val attack = max(1, (n * 0.08f).toInt())
        val releaseStart = n - max(1, (n * 0.3f).toInt())
        return when {
            i < attack -> i / attack.toFloat()
            i > releaseStart -> (n - i) / (n - releaseStart).toFloat()
            else -> 1f
        }.coerceIn(0f, 1f)
    }

    private fun pcm(v: Double): Short = (v.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()

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
                    .setSampleRate(rate)
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

/** Short vibration pulses for splats, blasts and results; silent when [enabled] is false. */
class CrewHaptics(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled = true
    private var lastMs = 0L

    fun cue(cue: Cue) {
        if (!enabled) return
        when (cue) {
            Cue.SPLAT -> pulse(18, 110, 150)
            Cue.BOOM -> pulse(45, 220, 200)
            Cue.WIN -> pulse(70, 160, 500)
            Cue.LOSE -> pulse(160, 220, 500)
            else -> {}
        }
    }

    private fun pulse(ms: Long, amplitude: Int, minGapMs: Long) {
        val v = vibrator ?: return
        val now = System.currentTimeMillis()
        if (now - lastMs < minGapMs) return
        lastMs = now
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, amplitude)) }
    }
}
