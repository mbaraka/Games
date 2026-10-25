package com.baraka.game.basedefender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.basedefender.model.Tuning
import com.baraka.game.basedefender.model.UpgradeType
import com.baraka.game.basedefender.systems.BaseDefenderEngine
import com.baraka.game.basedefender.systems.BdHud

/** Full-screen dimmed layer that swallows touches, so nothing underneath (the canvas) reacts. */
@Composable
fun BdOverlay(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(BdColors.Overlay).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun BdPanel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .padding(20.dp)
            .background(BdColors.Card, RoundedCornerShape(18.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun BdStartOverlay(bestScore: Int, bestWave: Int, onStart: () -> Unit, onSettings: () -> Unit, onExit: () -> Unit) {
    BdOverlay {
        BdPanel {
            Text("BASE DEFENDER", color = BdColors.Accent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Hold the line for ${Tuning.TOTAL_WAVES} waves. Your turrets fire on their own;\n" +
                    "drag anywhere to take aim yourself. Shoot falling power-up crates to grab them.",
                color = BdColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            if (bestScore > 0) Text("Best: $bestScore  -  wave $bestWave", color = BdColors.Money, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            BdButton("Start", onStart, Modifier.fillMaxWidth(), fontSize = 18.sp)
            BdGhostButton("Settings", onSettings, Modifier.fillMaxWidth())
            BdGhostButton("Exit to hub", onExit, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun BdPauseOverlay(onResume: () -> Unit, onShop: () -> Unit, onSettings: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    BdOverlay {
        BdPanel {
            Text("PAUSED", color = BdColors.Accent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            BdButton("Resume", onResume, Modifier.fillMaxWidth(), fontSize = 18.sp)
            BdGhostButton("Upgrade shop", onShop, Modifier.fillMaxWidth())
            BdGhostButton("Settings", onSettings, Modifier.fillMaxWidth())
            BdGhostButton("Restart run", onRestart, Modifier.fillMaxWidth())
            BdGhostButton("Exit to hub", onExit, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun BdEndOverlay(victory: Boolean, engine: BaseDefenderEngine, newBest: Boolean, onAgain: () -> Unit, onSettings: () -> Unit, onExit: () -> Unit) {
    BdOverlay {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            BdPanel {
                Text(
                    if (victory) "VICTORY!" else "GAME OVER",
                    color = if (victory) BdColors.Good else BdColors.Danger,
                    fontSize = 30.sp, fontWeight = FontWeight.ExtraBold
                )
                if (victory) Text("You held the line through all ${Tuning.TOTAL_WAVES} waves.", color = BdColors.Text, fontSize = 15.sp, textAlign = TextAlign.Center)
                StatRow("Score", "${engine.score}", BdColors.Money)
                StatRow("Wave reached", "${engine.wave}", BdColors.Info)
                StatRow("Enemies destroyed", "${engine.kills}", BdColors.Good)
                StatRow("Money earned", "$${engine.moneyEarned}", BdColors.Money)
                StatRow("Shots fired", "${engine.shotsFired}", BdColors.Text)
                if (newBest) Text("NEW BEST SCORE!", color = BdColors.Accent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                BdButton("Play again", onAgain, Modifier.fillMaxWidth(), fontSize = 18.sp)
                BdGhostButton("Settings", onSettings, Modifier.fillMaxWidth())
                BdGhostButton("Exit to hub", onExit, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        Modifier.fillMaxWidth().background(BdColors.Bar, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BdColors.Muted, fontSize = 14.sp)
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * The upgrade shop. It lists [UpgradeType.entries], so a new upgrade appears here with no UI change.
 * [tick] changes after every purchase to make this recompose (the engine is plain mutable state).
 */
@Composable
fun BdShopOverlay(engine: BaseDefenderEngine, hud: BdHud, tick: Int, onBuy: (UpgradeType) -> Unit, onClose: () -> Unit) {
    BdOverlay {
        Column(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("UPGRADE SHOP", color = BdColors.Accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("$${hud.money}", color = BdColors.Money, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (type in UpgradeType.entries) {
                    UpgradeRow(engine, type, tick, onBuy)
                }
            }
            BdButton("Close", onClose, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun UpgradeRow(engine: BaseDefenderEngine, type: UpgradeType, tick: Int, onBuy: (UpgradeType) -> Unit) {
    // [tick] is a parameter so this row recomposes after every purchase (the engine is plain mutable state).
    val level = engine.level(type) + 0 * tick
    val maxed = level >= type.maxLevel
    val can = engine.canBuy(type)
    val note = if (type == UpgradeType.REPAIR && engine.lives >= Tuning.MAX_LIVES) "Lives are full" else type.description
    Row(
        modifier = Modifier.fillMaxWidth().background(BdColors.Card, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(type.title, color = BdColors.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(note, color = BdColors.Muted, fontSize = 12.sp)
            Text(if (maxed) "MAX" else "Level $level / ${type.maxLevel}", color = BdColors.Info, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (!maxed) BdButton("$${engine.cost(type)}", { onBuy(type) }, enabled = can, container = BdColors.Good, content = BdColors.OnAccent)
    }
}
