package com.baraka.game.knightsurvivor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.knightsurvivor.ui.KnightSurvivorScreen
import com.baraka.game.ui.theme.GameTheme

class KnightSurvivorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                KnightSurvivorScreen()
            }
        }
    }
}
