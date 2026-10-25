package com.baraka.game.trainsurvivor.data

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/** Short vibration pulses for the big moments (hits, blasts, bosses). Does nothing when [enabled] is false or the device can't vibrate. */
class Haptics(context: Context) {
    @Suppress("DEPRECATION")
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled = true
    private var lastMs = 0L

    fun cue(cue: TrainSoundCue) {
        if (!enabled) return
        when (cue) {
            TrainSoundCue.TRAIN_HIT -> pulse(14, 70, 120)
            TrainSoundCue.DYNAMITE -> pulse(70, 255, 200)
            TrainSoundCue.BOSS -> pulse(150, 200, 800)
            TrainSoundCue.GAME_OVER -> pulse(280, 255, 800)
            TrainSoundCue.HORDE -> pulse(120, 160, 800)
            TrainSoundCue.OVERDRIVE, TrainSoundCue.REPAIR -> pulse(40, 150, 300)
            else -> {}
        }
    }

    private fun pulse(durationMs: Long, amplitude: Int, minGapMs: Long) {
        val v = vibrator ?: return
        val now = System.currentTimeMillis()
        if (now - lastMs < minGapMs) return
        lastMs = now
        runCatching { v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude)) }
    }
}
