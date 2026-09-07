package com.baraka.game.battleship

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.baraka.game.ui.theme.GameTheme

class BattleShipActivity : ComponentActivity() {

    private val viewModel: BattleShipViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                val uiState by viewModel.uiState.collectAsState()
                BattleShipScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onChooseMode = viewModel::startGame,
                    onBackToMenu = viewModel::backToMenu,
                    onToggleOrientation = viewModel::toggleSetupOrientation,
                    onPlaceCell = viewModel::tryPlaceShip,
                    onConfirmHandoffP1 = viewModel::confirmHandoffAfterP1,
                    onConfirmHandoffP2 = viewModel::confirmHandoffAfterP2,
                    onFire = viewModel::fire,
                    onPlayAgain = viewModel::playAgain
                )
            }
        }
    }
}
