package com.baraka.game.arcshot.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import com.baraka.game.arcshot.model.ARC_LEVELS
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Every player-facing option of Arc Shot. Volume is 0..1. */
data class ArcSettings(
    val sfxVolume: Float = 0.8f,
    val vibration: Boolean = true,
    val screenShake: Boolean = true,
    val showFps: Boolean = false
)

/** Settings plus level progress, stored in the `arc_shot` SharedPreferences file. Missing keys fall back to defaults. */
class ArcRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("arc_shot", Context.MODE_PRIVATE)

    fun loadSettings(): ArcSettings {
        val d = ArcSettings()
        return ArcSettings(
            sfxVolume = prefs.getFloat("sfx", d.sfxVolume),
            vibration = prefs.getBoolean("vibration", d.vibration),
            screenShake = prefs.getBoolean("shake", d.screenShake),
            showFps = prefs.getBoolean("fps", d.showFps)
        )
    }

    fun saveSettings(s: ArcSettings) {
        prefs.edit()
            .putFloat("sfx", s.sfxVolume)
            .putBoolean("vibration", s.vibration)
            .putBoolean("shake", s.screenShake)
            .putBoolean("fps", s.showFps)
            .apply()
    }

    /** How many levels (from the first) the player may open. */
    fun unlockedCount(): Int = prefs.getInt("unlocked", 1).coerceIn(1, ARC_LEVELS.size)

    /** Best star rating (1..3) earned on [level]; 0 when never cleared. */
    fun bestStars(level: Int): Int = prefs.getInt("stars_$level", 0)

    /** Fewest shots this level has ever been cleared in; 0 when never cleared. */
    fun bestShots(level: Int): Int = prefs.getInt("shots_$level", 0)

    fun recordResult(level: Int, shots: Int, stars: Int) {
        val e = prefs.edit()
        if (stars > bestStars(level)) e.putInt("stars_$level", stars)
        val bestShots = bestShots(level)
        if (bestShots == 0 || shots < bestShots) e.putInt("shots_$level", shots)
        e.putInt("unlocked", max(unlockedCount(), min(ARC_LEVELS.size, level + 2)))
        e.apply()
    }

    fun unlockAll() {
        prefs.edit().putInt("unlocked", ARC_LEVELS.size).apply()
    }

    fun completeAll() {
        val e = prefs.edit().putInt("unlocked", ARC_LEVELS.size)
        for (i in ARC_LEVELS.indices) {
            if (bestStars(i) == 0) {
                e.putInt("stars_$i", 3)
                e.putInt("shots_$i", 1)
            }
        }
        e.apply()
    }

    fun resetProgress() {
        val e = prefs.edit().putInt("unlocked", 1)
        for (i in ARC_LEVELS.indices) {
            e.remove("stars_$i")
            e.remove("shots_$i")
        }
        e.apply()
    }
}

enum class ArcCue { AIM, LAUNCH, BOUNCE_OFF, WIN }

/** Procedurally synthesised sound effects (no asset files), one preloaded [AudioTrack] per [ArcCue]. */
class ArcAudio {
    private val rate = 44_100
    var volume = 0.8f
    private val tracks: Map<ArcCue, AudioTrack> = ArcCue.entries.associateWith { buildTrack(synth(it)) }

    fun play(cue: ArcCue) {
        if (volume <= 0.01f) return
        val t = tracks[cue] ?: return
        t.setVolume(volume)
        t.pause()
        t.setPlaybackHeadPosition(0)
        t.play()
    }

    fun release() = tracks.values.forEach { it.release() }

    private fun synth(cue: ArcCue): ShortArray = when (cue) {
        ArcCue.AIM -> sweep(500.0, 560.0, 25, 0.1f)
        ArcCue.LAUNCH -> sweep(220.0, 700.0, 140, 0.32f)
        ArcCue.BOUNCE_OFF -> sweep(300.0, 120.0, 110, 0.28f)
        ArcCue.WIN -> notes(listOf(523.25, 659.25, 783.99, 1046.5), 120, 0.32f)
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

/** Short vibration pulses for launches, misses and wins; silent when [enabled] is false. */
class ArcHaptics(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled = true

    fun cue(cue: ArcCue) {
        if (!enabled) return
        when (cue) {
            ArcCue.LAUNCH -> pulse(20, 100)
            ArcCue.BOUNCE_OFF -> pulse(35, 70)
            ArcCue.WIN -> pulse(80, 180)
            else -> {}
        }
    }

    private fun pulse(ms: Long, amplitude: Int) {
        val v = vibrator ?: return
        runCatching { v.vibrate(VibrationEffect.createOneShot(ms, amplitude)) }
    }
}
