package com.baraka.game.clashdecks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.baraka.game.clashdecks.ui.ClashDecksScreen
import com.baraka.game.ui.theme.GameTheme

class ClashDecksActivity : ComponentActivity() {

    private val viewModel: ClashDecksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                val model by viewModel.ui.collectAsState()
                ClashDecksScreen(
                    model = model,
                    onBack = { finish() },
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
                    onBackToMenu = viewModel::backToMenu
                )
            }
        }
    }
}
