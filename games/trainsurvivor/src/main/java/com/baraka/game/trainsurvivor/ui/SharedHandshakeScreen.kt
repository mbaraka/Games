package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.coop.CoopLink
import com.baraka.game.trainsurvivor.coop.CoopMsg
import com.baraka.game.trainsurvivor.coop.SharedHandshake
import com.baraka.game.trainsurvivor.model.SaveData
import kotlinx.coroutines.delay

/** The moment after two phones connect for a shared world: they swap trains, then the run starts. Gives up after 8 seconds. */
@Composable
fun SharedHandshakeScreen(
    link: CoopLink<CoopMsg>,
    mySave: SaveData,
    onReady: (partner: SaveData) -> Unit,
    onFail: () -> Unit
) {
    val handshake = remember { SharedHandshake(link, mySave) }
    var message by remember { mutableStateOf("Syncing trains with your partner...") }
    var failed by remember { mutableStateOf(false) }
    BackHandler { link.close(); onFail() }

    LaunchedEffect(Unit) {
        var waited = 0
        while (true) {
            if (handshake.poll()) {
                onReady(handshake.partnerSave!!)
                return@LaunchedEffect
            }
            val why = handshake.failed
            if (why != null) {
                message = why
                failed = true
                return@LaunchedEffect
            }
            delay(50)
            waited += 50
            if (waited > 8000) {
                message = "Your partner did not answer. Both phones must pick SHARED WORLD (not Linked Trains) and run the same version."
                failed = true
                return@LaunchedEffect
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF12151C)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SHARED WORLD", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(message, color = if (failed) Color(0xFFFF8A65) else Color(0xFF80DEEA), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (failed) {
            Button(
                onClick = { link.close(); onFail() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3342), contentColor = Color.White)
            ) { Text("Back", fontWeight = FontWeight.Bold) }
        }
    }
}
