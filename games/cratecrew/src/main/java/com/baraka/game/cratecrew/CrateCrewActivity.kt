package com.baraka.game.cratecrew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.cratecrew.ui.CrateCrewScreen
import com.baraka.game.ui.theme.GameTheme

class CrateCrewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                CrateCrewScreen()
            }
        }
    }
}
