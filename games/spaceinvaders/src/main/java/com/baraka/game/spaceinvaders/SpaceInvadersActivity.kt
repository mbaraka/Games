package com.baraka.game.spaceinvaders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.spaceinvaders.ui.SpaceInvadersScreen
import com.baraka.game.ui.theme.GameTheme

class SpaceInvadersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                SpaceInvadersScreen()
            }
        }
    }
}
