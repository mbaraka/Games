package com.baraka.game.trainsurvivor.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class MusicTrack { GARAGE, RUN, BOSS }

/**
 * Everything the player hears: sound effects ([sfx]), looping music and the train's ambience, each with
 * its own volume. Music and ambience are synthesized once on a background thread (no audio assets) and
 * played as looping static [AudioTrack]s; effects come from [TrainSoundEngine].
 */
class TrainAudio {
    val sfx = TrainSoundEngine()

    private val musicRate = 22_050
    private val ambienceRate = 22_050
    private val lock = Any()
    private val musicTracks = HashMap<MusicTrack, AudioTrack>()
    private var ambience: AudioTrack? = null

    private var wantedMusic: MusicTrack? = null
    private var playingMusic: MusicTrack? = null
    private var ambienceWanted = false
    private var ambiencePlaying = false
    private var paused = false
    @Volatile private var released = false

    var musicVolume = 0.6f
        set(value) { field = value.coerceIn(0f, 1f); applyVolumes() }
    var ambienceVolume = 0.5f
        set(value) { field = value.coerceIn(0f, 1f); applyVolumes() }
    var sfxVolume: Float
        get() = sfx.volume
        set(value) { sfx.volume = value.coerceIn(0f, 1f) }

    init {
        Thread({ buildAll() }, "train-audio-synth").apply { isDaemon = true; start() }
    }

    fun play(cue: TrainSoundCue) = sfx.play(cue)

    /** Selects the music loop (null = silence). Safe to call every frame. */
    fun setMusic(track: MusicTrack?) {
        synchronized(lock) {
            wantedMusic = track
            syncPlayback()
        }
    }

    fun setAmbience(on: Boolean, speedFactor: Float = 1f) {
        synchronized(lock) {
            ambienceWanted = on
            val track = ambience
            if (track != null && on) {
                runCatching { track.playbackRate = (ambienceRate * speedFactor.coerceIn(0.6f, 2.2f)).toInt() }
            }
            syncPlayback()
        }
    }

    /** Silences everything while the app is in the background; [resume] restores it. */
    fun pauseAll() {
        synchronized(lock) {
            paused = true
            syncPlayback()
        }
    }

    fun resume() {
        synchronized(lock) {
            paused = false
            syncPlayback()
        }
    }

    fun release() {
        released = true
        synchronized(lock) {
            musicTracks.values.forEach { runCatching { it.stop(); it.release() } }
            musicTracks.clear()
            runCatching { ambience?.stop(); ambience?.release() }
            ambience = null
        }
        sfx.release()
    }

    private fun applyVolumes() {
        synchronized(lock) {
            musicTracks.values.forEach { runCatching { it.setVolume(musicVolume) } }
            runCatching { ambience?.setVolume(ambienceVolume) }
            syncPlayback()
        }
    }

    private fun syncPlayback() {
        val targetMusic = if (paused || musicVolume <= 0.01f) null else wantedMusic
        if (targetMusic != playingMusic) {
            playingMusic?.let { old -> runCatching { musicTracks[old]?.pause() } }
            val next = targetMusic?.let { musicTracks[it] }
            if (next != null) runCatching {
                next.setVolume(musicVolume)
                next.play()
                playingMusic = targetMusic
            } else {
                playingMusic = null
            }
        }
        val track = ambience
        val wantAmbience = ambienceWanted && !paused && ambienceVolume > 0.01f
        if (track != null && wantAmbience != ambiencePlaying) {
            runCatching { if (wantAmbience) { track.setVolume(ambienceVolume); track.play() } else track.pause() }
            ambiencePlaying = wantAmbience
        }
    }

    // ------------------------------------------------------------------ synthesis (background thread)

    private fun buildAll() {
        val built = HashMap<MusicTrack, AudioTrack>()
        for (track in MusicTrack.entries) {
            if (released) return
            built[track] = loopTrack(synthMusic(track), musicRate)
        }
        val amb = loopTrack(synthAmbience(), ambienceRate)
        synchronized(lock) {
            if (released) {
                built.values.forEach { it.release() }
                amb.release()
                return
            }
            musicTracks.putAll(built)
            ambience = amb
            applyVolumes()
        }
    }

    private fun loopTrack(samples: ShortArray, rate: Int): AudioTrack {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            bytes[i * 2] = (samples[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((samples[i].toInt() shr 8) and 0xFF).toByte()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(bytes.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(bytes, 0, bytes.size)
        track.setLoopPoints(0, samples.size, -1)
        return track
    }

    private fun hz(semitonesFromA4: Int): Double = 440.0 * 2.0.pow(semitonesFromA4 / 12.0)

    /** A buffer of floats we mix instruments into, then normalize to PCM. */
    private class Mix(seconds: Double, val rate: Int) {
        val data = FloatArray((seconds * rate).toInt())

        fun add(startSec: Double, samples: FloatArray, gain: Float) {
            val start = (startSec * rate).toInt()
            for (i in samples.indices) {
                val j = start + i
                if (j >= data.size) break
                data[j] += samples[i] * gain
            }
        }

        fun toPcm(): ShortArray {
            var peak = 0.0001f
            for (v in data) peak = max(peak, abs(v))
            val scale = 0.85f / peak
            return ShortArray(data.size) { (data[it] * scale * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort() }
        }
    }

    private fun tone(rate: Int, freq: Double, seconds: Double, harmonics: FloatArray, attack: Double, decayPower: Double): FloatArray {
        val n = (seconds * rate).toInt()
        return FloatArray(n) { i ->
            val t = i.toDouble() / rate
            val env = min(1.0, t / attack) * (1.0 - i.toDouble() / n).pow(decayPower)
            var v = 0.0
            for (h in harmonics.indices) v += sin(2 * PI * freq * (h + 1) * t) * harmonics[h]
            (v * env).toFloat()
        }
    }

    private fun kick(rate: Int): FloatArray {
        val n = (0.2 * rate).toInt()
        var phase = 0.0
        return FloatArray(n) { i ->
            val p = i.toDouble() / n
            phase += (45.0 + 110.0 * (1 - p).pow(3)) / rate
            (sin(2 * PI * phase) * (1 - p).pow(1.5)).toFloat()
        }
    }

    private fun noiseHit(rate: Int, seconds: Double, bright: Double, rnd: Random): FloatArray {
        val n = (seconds * rate).toInt()
        var prev = 0.0
        return FloatArray(n) { i ->
            val raw = rnd.nextDouble(-1.0, 1.0)
            prev += bright * (raw - prev)
            ((raw - prev * (1 - bright)) * (1 - i.toDouble() / n).pow(2.0)).toFloat()
        }
    }

    private fun synthMusic(track: MusicTrack): ShortArray {
        val rnd = Random(track.ordinal + 3)
        return when (track) {
            MusicTrack.GARAGE -> synthGarage(rnd)
            MusicTrack.RUN -> synthDrive(bpm = 118.0, bars = 8, snare = false, rnd = rnd)
            MusicTrack.BOSS -> synthDrive(bpm = 148.0, bars = 8, snare = true, rnd = rnd)
        }
    }

    /** Calm: soft pad chords with a slow arpeggio. 80 bpm, 8 bars. */
    private fun synthGarage(rnd: Random): ShortArray {
        val rate = musicRate
        val beat = 60.0 / 80.0
        val total = beat * 4 * 8
        val mix = Mix(total, rate)
        // Am - F - C - G, two bars each chord pair.
        val chords = listOf(intArrayOf(-12, -9, -5), intArrayOf(-16, -12, -9), intArrayOf(-21, -17, -14), intArrayOf(-26, -22, -19))
        val pad = floatArrayOf(1f, 0.3f, 0.15f)
        for (bar in 0 until 8) {
            val chord = chords[(bar / 2) % chords.size]
            for (note in chord) mix.add(bar * beat * 4, tone(rate, hz(note + 12), beat * 4.1, pad, 0.6, 0.6), 0.22f)
            val arp = chord.map { it + 24 }
            for (step in 0 until 8) {
                val n = arp[(step * 2 + (bar % 2)) % arp.size] + if (step % 4 == 3) 12 else 0
                mix.add(bar * beat * 4 + step * beat / 2, tone(rate, hz(n), beat * 1.2, floatArrayOf(1f, 0.2f), 0.01, 2.2), 0.16f)
            }
        }
        return mix.toPcm()
    }

    /** Driving: kick, hats, bass and a pentatonic lead; the boss version adds a snare and runs faster. */
    private fun synthDrive(bpm: Double, bars: Int, snare: Boolean, rnd: Random): ShortArray {
        val rate = musicRate
        val beat = 60.0 / bpm
        val mix = Mix(beat * 4 * bars, rate)
        val kickSample = kick(rate)
        val hat = noiseHit(rate, 0.05, 0.95, rnd)
        val snareSample = noiseHit(rate, 0.14, 0.6, rnd)
        val bassHarmonics = floatArrayOf(1f, 0.55f, 0.3f)
        val bassRoots = intArrayOf(-24, -24, -21, -24, -22, -22, -26, -22)   // A A C A | B B G B (semitones from A4)
        val lead = intArrayOf(0, 3, 5, 7, 10, 7, 5, 3)
        for (bar in 0 until bars) {
            val barStart = bar * beat * 4
            for (b in 0 until 4) {
                val t = barStart + b * beat
                mix.add(t, kickSample, 0.9f)
                mix.add(t + beat / 2, hat, 0.22f)
                if (snare && (b == 1 || b == 3)) mix.add(t, snareSample, 0.4f)
                val root = bassRoots[(bar % 2) * 4 + b]
                mix.add(t, tone(rate, hz(root), beat * 0.45, bassHarmonics, 0.005, 1.4), 0.5f)
                mix.add(t + beat / 2, tone(rate, hz(root), beat * 0.4, bassHarmonics, 0.005, 1.4), 0.4f)
                if (bar >= 2) {
                    val note = lead[(b * 2 + bar) % lead.size] + if (bar % 4 >= 2) 12 else 0
                    mix.add(t, tone(rate, hz(note), beat * 0.9, floatArrayOf(1f, 0.25f, 0.1f), 0.01, 2.0), 0.18f)
                }
            }
        }
        return mix.toPcm()
    }

    /** One second of rolling train: four chuffs, a low rumble and a rail clack on every beat. */
    private fun synthAmbience(): ShortArray {
        val rate = ambienceRate
        val rnd = Random(99)
        val mix = Mix(1.0, rate)
        val chuff = noiseHit(rate, 0.14, 0.18, rnd)
        val clack = noiseHit(rate, 0.025, 0.9, rnd)
        for (k in 0 until 4) {
            mix.add(k * 0.25, chuff, if (k % 2 == 0) 0.9f else 0.65f)
            mix.add(k * 0.25 + 0.125, clack, 0.35f)
        }
        var prev = 0.0
        val rumble = FloatArray(rate) { i ->
            prev += 0.02 * (rnd.nextDouble(-1.0, 1.0) - prev)
            (prev * 3.0 * (0.8 + 0.2 * sin(2 * PI * 2 * i / rate))).toFloat()
        }
        mix.add(0.0, rumble, 0.5f)
        return mix.toPcm()
    }
}
