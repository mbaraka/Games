package com.baraka.game.basedefender

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeaponButton(
    weaponType: WeaponType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (text, color) = when (weaponType) {
        WeaponType.BASIC -> Pair("Basic", Color.Yellow)
        WeaponType.FIRE -> Pair("Fire", Color.Red)
        WeaponType.ICE -> Pair("Ice", Color.Cyan)
        WeaponType.EXPLOSIVE -> Pair("Explosive", Color(0xFFFF9800))
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else Color.Gray
        ),
        modifier = Modifier
//            .size(60.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else Color.Black
        )
    }
}