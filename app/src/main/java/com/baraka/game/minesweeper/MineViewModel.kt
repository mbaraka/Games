package com.baraka.game.minesweeper

import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baraka.game.coop.CoopSide
import com.baraka.game.minesweeper.coop.MINE_PROTOCOL
import com.baraka.game.minesweeper.coop.MineLink
import com.baraka.game.minesweeper.coop.MineMsg
import com.baraka.game.minesweeper.data.MineSettings
import com.baraka.game.minesweeper.data.MineSettingsRepository
import com.baraka.game.minesweeper.model.MineDifficulty
import com.baraka.game.minesweeper.model.MineMode
import com.baraka.game.minesweeper.model.MineSnap
import com.baraka.game.minesweeper.systems.MineGame
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Carries one two-phone game. The host owns a [MineGame] (the only place the rules run) and broadcasts a [MineSnap] after
 * every change, including the guest's taps which arrive as [MineMsg.Reveal]/[MineMsg.Flag]. The guest never runs the rules:
 * it sends its taps and draws whatever snapshot the host last sent, so the two phones cannot disagree.
 */
class MineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MineSettingsRepository.getInstance(application)
    @Suppress("DEPRECATION")
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    var settings by mutableStateOf(repository.load())
        private set

    // Chosen on the menu; only the host's choice matters, the guest receives the board in the first snapshot.
    var mode by mutableStateOf(MineMode.COOP)
    var difficulty by mutableStateOf(MineDifficulty.EASY)

    var snap by mutableStateOf<MineSnap?>(null)
        private set
    var xray by mutableStateOf(false)
        private set
    var connected by mutableStateOf(true)
        private set
    var versionMismatch by mutableStateOf(false)
        private set

    /** 0 on the host phone, 1 on the guest phone: which colour and score are "mine". */
    var me = 0
        private set
    val isHost: Boolean get() = me == 0

    private var link: MineLink? = null
    private var game: MineGame? = null
    private var pollJob: Job? = null
    private var lastBoom = 0

    fun updateSettings(s: MineSettings) {
        settings = s
        repository.save(s)
    }

    fun begin(newLink: MineLink, side: CoopSide) {
        end()
        link = newLink
        me = if (side == CoopSide.HOST) 0 else 1
        connected = true
        versionMismatch = false
        xray = false
        lastBoom = 0
        newLink.send(MineMsg.Hello(MINE_PROTOCOL))
        if (isHost) newGame()
        pollJob = viewModelScope.launch {
            while (true) {
                delay(100)
                pump()
            }
        }
    }

    /** Leaves the game and closes the link so the partner's phone notices. */
    fun end() {
        pollJob?.cancel()
        pollJob = null
        link?.close()
        link = null
        game = null
        snap = null
    }

    fun tap(index: Int, flag: Boolean) {
        if (snap?.status != 0) return
        if (isHost) {
            val g = game ?: return
            if (if (flag) g.flag(index, 0) else g.reveal(index, 0)) publish()
        } else {
            link?.send(if (flag) MineMsg.Flag(index) else MineMsg.Reveal(index))
        }
    }

    fun rematch() {
        if (isHost) newGame() else link?.send(MineMsg.RequestRematch)
    }

    // --- debug tools (host only: they act on the real board through the normal engine paths) ---
    fun debugRevealSafe(count: Int) { game?.revealSafe(count, 0); publish() }
    fun debugRestoreLives() { game?.restoreLives(); publish() }
    fun debugRestart() { if (isHost) newGame() }
    fun debugXray(on: Boolean) { xray = on; publish(sendToPartner = false) }

    private fun newGame() {
        game = MineGame(mode, difficulty)
        publish()
    }

    private fun publish(sendToPartner: Boolean = true) {
        val g = game ?: return
        val s = g.snapshot(xray)
        onSnap(s)
        if (sendToPartner) link?.send(MineMsg.Snap(g.snapshot(false)))
    }

    private fun onSnap(s: MineSnap) {
        if (s.boomSeq != lastBoom) {
            if (s.boomSeq > lastBoom) buzz()
            lastBoom = s.boomSeq
        }
        snap = s
    }

    private fun pump() {
        val l = link ?: return
        l.tick(0.1f)
        var m = l.poll()
        while (m != null) {
            handle(m)
            m = l.poll()
        }
        if (!l.connected) connected = false
    }

    private fun handle(m: MineMsg) {
        when (m) {
            is MineMsg.Hello -> {
                if (m.version != MINE_PROTOCOL) versionMismatch = true
                if (isHost) publish() // the guest may have connected after our first snapshot
            }
            is MineMsg.Snap -> if (!isHost) onSnap(m.snap)
            is MineMsg.Reveal -> if (isHost && game?.reveal(m.index, 1) == true) publish()
            is MineMsg.Flag -> if (isHost && game?.flag(m.index, 1) == true) publish()
            MineMsg.RequestRematch -> if (isHost && snap?.status != 0) newGame()
            MineMsg.Bye -> connected = false
        }
    }

    private fun buzz() {
        if (!settings.vibration) return
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(120, 200)) }
    }

    override fun onCleared() {
        end()
    }
}
