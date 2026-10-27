package com.baraka.game.arcshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.arcshot.ui.ArcShotScreen
import com.baraka.game.ui.theme.GameTheme

class ArcShotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                ArcShotScreen()
            }
        }
    }
}
