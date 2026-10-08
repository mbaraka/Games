package com.baraka.game.trainsurvivor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.trainsurvivor.model.AUTO_WEAPON
import com.baraka.game.trainsurvivor.model.CrewSlot
import com.baraka.game.trainsurvivor.model.WeaponType
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.baraka.game.trainsurvivor.model.MAX_POWER
import com.baraka.game.trainsurvivor.model.MAX_SLOTS
import com.baraka.game.trainsurvivor.model.SaveData
import com.baraka.game.trainsurvivor.model.Shop
import com.baraka.game.trainsurvivor.model.TrainUpgrade
import com.baraka.game.trainsurvivor.render.drawTrainScene
import com.baraka.game.trainsurvivor.systems.TrainGame
import kotlin.math.max
import com.baraka.game.trainsurvivor.model.Perk
import androidx.compose.foundation.layout.RowScope
import com.baraka.game.trainsurvivor.data.GameSettings
import com.baraka.game.trainsurvivor.model.MAX_CAR_ARMOR
import com.baraka.game.trainsurvivor.model.DangerLevel
import com.baraka.game.trainsurvivor.model.Progression
import com.baraka.game.trainsurvivor.model.SURVIVORS
import com.baraka.game.ui.debug.DebugSection

private val BG = Color(0xFF10131A)
private val CARD = Color(0xFF1D222D)
private val AMBER = Color(0xFFFFB300)
private val MUTED = Color(0xFF9AA3B2)

private enum class GaragePage { MAIN, GUIDE, GOALS, SETTINGS }

@Composable
fun GarageScreen(
    save: SaveData,
    settings: GameSettings,
    onSettingsChange: (GameSettings) -> Unit,
    versionName: String,
    onPurchase: (SaveData) -> Unit,
    onDepart: () -> Unit,
    debugSections: List<DebugSection>
) {
    var tab by remember { mutableIntStateOf(0) }
    var page by remember { mutableStateOf(GaragePage.MAIN) }
    when (page) {
        GaragePage.GUIDE -> {
            FieldGuideScreen(onClose = { page = GaragePage.MAIN })
            return
        }
        GaragePage.GOALS -> {
            ProgressScreen(save = save, onChange = onPurchase, onClose = { page = GaragePage.MAIN })
            return
        }
        GaragePage.SETTINGS -> {
            TrainSettingsScreen(
                settings = settings,
                onChange = onSettingsChange,
                onClose = { page = GaragePage.MAIN },
                versionName = versionName,
                debugSections = debugSections
            )
            return
        }
        GaragePage.MAIN -> {}
    }
    val goalsReady = Progression.hasClaimable(save)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- left: live preview of the train as currently built ----
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TRAIN SURVIVOR", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Best ${save.bestDistance} m  •  ${save.totalKills} kills  •  ${save.bossKills} bosses" +
                            if (save.prestige > 0) "  •  Prestige ${save.prestige}" else "",
                        color = MUTED, fontSize = 11.sp
                    )
                }
                CoinChip(save.coins)
            }
            TrainPreview(save, Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(14.dp)))
            DangerSelector(save, onPurchase)
            Button(
                onClick = onDepart,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AMBER, contentColor = Color.Black)
            ) { Text("DEPART  ▶", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1, softWrap = false) }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallButton("Field Guide") { page = GaragePage.GUIDE }
                SmallButton(if (goalsReady) "Goals ●" else "Goals", highlight = goalsReady) { page = GaragePage.GOALS }
                SmallButton("Settings") { page = GaragePage.SETTINGS }
            }
        }

        // ---- right: upgrades ----
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip("TRAIN", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabChip("CREW & WEAPONS", tab == 1, Modifier.weight(1f)) { tab = 1 }
            }
            if (tab == 0) TrainTab(save, onPurchase) else CrewTab(save, onPurchase)
        }
    }
}

@Composable
private fun DangerSelector(save: SaveData, onChange: (SaveData) -> Unit) {
    val selected = save.selectedDanger
    val firstLocked = DangerLevel.entries.firstOrNull { !save.dangerUnlocked(it.ordinal) }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Danger", color = MUTED, fontSize = 11.sp)
            DangerLevel.entries.forEach { level ->
                val unlocked = save.dangerUnlocked(level.ordinal)
                val isSelected = level == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(level.color) else Color(0xFF2A2F3A))
                        .clickable(enabled = unlocked) { onChange(save.copy(dangerLevel = level.ordinal)) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (unlocked) level.displayName else "Locked " + level.displayName,
                        color = if (isSelected) Color.Black else if (unlocked) Color.White else MUTED,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
            }
        }
        Text(
            if (selected == DangerLevel.NORMAL) "Normal: the standard challenge."
            else "${selected.displayName}: zombies x${fmtMult(selected.hpMult)} health, x${fmtMult(selected.dmgMult)} damage, x${fmtMult(selected.spawnMult)} spawns, more elites. Coins x${fmtMult(selected.coinMult)}.",
            color = Color(selected.color), fontSize = 10.sp, maxLines = 2
        )
        if (firstLocked != null) {
            val previous = DangerLevel.entries[firstLocked.ordinal - 1]
            Text(
                "Unlock ${firstLocked.displayName}: reach ${firstLocked.requirementMeters} m on ${previous.displayName} (best ${save.dangerBest.getOrElse(previous.ordinal) { 0 }} m).",
                color = MUTED, fontSize = 10.sp, maxLines = 2
            )
        }
    }
}

private fun fmtMult(x: Float): String = "%.2f".format(x).trimEnd('0').trimEnd('.')

@Composable
private fun RowScope.SmallButton(text: String, highlight: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(40.dp),
        contentPadding = PaddingValues(horizontal = 6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlight) Color(0xFF2E7D32) else CARD,
            contentColor = Color.White
        )
    ) { Text(text, fontSize = 12.sp, maxLines = 1, softWrap = false) }
}

@Composable
private fun TrainPreview(save: SaveData, modifier: Modifier) {
    val game = remember(save.unlockedSlots, save.slots, save.upgrades) {
        TrainGame(save, spawnEnabled = false).also { it.time = 5f }
    }
    val tick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(game) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                if (last != 0L) game.update(((now - last) / 1_000_000_000f).coerceAtMost(0.05f))
                last = now
                tick.longValue++
            }
        }
    }
    Canvas(
        modifier = modifier.onSizeChanged { game.worldW = game.trainLength + 200f }
    ) {
        tick.longValue
        // Frame the train: centre it vertically whatever the preview's aspect ratio.
        val visibleHeight = game.worldW * size.height / size.width
        drawTrainScene(game, topOffset = 450f - visibleHeight / 2f, showHud = false)
    }
}

@Composable
private fun TrainTab(save: SaveData, onPurchase: (SaveData) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(TrainUpgrade.entries.toList()) { upgrade ->
            val level = save.level(upgrade)
            val cost = Shop.upgradeCost(save, upgrade)
            UpgradeCard(
                title = upgrade.displayName,
                subtitle = upgrade.blurb,
                detail = "Level $level / ${upgrade.maxLevel}",
                cost = cost,
                canAfford = cost != null && save.coins >= cost,
                actionLabel = "UPGRADE",
                onClick = { Shop.buyUpgrade(save, upgrade)?.let(onPurchase) }
            )
        }
    }
}

@Composable
private fun CrewTab(save: SaveData, onPurchase: (SaveData) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items((0 until MAX_SLOTS).toList()) { index ->
            when {
                index < save.unlockedSlots -> CrewCard(index, save.slots[index], save, onPurchase)
                index == save.unlockedSlots -> {
                    val cost = Shop.slotCost(save)
                    UpgradeCard(
                        title = "New wagon + survivor",
                        subtitle = "Another gunner joins the train",
                        detail = "Passenger ${index + 1}",
                        cost = cost,
                        canAfford = cost != null && save.coins >= cost,
                        actionLabel = "ADD",
                        onClick = { Shop.buySlot(save)?.let(onPurchase) }
                    )
                }
                else -> Box(
                    Modifier.fillMaxWidth().background(CARD.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(14.dp)
                ) { Text("Passenger ${index + 1} — locked", color = MUTED, fontSize = 13.sp) }
            }
        }
    }
}

@Composable
private fun CrewCard(index: Int, slot: CrewSlot, save: SaveData, onPurchase: (SaveData) -> Unit) {
    val survivor = SURVIVORS[index]
    val powerCost = Shop.powerCost(slot)
    val armorCost = Shop.carArmorCost(index, slot)
    val autoMode = slot.selected == AUTO_WEAPON
    val inHand = if (autoMode) slot.bestWeapon else WeaponType.entries[slot.selected]
    Column(
        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("${survivor.name}  -  ${survivor.role}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(survivor.perkText, color = Color(0xFF81C784), fontSize = 11.sp)
        Text(
            if (autoMode) "AUTO - picks the best weapon for the situation" else "Always uses the ${inHand.displayName}",
            color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
        val damageFactor = slot.powerMultiplier * (if (survivor.perk == Perk.DAMAGE) 1.15f else 1f)
        Text(
            "${inHand.displayName}: ${Math.round(inHand.damage * damageFactor)} damage per shot (base ${inHand.damage.toInt()}, +${Math.round((damageFactor - 1f) * 100f)}%)",
            color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
        Text(
            "${slot.ownedWeapons.size}/${WeaponType.entries.size} weapons  •  power ${slot.power}/$MAX_POWER (+${slot.power * 30}% damage)  •  car armor ${slot.armor}/$MAX_CAR_ARMOR",
            color = MUTED, fontSize = 11.sp
        )

        // Which weapon this passenger uses: AUTO or one they own. (In a run you can also tap their chip to change it.)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip("AUTO", autoMode) { onPurchase(Shop.selectWeapon(save, index, AUTO_WEAPON)) }
            slot.ownedWeapons.forEach { w ->
                ModeChip(w.displayName, slot.selected == w.ordinal) { onPurchase(Shop.selectWeapon(save, index, w.ordinal)) }
            }
        }

        // Weapons still available to buy.
        val locked = WeaponType.entries.filter { !slot.owns(it) }
        if (locked.isNotEmpty()) {
            Text("Buy more weapons", color = MUTED, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                locked.forEach { w ->
                    BuyButton(
                        label = w.displayName,
                        cost = w.unlockCost,
                        enabled = save.coins >= w.unlockCost,
                        modifier = Modifier.width(112.dp)
                    ) { Shop.buyWeapon(save, index, w)?.let(onPurchase) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BuyButton(
                label = if (powerCost != null) "+30% power" else "MAX POWER",
                cost = powerCost,
                enabled = powerCost != null && save.coins >= powerCost,
                modifier = Modifier.weight(1f)
            ) { Shop.buyPower(save, index)?.let(onPurchase) }
            BuyButton(
                label = if (armorCost != null) (if (index == 0) "Loco armor +40%" else "Car armor +40%") else "MAX ARMOR",
                cost = armorCost,
                enabled = armorCost != null && save.coins >= armorCost,
                modifier = Modifier.weight(1f)
            ) { Shop.buyCarArmor(save, index)?.let(onPurchase) }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AMBER else Color(0xFF2A2F3A))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun UpgradeCard(
    title: String,
    subtitle: String,
    detail: String,
    cost: Int?,
    canAfford: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MUTED, fontSize = 11.sp)
            Text(detail, color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        BuyButton(
            label = if (cost == null) "MAX" else actionLabel,
            cost = cost,
            enabled = canAfford,
            modifier = Modifier.width(116.dp),
            onClick = onClick
        )
    }
}

@Composable
private fun BuyButton(label: String, cost: Int?, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32),
            disabledContainerColor = Color(0xFF2A2F3A),
            disabledContentColor = if (cost == null) AMBER else MUTED
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (cost != null) Text("$cost coins", fontSize = 11.sp)
        }
    }
}

@Composable
private fun TabChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) AMBER else CARD)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun CoinChip(coins: Int) {
    Row(
        Modifier.background(CARD, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(14.dp).height(14.dp).background(Color(0xFFFFC107), RoundedCornerShape(7.dp)))
        Spacer(Modifier.width(6.dp))
        Text("$coins", color = AMBER, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}
