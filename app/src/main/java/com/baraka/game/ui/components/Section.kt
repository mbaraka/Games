package com.baraka.game.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A row with a title and optional trailing content — a plain section header (just [text]),
 * a header with a clickable icon (e.g. a sound toggle), or a header with any other trailing
 * composable (e.g. an expand/collapse chevron). One component covers all three so screens
 * don't grow their own header variants.
 */
@Composable
fun Section(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: TextUnit = 24.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
        when {
            trailingIcon != null -> Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier
                    .size(28.dp)
                    .let { iconModifier ->
                        if (onTrailingIconClick != null) iconModifier.clickable(onClick = onTrailingIconClick) else iconModifier
                    }
            )
            trailingContent != null -> trailingContent()
        }
    }
}
