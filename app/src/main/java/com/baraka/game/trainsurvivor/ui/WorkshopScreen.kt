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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.trainsurvivor.model.CraftedWeapon
import com.baraka.game.trainsurvivor.model.Progression
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.Shop
import com.baraka.game.trainsurvivor.model.WeaponEffect
import com.baraka.game.trainsurvivor.model.craftedWeaponForDay
import com.baraka.game.trainsurvivor.render.drawWorkshopPortrait
import java.time.LocalDate

private val BG = Color(0xFF10131A)
private val CARD = Color(0xFF1D222D)
private val AMBER = Color(0xFFFFB300)
private val MUTED = Color(0xFF9AA3B2)
private val PORTRAIT_BG = Color(0xFF2A3342)

/** The Craft screen: breaks today's blueprint into its parts, shows the combined result, and lists every
 * crafted weapon still owned so it can be re-mounted or scrapped. */
@Composable
fun WorkshopScreen(save: SaveData, onChange: (SaveData) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val today = Progression.today()
    val todays = craftedWeaponForDay(today)
    val owned = todays.let { save.workshopOwned.contains(today) }

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
            Text("CRAFT A WEAPON", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("TODAY'S PARTS", color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PartCard("Archetype", todays.archetype.displayName, todays.archetype.blurb, Modifier.weight(1f))
                    PartCard("Element", todays.kind.displayName, "Matters against elemental enemies", Modifier.weight(1f))
                    PartCard("Effect", todays.effect.label.substringBefore(":"), todays.effect.label.substringAfter(": ", ""), Modifier.weight(1f))
                }
            }
            if (todays.inspiredBy != null) {
                item { PartCard("Inspiration", "${todays.inspiredBy.name} the ${todays.inspiredBy.role}", todays.inspiredBy.perkText, Modifier.fillMaxWidth()) }
            }

            item { Text("RESULT", color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
            item { ResultCard(todays, owned, save.workshopActive == today, Shop.workshopCost(save), save.coins, onChange, save, today) }

            val others = save.workshopOwned.sortedDescending().filter { it != today }
            if (others.isNotEmpty()) {
                item { Text("YOUR COLLECTION", color = MUTED, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
                items(others) { day ->
                    val w = craftedWeaponForDay(day)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Crafted ${LocalDate.ofEpochDay(day)}", color = MUTED, fontSize = 10.sp)
                        ResultCard(w, owned = true, active = save.workshopActive == day, cost = 0, coins = save.coins, onChange = onChange, save = save, day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun PartCard(label: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(CARD, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label.uppercase(), color = MUTED, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = MUTED, fontSize = 10.sp)
    }
}

@Composable
private fun ResultCard(
    weapon: CraftedWeapon,
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
            Canvas(Modifier.fillMaxSize().padding(8.dp)) { drawWorkshopPortrait(weapon) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(weapon.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "${weapon.kind.displayName} - ${weapon.damage.toInt()} dmg / ${"%.1f".format(1f / weapon.interval)} shots/s - range ${weapon.range.toInt()}" +
                    (if (weapon.pierce > 1) " - pierces ${weapon.pierce}" else "") +
                    (if (weapon.splashRadius > 0f) " - splash ${weapon.splashRadius.toInt()}" else ""),
                color = MUTED, fontSize = 11.sp
            )
            if (weapon.effect != WeaponEffect.NONE) Text(weapon.effect.label, color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (weapon.inspiredBy != null) Text("Inspired by ${weapon.inspiredBy.name}'s ${weapon.inspiredBy.perkText}", color = Color(0xFF80CBC4), fontSize = 10.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                !owned -> BuyButton("CRAFT", cost, cost != null && coins >= cost, Modifier, { Shop.craftWorkshopWeapon(save, day)?.let(onChange) })
                active -> Text("MOUNTED", color = AMBER, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                else -> Button(onClick = { onChange(Shop.selectWorkshopWeapon(save, day)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("MOUNT", fontSize = 12.sp) }
            }
            if (owned) {
                OutlinedButton(
                    onClick = { onChange(Shop.scrapWorkshopWeapon(save, day)) },
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color(0xFFEF9A9A))
                ) { Text("SCRAP", fontSize = 12.sp) }
            }
        }
    }
}
