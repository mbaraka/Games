package com.baraka.game.tunnelcrew

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.tunnelcrew.ui.TunnelCrewScreen
import com.baraka.game.ui.theme.GameTheme

class TunnelCrewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                TunnelCrewScreen()
            }
        }
    }
}
