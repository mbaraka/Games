package com.baraka.game.roadrush.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.roadrush.model.CarDesign

/**
 * Shown before a run starts so the player can pick which [CarDesign] their car wears —
 * purely cosmetic, same role as numbercorridor's ui/AvatarPickerOverlay.kt.
 */
@Composable
fun CarPickerOverlay(selected: CarDesign, onSelected: (CarDesign) -> Unit, onStart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Road Rush", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Dodge oncoming traffic by shifting lanes. You've got 3 lives — the road only gets faster.",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text("Choose your car", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CarDesign.entries.forEach { design ->
                    CarOptionCard(design = design, isSelected = design == selected, onClick = { onSelected(design) })
                }
            }

            Button(onClick = onStart) {
                Text("Start Driving")
            }
        }
    }
}

@Composable
private fun CarOptionCard(design: CarDesign, isSelected: Boolean, onClick: () -> Unit) {
    val carColor = Color(design.colorArgb)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color(0xFF1D1D1D), RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) carColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(28.dp)
                        .background(carColor, CircleShape)
                )
                Text(design.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(design.description, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
