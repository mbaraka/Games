package com.baraka.game.dreadnought

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.dreadnought.ui.DreadnoughtScreen
import com.baraka.game.ui.theme.GameTheme

class DreadnoughtActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                DreadnoughtScreen()
            }
        }
    }
}
