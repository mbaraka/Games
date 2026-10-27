package com.baraka.game.breakout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.breakout.ui.BreakoutScreen
import com.baraka.game.ui.theme.GameTheme

class BreakoutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                BreakoutScreen()
            }
        }
    }
}
