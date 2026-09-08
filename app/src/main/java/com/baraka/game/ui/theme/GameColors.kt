package com.baraka.game.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Shared color tokens for this app's dark-UI games. Every game here draws its own screens
 * with literal `Color(...)` values rather than MaterialTheme's ColorScheme, so this is a
 * plain, reusable palette any game can pull from — starting with a text hierarchy, since
 * `Color.Gray`/`Color.DarkGray` read as near-invisible against this app's near-black
 * surfaces (e.g. Knight Survivor's Compendium cards).
 */
object GameColors {
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB8BFCC)
    val TextMuted = Color(0xFF8A93A3)
    val TextDisabled = Color(0xFF5C6472)

    val Accent = Color(0xFFFFD54F)
    val Success = Color(0xFF66BB6A)
    val Danger = Color(0xFFE53935)
}
