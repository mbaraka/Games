package com.baraka.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Prompts for the parent PIN, then calls [onUnlocked]. If no PIN has been set yet, the entered
 * value becomes the new PIN (first-run setup) instead of being checked against one.
 */
@Composable
fun PinPromptDialog(onDismiss: () -> Unit, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val pinAlreadySet = remember { ParentalControls.isPinSet(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pinAlreadySet) "Enter Parent PIN" else "Create a Parent PIN") },
        text = {
            Column {
                if (!pinAlreadySet) {
                    Text("This PIN will protect the daily play-time limit.")
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6); error = false },
                    label = { Text("PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    modifier = Modifier.padding(top = 8.dp)
                )
                if (error) {
                    Text("Incorrect PIN", color = androidx.compose.ui.graphics.Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length < 4) {
                    error = true
                    return@TextButton
                }
                if (pinAlreadySet) {
                    if (ParentalControls.checkPin(context, pin)) onUnlocked() else error = true
                } else {
                    ParentalControls.setPin(context, pin)
                    onUnlocked()
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ParentSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var limitText by remember {
        mutableStateOf(
            ParentalControls.getDailyLimitMinutes(context).let { if (it > 0) it.toString() else "" }
        )
    }
    val usedToday = remember { ParentalControls.getMinutesUsedToday(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Parent Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Played today: $usedToday min")
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Daily limit (minutes, 0 = off)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { ParentalControls.addBonusMinutes(context, 15) }) {
                        Text("+15 min today")
                    }
                    Button(onClick = { ParentalControls.resetUsageToday(context) }) {
                        Text("Reset today")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                ParentalControls.setDailyLimitMinutes(context, limitText.toIntOrNull() ?: 0)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
