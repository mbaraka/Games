package com.baraka.game.dreadnought.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dreadnought's palette, decided once. Nothing else in the game hard-codes a UI colour: dark navy surfaces with
 * light text, brass for the one thing to press, and dark text only ever on the brass fill.
 */
object NavyColors {
    val Bg = Color(0xFF0B1722)
    val Card = Color(0xFF14293B)
    val Bar = Color(0xFF1F3C55)
    val Accent = Color(0xFFFFB74D)
    val OnAccent = Color(0xFF1B1200)
    val Text = Color(0xFFEAF2F8)
    val Muted = Color(0xFF93A9BA)
    val Good = Color(0xFF66BB6A)
    val Bad = Color(0xFFEF5350)
    val Water = Color(0xFF123248)
    val Scrim = Color(0xCC050B11)
    val Disabled = Color(0xFF16222D)
    val OnDisabled = Color(0xFF6E8294)
}

/** The one button every screen, dialog and overlay uses, with its colours set explicitly for every state. */
@Composable
fun NavyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    fontSize: TextUnit = 14.sp,
    compact: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        contentPadding = if (compact) PaddingValues(horizontal = 2.dp, vertical = 8.dp) else ButtonDefaults.ContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (accent) NavyColors.Accent else NavyColors.Bar,
            contentColor = if (accent) NavyColors.OnAccent else NavyColors.Text,
            disabledContainerColor = NavyColors.Disabled,
            disabledContentColor = NavyColors.OnDisabled
        )
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun NavyLabel(text: String, modifier: Modifier = Modifier, size: TextUnit = 14.sp, color: Color = NavyColors.Text, bold: Boolean = false) {
    Text(text, modifier = modifier, color = color, fontSize = size, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
}
