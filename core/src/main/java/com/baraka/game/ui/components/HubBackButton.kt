package com.baraka.game.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baraka.game.ui.theme.GameColors

/**
 * A small top-left control that returns to the Game Hub by finishing the current Activity.
 * Every game's start/menu screen should carry one, so the player always has an on-screen tap
 * back to the hub instead of only the phone's own back gesture - see AGENTS.md's UI consistency
 * rules. Colors default to [GameColors]/[MaterialTheme] so this reads correctly in both Light
 * and Dark theme on a game screen that uses the shared theme; pass explicit [tint]/[background]
 * for a game with its own fixed-dark palette (e.g. Dreadnought's `NavyColors`).
 */
@Composable
fun HubBackButton(
    modifier: Modifier = Modifier,
    tint: Color = GameColors.TextPrimary,
    background: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
) {
    val activity = LocalContext.current as? ComponentActivity
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back to Game Hub",
        tint = tint,
        modifier = modifier
            .background(background, CircleShape)
            .clickable(enabled = activity != null) { activity?.finish() }
            .padding(8.dp)
    )
}
