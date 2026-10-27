package com.baraka.game.arcshot.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every colour used by Arc Shot's own screens, decided before any screen was built. */
internal object ArcColors {
    val Bg = Color(0xFF0B1220)
    val Card = Color(0xFF162236)
    val Bar = Color(0xFF101A2C)
    val SkyTop = Color(0xFF1B3A63)
    val SkyBottom = Color(0xFF0B1220)
    val Ground = Color(0xFF2E4A2E)
    val Obstacle = Color(0xFF7A5230)
    val Target = Color(0xFFE05353)
    val TargetRing = Color(0xFFFFD9D9)
    val Ball = Color(0xFFFFC94A)
    val Trail = Color(0xFF4FC3F7)
    val Accent = Color(0xFF4FC3F7)
    val OnAccent = Color(0xFF071726)
    val Text = Color.White
    val Muted = Color(0xFFA9B8CC)
    val Good = Color(0xFF69F0AE)
    val Bad = Color(0xFFFF6E6E)
    val Danger = Color(0xFFB3261E)
    val Star = Color(0xFFFFD54A)
}

/**
 * The one button used on Arc Shot screens. Colours are always explicit - Material would otherwise
 * pick a black label for our dark custom containers.
 */
@Composable
internal fun ArcButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = when {
        danger -> ArcColors.Danger
        primary -> ArcColors.Accent
        else -> Color(0xFF233249)
    }
    val content = if (primary) ArcColors.OnAccent else Color.White
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = content.copy(alpha = 0.4f)
        )
    ) {
        Text(text, color = if (enabled) content else content.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
