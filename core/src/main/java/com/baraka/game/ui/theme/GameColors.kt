package com.baraka.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Shared, theme-aware color tokens for this app's games. Every property resolves against
 * [isDarkTheme] (or [MaterialTheme.colorScheme] where a Material role already fits) so games
 * render correctly in both Light and Dark theme instead of assuming the old dark-only UI.
 * Prefer these over literal `Color(0x...)` values in game screens.
 */
object GameColors {
    val TextPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val TextSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val TextMuted: Color @Composable get() = if (isDarkTheme) Color(0xFF8A93A3) else Color(0xFF6B7280)
    val TextDisabled: Color @Composable get() = if (isDarkTheme) Color(0xFF5C6472) else Color(0xFFB0B4BB)

    val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
    val Success: Color @Composable get() = if (isDarkTheme) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val Danger: Color @Composable get() = MaterialTheme.colorScheme.error
    val Warning: Color @Composable get() = if (isDarkTheme) Color(0xFFFFB300) else Color(0xFFB8860B)

    // Board-game tones (Snakes & Ladders and similar): pastel light backgrounds read fine on a
    // white board but wash out against a dark surface, so each has a dark-theme counterpart.
    val Ladder: Color @Composable get() = Success
    val Snake: Color @Composable get() = Danger
    val Finish: Color @Composable get() = if (isDarkTheme) Color(0xFFF9A825) else Color(0xFFF57F17)
    val LadderCellBackground: Color @Composable get() = if (isDarkTheme) Color(0xFF1B3320) else Color(0xFFC8E6C9)
    val SnakeCellBackground: Color @Composable get() = if (isDarkTheme) Color(0xFF3B1414) else Color(0xFFFFCDD2)
    val FinishCellBackground: Color @Composable get() = if (isDarkTheme) Color(0xFF3A3413) else Color(0xFFFFF9C4)
    val LadderCellHighlight: Color @Composable get() = if (isDarkTheme) Color(0xFFC9A227) else Color(0xFFFFD54F)
    val SnakeCellHighlight: Color @Composable get() = if (isDarkTheme) Color(0xFF8C3A3A) else Color(0xFFEF9A9A)
}
