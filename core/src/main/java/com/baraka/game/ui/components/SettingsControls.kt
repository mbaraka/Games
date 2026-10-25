package com.baraka.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Shared building blocks for every game's Settings screen. Each game keeps its own settings model and
 * screen, but builds it from these rows so all settings look and behave the same. See AGENTS.md.
 *
 * Colours default to the Material theme; screens with a fixed dark background pass their own.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    titleColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().background(background, RoundedCornerShape(12.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title.uppercase(), color = titleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    mutedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (description != null) Text(description, color = mutedColor, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    valueLabel: (Float) -> String = { "${(it * 100).roundToInt()}%" }
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = contentColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(valueLabel(value), color = contentColor, fontSize = 13.sp, modifier = Modifier.width(52.dp))
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}
