package com.baraka.game.snakeladder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.baraka.game.ui.theme.GameTheme

class SnakeLadderActivity : ComponentActivity() {

    private val viewModel: SnakeLadderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                val uiState by viewModel.uiState.collectAsState()
                SnakeLadderScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onStartGame = viewModel::startGame,
                    onRoll = viewModel::rollDice,
                    onPlayAgain = viewModel::playAgain,
                    onMenu = {
                        viewModel.backToMenu()
                    }
                )
            }
        }
    }
}
