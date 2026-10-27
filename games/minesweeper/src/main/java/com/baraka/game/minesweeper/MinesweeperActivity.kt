package com.baraka.game.minesweeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.baraka.game.coop.CoopLobbyScreen
import com.baraka.game.minesweeper.coop.MINE_COOP
import com.baraka.game.minesweeper.coop.SimulatedMineLink
import com.baraka.game.minesweeper.ui.MineGameScreen
import com.baraka.game.minesweeper.ui.MineMenuScreen
import com.baraka.game.minesweeper.ui.MineSettingsScreen
import com.baraka.game.ui.theme.GameTheme

class MinesweeperActivity : ComponentActivity() {
    private val viewModel: MineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                MinesweeperRoot(viewModel, onExit = { finish() })
            }
        }
    }
}

private enum class MineScreen { MENU, SETTINGS, LOBBY, GAME }

@Composable
private fun MinesweeperRoot(vm: MineViewModel, onExit: () -> Unit) {
    val versionName = com.baraka.game.minesweeper.BuildConfig.MINESWEEPER_VERSION
    // Survive an Activity recreation mid-game: if the view model still has a board, go straight back to it.
    var screen by remember { mutableStateOf(if (vm.snap != null) MineScreen.GAME else MineScreen.MENU) }

    when (screen) {
        MineScreen.MENU -> MineMenuScreen(
            vm,
            onPlaySolo = { screen = MineScreen.GAME },
            onPlayFriend = { screen = MineScreen.LOBBY },
            onSettings = { screen = MineScreen.SETTINGS },
            onBack = onExit
        )
        MineScreen.SETTINGS -> MineSettingsScreen(vm.settings, versionName, vm::updateSettings) { screen = MineScreen.MENU }
        MineScreen.LOBBY -> CoopLobbyScreen(
            spec = MINE_COOP,
            heading = "MINESWEEPER WITH A FRIEND",
            intro = "Pair both phones in Bluetooth settings first. Whoever hosts picks the mode and board size and keeps the rules; the other phone joins and plays on the same board at the same time.",
            practiceNote = "A simulated partner plays the second seat so you can try the game alone.",
            simulate = { SimulatedMineLink() },
            onReady = { ready ->
                vm.begin(ready.link, ready.side)
                screen = MineScreen.GAME
            },
            onClose = { screen = MineScreen.MENU }
        )
        MineScreen.GAME -> MineGameScreen(vm, versionName, onLeave = {
            vm.end()
            screen = MineScreen.MENU
        })
    }
}
