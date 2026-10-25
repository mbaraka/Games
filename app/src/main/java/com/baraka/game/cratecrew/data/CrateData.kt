package com.baraka.game.cratecrew.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import com.baraka.game.cratecrew.model.CRATE_LEVELS
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Every player-facing option of Crate Crew. Volume is 0..1. */
data class CrateSettings(
    val sfxVolume: Float = 0.8f,
    val vibration: Boolean = true,
    val showFps: Boolean = false
)

/** Settings plus level progress, stored in the `crate_crew` SharedPreferences file. Missing keys fall back to defaults. */
class CrateRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("crate_crew", Context.MODE_PRIVATE)

    fun loadSettings(): CrateSettings {
        val d = CrateSettings()
        return CrateSettings(
            sfxVolume = prefs.getFloat("sfx", d.sfxVolume),
            vibration = prefs.getBoolean("vibration", d.vibration),
            showFps = prefs.getBoolean("fps", d.showFps)
        )
    }

    fun saveSettings(s: CrateSettings) {
        prefs.edit().putFloat("sfx", s.sfxVolume).putBoolean("vibration", s.vibration).putBoolean("fps", s.showFps).apply()
    }

    /** How many levels (from the first) the player may open. */
    fun unlockedCount(): Int = prefs.getInt("unlocked", 1).coerceIn(1, CRATE_LEVELS.size)

    /** Fewest moves this level has ever been solved in; 0 when never completed. */
    fun bestMoves(level: Int): Int = prefs.getInt("best_$level", 0)

    /** Stores a finished level: keeps the fewest moves and unlocks the next level. */
    fun recordResult(level: Int, moves: Int) {
        val best = bestMoves(level)
        val e = prefs.edit()
        if (best == 0 || moves < best) e.putInt("best_$level", moves)
        e.putInt("unlocked", max(unlockedCount(), min(CRATE_LEVELS.size, level + 2)))
        e.apply()
    }

    fun unlockAll() {
        prefs.edit().putInt("unlocked", CRATE_LEVELS.size).apply()
    }

    fun completeAll() {
        val e = prefs.edit().putInt("unlocked", CRATE_LEVELS.size)
        for (i in CRATE_LEVELS.indices) if (bestMoves(i) == 0) e.putInt("best_$i", CRATE_LEVELS[i].par)
        e.apply()
    }

    fun resetProgress() {
        val e = prefs.edit().putInt("unlocked", 1)
        for (i in CRATE_LEVELS.indices) e.remove("best_$i")
        e.apply()
    }
}

enum class CrateCue { STEP, PUSH, BLOCKED, UNDO, WIN }

/** Procedurally synthesised sound effects (no asset files), one preloaded [AudioTrack] per [CrateCue]. */
class CrateAudio {
    private val rate = 44_100
    var volume = 0.8f
    private val tracks: Map<CrateCue, AudioTrack> = CrateCue.entries.associateWith { buildTrack(synth(it)) }

    fun play(cue: CrateCue) {
        if (volume <= 0.01f) return
        val t = tracks[cue] ?: return
        t.setVolume(volume)
        t.pause()
        t.setPlaybackHeadPosition(0)
        t.play()
    }

    fun release() = tracks.values.forEach { it.release() }

    private fun synth(cue: CrateCue): ShortArray = when (cue) {
        CrateCue.STEP -> sweep(300.0, 260.0, 30, 0.12f)
        CrateCue.PUSH -> sweep(220.0, 160.0, 90, 0.3f)
        CrateCue.BLOCKED -> sweep(140.0, 110.0, 60, 0.25f)
        CrateCue.UNDO -> sweep(500.0, 650.0, 60, 0.2f)
        CrateCue.WIN -> notes(listOf(523.25, 659.25, 783.99, 1046.5), 120, 0.3f)
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

    private fun count(ms: Int) = (rate * ms / 1000.0).roundToInt().coerceAtLeast(1)

    private fun envelope(i: Int, n: Int): Float {
        val attack = max(1, (n * 0.1f).toInt())
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

/** Short vibration pulses for pushes, blocked moves and the win fanfare; silent when [enabled] is false. */
class CrateHaptics(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled = true

    fun cue(cue: CrateCue) {
        if (!enabled) return
        when (cue) {
            CrateCue.PUSH -> pulse(16, 90)
            CrateCue.BLOCKED -> pulse(30, 60)
            CrateCue.WIN -> pulse(80, 180)
            else -> {}
        }
    }

    private fun pulse(ms: Long, amplitude: Int) {
        val v = vibrator ?: return
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, amplitude)) }
    }
}
