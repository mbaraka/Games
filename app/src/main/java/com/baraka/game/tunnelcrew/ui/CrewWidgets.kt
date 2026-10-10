package com.baraka.game.tunnelcrew.ui

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

/** Every colour used by Tunnel Crew's own screens, so contrast is decided in one place. */
internal object CrewColors {
    val Bg = Color(0xFF10131A)
    val Card = Color(0xFF1D222D)
    val Bar = Color(0xFF161B26)
    val Accent = Color(0xFFFFB300)
    val OnAccent = Color(0xFF1A1200)
    val Text = Color.White
    val Muted = Color(0xFFB4BDCC)
    val Good = Color(0xFF69F0AE)
    val Bad = Color(0xFFFF6E6E)
    val Danger = Color(0xFFB3261E)
}

/**
 * The one button used on Tunnel Crew screens. Colours are always explicit - Material would otherwise
 * pick a black label for our dark custom containers. [primary] is the amber call-to-action.
 */
@Composable
internal fun CrewButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val container = when {
        primary -> CrewColors.Accent
        danger -> CrewColors.Danger
        else -> Color(0xFF2E3647)
    }
    val content = if (primary) CrewColors.OnAccent else Color.White
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) {
        Text(text, color = content, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
