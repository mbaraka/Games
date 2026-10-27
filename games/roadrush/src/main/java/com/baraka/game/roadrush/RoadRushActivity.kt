package com.baraka.game.roadrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.roadrush.ui.RoadRushScreen
import com.baraka.game.ui.theme.GameTheme

class RoadRushActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                RoadRushScreen()
            }
        }
    }
}
