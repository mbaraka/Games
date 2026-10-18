package com.baraka.game.clashdecks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.baraka.game.clashdecks.coop.CLASH_COOP
import com.baraka.game.clashdecks.coop.ClashCoopMsg
import com.baraka.game.clashdecks.coop.SimulatedClashLink
import com.baraka.game.coop.CoopLaunch
import com.baraka.game.coop.CoopLobbyScreen
import com.baraka.game.clashdecks.ui.ClashCoopNameEntryScreen
import com.baraka.game.clashdecks.ui.ClashCoopScreen
import com.baraka.game.clashdecks.ui.ClashDecksScreen
import com.baraka.game.ui.debug.DebugItem
import com.baraka.game.ui.debug.DebugSection
import com.baraka.game.ui.theme.GameTheme

class ClashDecksActivity : ComponentActivity() {

    private val viewModel: ClashDecksViewModel by viewModels()
    private val coopViewModel: ClashCoopViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                ClashDecksRoot(viewModel, coopViewModel, onBack = { finish() })
            }
        }
    }
}

/**
 * Four mutually-exclusive screens live here: the normal (single-player) flow, the co-op name
 * entry step, the Bluetooth lobby, and an active two-phone duel. [coopLaunch]/[coopName] are
 * the only local navigation state — everything about the duel itself still lives in whichever
 * ViewModel is driving it.
 */
@Composable
private fun ClashDecksRoot(
    viewModel: ClashDecksViewModel,
    coopViewModel: ClashCoopViewModel,
    onBack: () -> Unit
) {
    var showNameEntry by remember { mutableStateOf(false) }
    var showCoopLobby by remember { mutableStateOf(false) }
    var coopLaunch by remember { mutableStateOf<CoopLaunch<ClashCoopMsg>?>(null) }
    var coopName by remember { mutableStateOf("") }

    val launch = coopLaunch
    if (launch != null) {
        LaunchedEffect(launch) { coopViewModel.begin(launch.link, launch.side, coopName) }
        val coopModel by coopViewModel.ui.collectAsState()
        ClashCoopScreen(
            model = coopModel,
            opponentLabel = coopModel.duel?.cpu?.name?.takeIf { it.isNotBlank() } ?: "Opponent",
            onPlayCard = coopViewModel::playCard,
            onEndTurn = coopViewModel::endTurn,
            onOpenCodex = coopViewModel::openCodex,
            onCloseCodex = coopViewModel::closeCodex,
            onOpenSettings = coopViewModel::openSettings,
            onCloseSettings = coopViewModel::closeSettings,
            onSetSoundEnabled = coopViewModel::setSoundEnabled,
            onRematch = coopViewModel::rematch,
            onLeave = {
                coopViewModel.disconnect()
                coopLaunch = null
            }
        )
        return
    }

    if (showNameEntry) {
        ClashCoopNameEntryScreen(
            initialName = coopViewModel.playerName(),
            onContinue = { name ->
                coopName = name
                coopViewModel.setPlayerName(name)
                showNameEntry = false
                showCoopLobby = true
            },
            onClose = { showNameEntry = false }
        )
        return
    }

    if (showCoopLobby) {
        CoopLobbyScreen(
            spec = CLASH_COOP,
            heading = "PLAY VS FRIEND",
            intro = "Whoever hosts deals the cards and runs the rules; the other phone just plays its hand over the connection. Difficulty doesn't apply - you're dueling a real person.",
            practiceNote = "A simulated partner plays its hand using the normal CPU logic, so you can try hosting alone.",
            simulate = { SimulatedClashLink() },
            onReady = { ready ->
                coopLaunch = ready
                showCoopLobby = false
            },
            onClose = { showCoopLobby = false }
        )
        return
    }

    val model by viewModel.ui.collectAsState()
    ClashDecksScreen(
        model = model,
        onBack = onBack,
        onSetDifficulty = viewModel::setDifficulty,
        onStartDuel = viewModel::startDuel,
        onPlayCard = viewModel::playCard,
        onEndTurn = viewModel::endTurn,
        onOpenCodex = viewModel::openCodex,
        onCloseCodex = viewModel::closeCodex,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onSetSoundEnabled = viewModel::setSoundEnabled,
        onRematch = viewModel::rematch,
        onBackToMenu = viewModel::backToMenu,
        onPlayVsFriend = { showNameEntry = true },
        debugSections = clashDebugSections(viewModel)
    )
}

/** Single-player-only cheats (see systems/DuelDebugTools.kt) — each is a no-op without an active duel. */
private fun clashDebugSections(viewModel: ClashDecksViewModel): List<DebugSection> = listOf(
    DebugSection(
        "Health & energy",
        listOf(
            DebugItem.Action("Heal you to full") { viewModel.debugHealPlayer() },
            DebugItem.Action("Heal opponent to full") { viewModel.debugHealCpu() },
            DebugItem.Action("Drop you to 1 HP") { viewModel.debugDamagePlayerToOne() },
            DebugItem.Action("Drop opponent to 1 HP") { viewModel.debugDamageCpuToOne() },
            DebugItem.Action("+5 energy (you)") { viewModel.debugGrantPlayerEnergy() },
            DebugItem.Action("Draw a card (you)") { viewModel.debugDrawPlayerCard() }
        )
    ),
    DebugSection(
        "Status effects",
        listOf(
            DebugItem.Action("Cleanse your debuffs") { viewModel.debugCleansePlayer() },
            DebugItem.Action("Poison opponent") { viewModel.debugPoisonCpu() },
            DebugItem.Action("Freeze opponent") { viewModel.debugFreezeCpu() }
        )
    ),
    DebugSection(
        "Duel outcome",
        listOf(
            DebugItem.Action("Force a win") { viewModel.debugForceWin() },
            DebugItem.Action("Force a loss") { viewModel.debugForceLose() },
            DebugItem.Info("All of the above need an active duel — they silently do nothing from the menu.")
        )
    )
)
