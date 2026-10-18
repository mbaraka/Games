package com.baraka.game.basedefender

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baraka.game.basedefender.ui.BaseDefenderScreen
import com.baraka.game.ui.theme.GameTheme

/**
 * Base Defender. Code map (see also AGENTS.md "Requirements for EVERY game"):
 *  - `model/`    pure data: `Content.kt` (weapons, enemies, power-ups, upgrades, turret roles), `Waves.kt` (pacing)
 *  - `systems/`  `BaseDefenderEngine` (the simulation), pooled entities and effects
 *  - `render/`   `BaseDefenderRenderer` (scene) and `EnemyArts` (one art per `EnemyShape`)
 *  - `data/`     settings and best score
 *  - `ui/`       palette + buttons, HUD screen, overlays, settings, debug cheats
 */
class BaseDefenderGame : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                BaseDefenderScreen()
            }
        }
    }
}
