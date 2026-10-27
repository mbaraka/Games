package com.baraka.game.missilecommand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.missilecommand.ui.MissileCommandScreen
import com.baraka.game.ui.theme.GameTheme

class MissileCommandActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                MissileCommandScreen()
            }
        }
    }
}
