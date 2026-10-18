package com.baraka.game.basedefender.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The game's whole palette, decided once. Nothing else in Base Defender hard-codes a colour for UI:
 * light text sits on dark fills, dark text only on accent fills ([OnAccent]).
 */
object BdColors {
    val Bg = Color(0xFF0B1020)
    val Card = Color(0xFF161C33)
    val Bar = Color(0xFF10162B)
    val Overlay = Color(0xE60B1020)
    val Accent = Color(0xFFFFC107)
    val OnAccent = Color(0xFF1A1300)
    val Text = Color(0xFFFFFFFF)
    val Muted = Color(0xFFA4AECB)
    val Good = Color(0xFF66BB6A)
    val Bad = Color(0xFFEF5350)
    val Danger = Color(0xFFFF5252)
    val Info = Color(0xFF4DD0E1)
    val Money = Color(0xFFFFD54F)
    val Chip = Color(0xFF232B47)
    val Disabled = Color(0xFF2B3350)
    val OnDisabled = Color(0xFF7C88AA)
    val Outline = Color(0xFF3A4468)
}

/** The one button style for the whole game, with every colour explicit (Material would pick black labels on our dark cards). */
@Composable
fun BdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = BdColors.Accent,
    content: Color = BdColors.OnAccent,
    fontSize: TextUnit = 15.sp
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = BdColors.Disabled,
            disabledContentColor = BdColors.OnDisabled
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = fontSize, fontWeight = FontWeight.Bold, color = if (enabled) content else BdColors.OnDisabled)
    }
}

/** Secondary (dark) button, same shape. */
@Composable
fun BdGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    BdButton(text, onClick, modifier, enabled, container = BdColors.Chip, content = BdColors.Text)

/** A small selectable chip: [tint] fill with dark text when selected, dark fill with tinted outline otherwise. */
@Composable
fun BdChip(
    label: String,
    selected: Boolean,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 40.dp
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .background(if (selected) tint else BdColors.Chip, shape)
            .border(BorderStroke(1.5.dp, tint), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) BdColors.OnAccent else BdColors.Text)
    }
}

/** A read-only status pill (power-ups): lit with its colour when [active], dim when not. */
@Composable
fun BdPill(label: String, active: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .background(if (active) tint else BdColors.Bar, shape)
            .border(BorderStroke(1.dp, if (active) tint else BdColors.Outline), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (active) BdColors.OnAccent else BdColors.OnDisabled)
    }
}
