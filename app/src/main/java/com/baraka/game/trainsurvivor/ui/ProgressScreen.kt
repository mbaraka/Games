package com.baraka.game.trainsurvivor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.trainsurvivor.model.Milestone
import com.baraka.game.trainsurvivor.model.Progression
import com.baraka.game.trainsurvivor.model.SaveData
import java.time.LocalDate

private val BG = Color(0xFF10131A)
private val CARD = Color(0xFF1D222D)
private val AMBER = Color(0xFFFFB300)
private val MUTED = Color(0xFF9AA3B2)

/** Daily goals, lifetime achievements, the local leaderboard and prestige. [onChange] saves the new data. */
@Composable
fun ProgressScreen(save: SaveData, onChange: (SaveData) -> Unit, onClose: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    BackHandler(onBack = onClose)
    val synced = Progression.sync(save)

    Column(
        modifier = Modifier.fillMaxSize().background(BG).windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = CARD, contentColor = Color.White)) {
                Text("< Back", fontSize = 12.sp)
            }
            Text("GOALS & PROGRESS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            listOf("DAILY", "ACHIEVEMENTS", "RECORDS", "PRESTIGE").forEachIndexed { i, label ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (tab == i) AMBER else CARD)
                        .clickable { tab = i }.padding(horizontal = 12.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) { Text(label, color = if (tab == i) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
        }
        when (tab) {
            0 -> DailyGoals(synced, onChange)
            1 -> Achievements(synced, onChange)
            2 -> Records(synced)
            else -> PrestigePage(synced, onChange)
        }
    }
}

@Composable
private fun DailyGoals(save: SaveData, onChange: (SaveData) -> Unit) {
    val goals = Progression.goalsForDay(save.goalDay)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("New goals every day. Progress counts from all your runs today.", color = MUTED, fontSize = 12.sp) }
        items(goals.indices.toList()) { i ->
            val goal = goals[i]
            val progress = save.goalProgress.getOrElse(i) { 0 }
            val claimed = save.goalClaimed and (1 shl i) != 0
            val done = progress >= goal.target
            ProgressCard(
                title = goal.title,
                subtitle = "${minOf(progress, goal.target)} / ${goal.target}   -   reward ${goal.reward} coins",
                fraction = progress.toFloat() / goal.target,
                actionLabel = if (claimed) "Claimed" else if (done) "Claim" else "In progress",
                actionEnabled = done && !claimed
            ) { Progression.claimGoal(save, i)?.let(onChange) }
        }
    }
}

@Composable
private fun Achievements(save: SaveData, onChange: (SaveData) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(Milestone.entries.toList()) { m ->
            val done = m.isDone(save)
            val claimed = save.milestonesClaimed and (1 shl m.ordinal) != 0
            ProgressCard(
                title = m.title,
                subtitle = "${m.description}   -   reward ${m.reward} coins",
                fraction = if (done) 1f else 0f,
                actionLabel = if (claimed) "Claimed" else if (done) "Claim" else "Locked",
                actionEnabled = done && !claimed
            ) { Progression.claimMilestone(save, m)?.let(onChange) }
        }
    }
}

@Composable
private fun Records(save: SaveData) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Your five longest runs.", color = MUTED, fontSize = 12.sp) }
        if (save.records.isEmpty()) item { Text("No runs yet - go set a record!", color = Color.White, fontSize = 14.sp) }
        items(save.records.indices.toList()) { i ->
            val r = save.records[i]
            Row(
                Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("#${i + 1}", color = AMBER, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    Text("${r.distance} m", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${r.kills} zombies   -   ${r.coins} coins", color = MUTED, fontSize = 12.sp)
                }
                Text(runCatching { LocalDate.ofEpochDay(r.day).toString() }.getOrDefault(""), color = MUTED, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PrestigePage(save: SaveData, onChange: (SaveData) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val required = Progression.prestigeRequirement(save)
    val can = Progression.canPrestige(save)
    Column(
        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Prestige level ${save.prestige}", color = AMBER, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "Permanent coin bonus: +${save.prestige * 10}%.  Prestige again to raise it by another 10%.",
            color = Color.White, fontSize = 13.sp
        )
        Text(
            "Prestiging resets your coins, upgrades, weapons and seats back to the start. Your records, goals, achievements and stats are kept.",
            color = MUTED, fontSize = 12.sp
        )
        Text(
            if (can) "Ready!" else "Reach $required m in a single run to unlock (best so far: ${save.bestDistance} m).",
            color = if (can) Color(0xFF66BB6A) else MUTED, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Button(
            onClick = { confirm = true },
            enabled = can,
            colors = ButtonDefaults.buttonColors(containerColor = AMBER, contentColor = Color.Black)
        ) { Text("PRESTIGE", fontWeight = FontWeight.ExtraBold) }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Prestige?") },
            text = { Text("You will lose all coins, upgrades, weapons and seats, and gain a permanent +10% coin bonus.") },
            confirmButton = { TextButton(onClick = { confirm = false; Progression.prestige(save)?.let(onChange) }) { Text("Prestige") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ProgressCard(
    title: String,
    subtitle: String,
    fraction: Float,
    actionLabel: String,
    actionEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MUTED, fontSize = 11.sp)
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF2A3342))) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp).background(AMBER))
            }
        }
        Button(
            onClick = onClick,
            enabled = actionEnabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), disabledContainerColor = Color(0xFF2A2F3A), disabledContentColor = MUTED),
            modifier = Modifier.width(112.dp)
        ) { Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
    }
}
