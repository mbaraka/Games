package com.baraka.game.minesweeper.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The whole palette of Minesweeper. Nothing else in the game hard-codes a colour. */
object MineColors {
    val Background = Color(0xFF10151C)
    val Card = Color(0xFF1B2330)
    val Bar = Color(0xFF232E3F)
    val Accent = Color(0xFF4DD0E1)
    val OnAccent = Color(0xFF00262B)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFFB0B8C6)
    val Good = Color(0xFF66BB6A)
    val Bad = Color(0xFFFF5252)
    val Danger = Color(0xFFFF7043)
    val OnLight = Color(0xFF10151C)

    val Hidden = Color(0xFF4A5A70)
    val HiddenEdge = Color(0xFF6B7F9A)
    val Open = Color(0xFF1E2733)
    val Mine = Color(0xFFEEEEEE)
    val Boom = Color(0xFFB71C1C)

    /** The two players' tints: mine is cyan, the partner's is amber. */
    val Me = Color(0xFF4FC3F7)
    val Partner = Color(0xFFFFB74D)

    /** Digit colours for 1..8 (index 0 unused), all readable on [Open]. */
    val Digits = arrayOf(
        Color.Transparent, Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFF8A80), Color(0xFFB39DDB),
        Color(0xFFFFB74D), Color(0xFF4DD0E1), Color(0xFFF5F5F5), Color(0xFFBDBDBD)
    )
}

/** The one button every screen, dialog and overlay uses, so its label colour is always explicit. */
@Composable
fun MineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    val container = if (primary || selected) MineColors.Accent else MineColors.Bar
    val content = if (primary || selected) MineColors.OnAccent else MineColors.Text
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = MineColors.Bar.copy(alpha = 0.5f),
            disabledContentColor = MineColors.Muted
        )
    ) {
        androidx.compose.material3.Text(text, maxLines = 1)
    }
}
