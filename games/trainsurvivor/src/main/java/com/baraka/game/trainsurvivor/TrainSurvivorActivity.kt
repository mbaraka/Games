package com.baraka.game.trainsurvivor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.trainsurvivor.ui.TrainSurvivorScreen
import com.baraka.game.ui.theme.GameTheme

class TrainSurvivorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                TrainSurvivorScreen()
            }
        }
    }
}
