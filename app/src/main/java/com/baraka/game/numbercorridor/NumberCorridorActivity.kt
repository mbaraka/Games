package com.baraka.game.numbercorridor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.numbercorridor.ui.NumberCorridorScreen
import com.baraka.game.ui.theme.GameTheme

class NumberCorridorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                NumberCorridorScreen()
            }
        }
    }
}
