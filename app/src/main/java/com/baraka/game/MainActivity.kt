package com.baraka.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.baraka.game.basedefender.BaseDefenderGame
import com.baraka.game.battleship.BattleShipActivity
import com.baraka.game.clashdecks.ClashDecksActivity
import com.baraka.game.keyreader.KeyReaderActivity
import com.baraka.game.knightsurvivor.KnightSurvivorActivity
import com.baraka.game.memorymatch.MemoryMatchGame
import com.baraka.game.numbercorridor.NumberCorridorActivity
import com.baraka.game.numbercruncher.NumberCruncherGame
import com.baraka.game.rhythmrunner.RhythmRunnerGame
import com.baraka.game.roadrush.RoadRushActivity
import com.baraka.game.snakeladder.SnakeLadderActivity
import com.baraka.game.ui.components.Section
import com.baraka.game.ui.theme.GameTheme
import com.baraka.game.zombie.ZombieGame

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                GameSelectionScreen(
                    onGameSelected = { entry ->
                        GamePlayStats.recordPlay(this, entry.id)
                        startActivity(Intent(this, entry.activityClass))
                    }
                )
            }
        }
    }
}

/**
 * A single entry point for adding a new game to the hub: append one [GameCatalogEntry] to
 * [GAME_CATALOG] with its own launcher Activity. No enum, no `when` branch, and no other
 * file needs to change — "Most Played" / alphabetical sorting and play-count tracking all
 * key off [GameCatalogEntry.id] automatically.
 */
data class GameCatalogEntry(
    val id: String,
    val title: String,
    val description: String,
    val color: Color,
    val activityClass: Class<out ComponentActivity>
)

val GAME_CATALOG: List<GameCatalogEntry> = listOf(
    GameCatalogEntry(
        id = "road_rush",
        title = "Road Rush",
        description = "Atari-style vertical racer — shift lanes to dodge oncoming traffic before you run out of lives.",
        color = Color(0xFFE53935),
        activityClass = RoadRushActivity::class.java
    ),
    GameCatalogEntry(
        id = "knight_survivor",
        title = "Knight Survivor",
        description = "Arena survivor — auto-attack hordes, level up, and outlast the clock!",
        color = Color(0xFF7B1FA2),
        activityClass = KnightSurvivorActivity::class.java
    ),
    GameCatalogEntry(
        id = "number_corridor",
        title = "Number Corridor",
        description = "Forward-scrolling shooter — blast obstacle walls, grab +/-/x/÷ buff walls for damage, fire rate, and squad size.",
        color = Color(0xFF00ACC1),
        activityClass = NumberCorridorActivity::class.java
    ),
    GameCatalogEntry(
        id = "clash_decks",
        title = "Clash Decks",
        description = "Card duel vs computer — Attack, Defense, Support, and status-effect tactics.",
        color = Color(0xFF3949AB),
        activityClass = ClashDecksActivity::class.java
    ),
    GameCatalogEntry(
        id = "memory_match",
        title = "Memory Match",
        description = "Find matching pairs of gaming cards!",
        color = Color(0xFF2196F3),
        activityClass = MemoryMatchGame::class.java
    ),
    GameCatalogEntry(
        id = "number_cruncher",
        title = "Number Cruncher",
        description = "Combine numbers to reach the target!",
        color = Color(0xFFFF6B6B),
        activityClass = NumberCruncherGame::class.java
    ),
    GameCatalogEntry(
        id = "snakes_ladders",
        title = "Snakes & Ladders",
        description = "2D board — 2–6 players, each human or CPU.",
        color = Color(0xFF00897B),
        activityClass = SnakeLadderActivity::class.java
    ),
    GameCatalogEntry(
        id = "base_defender",
        title = "Base Defender",
        description = "Defend your base! Aim and shoot at incoming enemies!",
        color = Color(0xFFFF5722),
        activityClass = BaseDefenderGame::class.java
    ),
    GameCatalogEntry(
        id = "pixel_collector",
        title = "Pixel Collector",
        description = "Collect green pixels while avoiding red obstacles!",
        color = Color(0xFF4CAF50),
        activityClass = PixelCollectorGame::class.java
    ),
    GameCatalogEntry(
        id = "battleship",
        title = "Battleship",
        description = "Local 2 players or vs computer — place fleets, then take turns firing.",
        color = Color(0xFF0277BD),
        activityClass = BattleShipActivity::class.java
    ),
    GameCatalogEntry(
        id = "rhythm_runner",
        title = "Rhythm Runner",
        description = "Run, jump, and collect to the beat!",
        color = Color(0xFF9C27B0),
        activityClass = RhythmRunnerGame::class.java
    ),
    GameCatalogEntry(
        id = "zombie_survival",
        title = "Zombie Survival",
        description = "3D FPS - Survive zombie waves, upgrade weapons!",
        color = Color(0xFF8B0000),
        activityClass = ZombieGame::class.java
    ),
    GameCatalogEntry(
        id = "key_reader",
        title = "Key Reader",
        description = "Scan NFC keys and manage saved entries.",
        color = Color(0xFF2E7D32),
        activityClass = KeyReaderActivity::class.java
    )
)

private const val MOST_PLAYED_COUNT = 5

@Composable
fun GameSelectionScreen(onGameSelected: (GameCatalogEntry) -> Unit) {
    val context = LocalContext.current
    val gameIds = remember(GAME_CATALOG) { GAME_CATALOG.map { it.id } }
    var playCounts by remember { mutableStateOf(GamePlayStats.getCounts(context, gameIds)) }

    // Re-read play counts whenever the screen resumes (e.g. after returning from a game).
    DisposableEffect(Unit) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                playCounts = GamePlayStats.getCounts(context, gameIds)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    val mostPlayed = GAME_CATALOG
        .filter { (playCounts[it.id] ?: 0) > 0 }
        .sortedWith(compareByDescending<GameCatalogEntry> { playCounts[it.id] ?: 0 }.thenBy { it.title })
        .take(MOST_PLAYED_COUNT)
    val mostPlayedIds = mostPlayed.map { it.id }.toSet()
    val remainingGames = GAME_CATALOG
        .filterNot { it.id in mostPlayedIds }
        .sortedBy { it.title }

    Scaffold(
        topBar = {
            Text(
                text = "Game Hub",
                color = Color.White,
                fontSize = 36.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 8.dp)
            )
        }
    ) { padding->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 6.dp)
                .fillMaxSize()
                .background(Color(0xFF1E1E1E))
                .verticalScroll(rememberScrollState())
                ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Choose Your Adventure",
                color = Color.Gray,
                fontSize = 18.sp,
                modifier = Modifier
            )

            if (mostPlayed.isNotEmpty()) {
                Section(text = "Most Played", textColor = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Normal)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    mostPlayed.forEach { entry ->
                        GameCard(
                            title = entry.title,
                            description = entry.description,
                            color = entry.color,
                            onClick = { onGameSelected(entry) }
                        )
                    }
                }
            }

            var allGamesExpanded by remember(mostPlayed.isEmpty()) {
                mutableStateOf(mostPlayed.isEmpty())
            }

            Section(
                text = if (mostPlayed.isEmpty()) "All Games" else "Other Games",
                textColor = Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable { allGamesExpanded = !allGamesExpanded },
                trailingContent = { ExpandChevron(expanded = allGamesExpanded) }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (allGamesExpanded) {
                    remainingGames.forEach { entry ->
                        GameCard(
                            title = entry.title,
                            description = entry.description,
                            color = entry.color,
                            onClick = { onGameSelected(entry) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

        }
    }

}

/**
 * A small hand-drawn expand/collapse arrow. A plain Text glyph ("▾"/"▸") rendered too
 * faintly to read against the dark background on some devices/fonts, so this draws the
 * arrow itself with an explicit, always-visible stroke color instead of depending on a
 * font's glyph coverage or weight.
 */
@Composable
private fun ExpandChevron(expanded: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp)) {
        val path = Path().apply {
            if (expanded) {
                moveTo(size.width * 0.2f, size.height * 0.35f)
                lineTo(size.width * 0.5f, size.height * 0.65f)
                lineTo(size.width * 0.8f, size.height * 0.35f)
            } else {
                moveTo(size.width * 0.35f, size.height * 0.2f)
                lineTo(size.width * 0.65f, size.height * 0.5f)
                lineTo(size.width * 0.35f, size.height * 0.8f)
            }
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun GameCard(
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF2D2D2D) else Color(0xFF1A1A1A)
        ),
        onClick = if (enabled) onClick else { {} }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game icon/color indicator
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (enabled) color else Color.Gray,
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Game info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = description,
                    color = if (enabled) Color.Gray else Color.DarkGray,
                    fontSize = 14.sp
                )
            }

            // Arrow indicator
            if (enabled) {
                Text(
                    text = "→",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
