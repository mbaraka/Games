package com.baraka.game.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF1A1300),
    secondary = Color(0xFF66BB6A),
    onSecondary = Color(0xFF08210B),
    tertiary = Color(0xFFAB47BC),
    background = Color(0xFF121318),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1E26),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF262A35),
    onSurfaceVariant = Color(0xFFB8BFCC),
    outline = Color(0xFF3A3F4B),
    error = Color(0xFFE53935),
    onError = Color(0xFFFFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB8860B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF4A4F58),
    outline = Color(0xFFC9CCD1),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF)
)

/**
 * True when the effective theme is dark — the single switch [GameColors] and callers should
 * branch on, instead of re-deriving it from system settings.
 */
val isDarkTheme: Boolean
    @Composable get() = isSystemInDarkTheme()

@Composable
fun GameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
