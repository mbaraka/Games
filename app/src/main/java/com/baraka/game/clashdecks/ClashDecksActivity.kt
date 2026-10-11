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
import com.baraka.game.clashdecks.ui.ClashCoopLaunch
import com.baraka.game.clashdecks.ui.ClashCoopLobbyScreen
import com.baraka.game.clashdecks.ui.ClashCoopScreen
import com.baraka.game.clashdecks.ui.ClashDecksScreen
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
 * Three mutually-exclusive screens live here: the normal (single-player) flow, the Bluetooth
 * lobby, and an active two-phone duel. [coopLaunch] is the only piece of local navigation
 * state — everything about the duel itself still lives in whichever ViewModel is driving it.
 */
@Composable
private fun ClashDecksRoot(
    viewModel: ClashDecksViewModel,
    coopViewModel: ClashCoopViewModel,
    onBack: () -> Unit
) {
    var showCoopLobby by remember { mutableStateOf(false) }
    var coopLaunch by remember { mutableStateOf<ClashCoopLaunch?>(null) }

    val launch = coopLaunch
    if (launch != null) {
        LaunchedEffect(launch) { coopViewModel.begin(launch.link, launch.role) }
        val coopModel by coopViewModel.ui.collectAsState()
        ClashCoopScreen(
            model = coopModel,
            opponentLabel = if (coopViewModel.isHost) "Player 2" else "Player 1",
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

    if (showCoopLobby) {
        ClashCoopLobbyScreen(
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
        onPlayVsFriend = { showCoopLobby = true }
    )
}
