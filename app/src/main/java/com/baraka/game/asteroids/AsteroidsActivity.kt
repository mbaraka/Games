package com.baraka.game.asteroids

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.asteroids.ui.AsteroidsScreen
import com.baraka.game.ui.theme.GameTheme

class AsteroidsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                AsteroidsScreen()
            }
        }
    }
}
