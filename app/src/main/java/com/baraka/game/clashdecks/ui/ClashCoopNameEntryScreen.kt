package com.baraka.game.clashdecks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.ui.theme.GameColors

private const val MAX_NAME_LENGTH = 18

/**
 * One step before the Bluetooth lobby: what the partner sees you as. Shown every time "Play
 * vs Friend" is opened (pre-filled with the last name used, via
 * [com.baraka.game.clashdecks.data.ClashDecksSettingsRepository]) rather than asked once and
 * forgotten, since two people sharing one phone across sessions might want to correct it.
 */
@Composable
fun ClashCoopNameEntryScreen(initialName: String, onContinue: (String) -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }

    Box(
        modifier = Modifier.fillMaxSize().background(ScreenBg).windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Play vs Friend", color = GameColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "What should your opponent see you as?",
                color = GameColors.TextSecondary,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                singleLine = true,
                label = { Text("Your name") },
                placeholder = { Text("You") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onContinue(name.trim()) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GameColors.TextPrimary,
                    unfocusedTextColor = GameColors.TextPrimary,
                    focusedLabelColor = GameColors.Accent,
                    unfocusedLabelColor = GameColors.TextMuted,
                    focusedPlaceholderColor = GameColors.TextMuted,
                    unfocusedPlaceholderColor = GameColors.TextMuted,
                    focusedBorderColor = GameColors.Accent,
                    unfocusedBorderColor = GameColors.TextMuted,
                    cursorColor = GameColors.Accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Left blank, you'll just show up as \"You\".",
                color = GameColors.TextMuted,
                fontSize = 12.sp
            )
            PillButton("Continue", onClick = { onContinue(name.trim()) })
            PillButton("Back", onClick = onClose, primary = false)
        }
    }
}
