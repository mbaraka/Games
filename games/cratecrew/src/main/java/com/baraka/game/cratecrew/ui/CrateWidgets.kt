package com.baraka.game.cratecrew.ui

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

/** Every colour used by Crate Crew's own screens, decided before any screen was built. */
internal object CrateColors {
    val Bg = Color(0xFF17140F)
    val Card = Color(0xFF2A241A)
    val Bar = Color(0xFF221D15)
    val Floor = Color(0xFF3A3225)
    val Wall = Color(0xFF574B35)
    val Target = Color(0xFF8D6E3C)
    val Crate = Color(0xFFC9892F)
    val CrateOnTarget = Color(0xFF6FCB6B)
    val Player = Color(0xFFFFC94A)
    val Accent = Color(0xFFFFC94A)
    val OnAccent = Color(0xFF231B06)
    val Text = Color.White
    val Muted = Color(0xFFC9BE9C)
    val Good = Color(0xFF6FCB6B)
    val Bad = Color(0xFFFF6E6E)
    val Danger = Color(0xFFB3261E)
}

/**
 * The one button used on Crate Crew screens. Colours are always explicit - Material would otherwise
 * pick a black label for our dark custom containers.
 */
@Composable
internal fun CrateButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val container = when {
        danger -> CrateColors.Danger
        primary -> CrateColors.Accent
        else -> Color(0xFF3C3424)
    }
    val content = if (primary) CrateColors.OnAccent else Color.White
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
