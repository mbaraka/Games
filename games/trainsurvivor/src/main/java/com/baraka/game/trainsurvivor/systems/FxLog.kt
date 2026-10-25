package com.baraka.game.trainsurvivor.systems

/**
 * A log of visual and audio effects, used only while a shared world is being streamed to a partner's phone.
 * The host simulation records every effect it makes (tracers, explosions, sparks, bursts, floating texts, sounds);
 * the snapshot sends the log; the guest replays it on its mirror, so both phones see the same fight without sending
 * every particle. Parallel primitive arrays, no allocation while recording, capped.
 */
class FxLog(val capacity: Int = 400) {
    var recording = false
    var count = 0
    val type = IntArray(capacity)
    val lane = IntArray(capacity)
    val x = FloatArray(capacity)
    val y = FloatArray(capacity)
    val x2 = FloatArray(capacity)
    val y2 = FloatArray(capacity)
    val w = FloatArray(capacity)
    val color = IntArray(capacity)
    val n = IntArray(capacity)
    val text = arrayOfNulls<String>(capacity)

    fun add(kind: Int, laneId: Int, px: Float, py: Float, qx: Float = 0f, qy: Float = 0f, width: Float = 0f, rgb: Int = 0, count0: Int = 0, label: String? = null) {
        if (!recording || count >= capacity) return
        val i = count++
        type[i] = kind
        lane[i] = laneId
        x[i] = px
        y[i] = py
        x2[i] = qx
        y2[i] = qy
        w[i] = width
        color[i] = rgb
        n[i] = count0
        text[i] = label
    }

    fun clear() {
        count = 0
    }

    companion object {
        const val TRACER = 0
        const val EXPLOSION = 1
        const val SPARK = 2
        const val GREEN_BURST = 3
        const val CONFETTI = 4
        const val TEXT = 5
        const val SOUND = 6
        const val COINS = 7
    }
}

/** What a guest's buttons call instead of acting directly: the host performs the action on the guest's train. */
interface RemoteControl {
    fun act(code: Int, arg: Int = 0, x: Float = 0f, y: Float = 0f)
}

/** The things a guest can ask the host to do on its train. */
object GuestAction {
    const val BLAST = 1
    const val BOOST = 2
    const val FIELD_REPAIR = 3
    const val CYCLE_WEAPON = 4
    const val GIVE_COINS = 5
    const val SEND_REPAIR = 6
    const val SEND_TOW = 7
    const val HELP_PARTNER = 8
    const val SEND_PASSENGER = 9
    const val CHOOSE_ROUTE = 10
    const val TAP = 11
}
