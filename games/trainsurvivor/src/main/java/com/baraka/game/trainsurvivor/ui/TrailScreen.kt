package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.trainsurvivor.model.Progression
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.Shop
import com.baraka.game.trainsurvivor.model.TrailLook
import com.baraka.game.trainsurvivor.model.trailForDay
import com.baraka.game.trainsurvivor.render.drawTrailPortrait
import java.time.LocalDate

private val BG = Color(0xFF10131A)
private val CARD = Color(0xFF1D222D)
private val AMBER = Color(0xFFFFB300)
private val MUTED = Color(0xFF9AA3B2)
private val PORTRAIT_BG = Color(0xFF2A3342)

/** The Trails screen: previews today's trail animated, and lists every owned trail so it can be shown or
 * scrapped - so the collection never has to just keep growing. */
@Composable
fun TrailScreen(save: SaveData, onChange: (SaveData) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) time.floatValue += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
            }
        }
    }
    val today = Progression.today()
    val todays = trailForDay(today)
    val owned = save.trailsOwned.contains(today)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = CARD, contentColor = Color.White)) { Text("< Back", fontSize = 12.sp) }
            Text("TRAILS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("TODAY'S TRAIL", color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item { TrailCard(todays, time.floatValue, owned, save.selectedTrail == today, Shop.trailCost(save), save.coins, onChange, save, today) }

            if (save.selectedTrail != -1L) {
                item {
                    Row(
                        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Turn the trail off", color = MUTED, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Button(onClick = { onChange(Shop.selectTrail(save, -1L)) }, colors = ButtonDefaults.buttonColors(containerColor = CARD, contentColor = Color.White)) {
                            Text("NONE", fontSize = 12.sp)
                        }
                    }
                }
            }

            val others = save.trailsOwned.sortedDescending().filter { it != today }
            if (others.isNotEmpty()) {
                item { Text("YOUR COLLECTION (${others.size})", color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                items(others) { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Bought ${LocalDate.ofEpochDay(day)}", color = MUTED, fontSize = 10.sp)
                        TrailCard(trailForDay(day), time.floatValue, owned = true, active = save.selectedTrail == day, cost = 0, coins = save.coins, onChange = onChange, save = save, day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailCard(
    look: TrailLook,
    time: Float,
    owned: Boolean,
    active: Boolean,
    cost: Int?,
    coins: Int,
    onChange: (SaveData) -> Unit,
    save: SaveData,
    day: Long
) {
    Row(
        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 72.dp, height = 88.dp).clip(RoundedCornerShape(10.dp)).background(PORTRAIT_BG)) {
            Canvas(Modifier.fillMaxSize()) { drawTrailPortrait(look, time) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(look.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${look.shape.name.lowercase().replaceFirstChar { it.uppercase() }} motes", color = MUTED, fontSize = 11.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                !owned -> BuyButton("BUY", cost, cost != null && coins >= cost, Modifier, { Shop.buyTrail(save, day)?.let(onChange) })
                active -> Text("SHOWN", color = AMBER, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                else -> Button(onClick = { onChange(Shop.selectTrail(save, day)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("SHOW", fontSize = 12.sp) }
            }
            if (owned) {
                OutlinedButton(
                    onClick = { onChange(Shop.scrapTrail(save, day)) },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color(0xFFEF9A9A))
                ) { Text("DELETE", fontSize = 12.sp) }
            }
        }
    }
}
