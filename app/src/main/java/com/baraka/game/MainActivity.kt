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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baraka.game.basedefender.BaseDefenderGame
import com.baraka.game.battleship.BattleShipActivity
import com.baraka.game.clashdecks.ClashDecksActivity
import com.baraka.game.keyreader.KeyReaderActivity
import com.baraka.game.knightsurvivor.KnightSurvivorActivity
import com.baraka.game.memorymatch.MemoryMatchGame
import com.baraka.game.numbercorridor.NumberCorridorActivity
import com.baraka.game.numbercruncher.NumberCruncherGame
import com.baraka.game.rhythmrunner.RhythmRunnerGame
import com.baraka.game.snakeladder.SnakeLadderActivity
import com.baraka.game.ui.theme.GameTheme
import com.baraka.game.zombie.ZombieGame

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                GameSelectionScreen(
                    onGameSelected = { gameType ->
                        when (gameType) {
                            GameType.PIXEL_COLLECTOR -> {
                                val intent = Intent(this, PixelCollectorGame::class.java)
                                startActivity(intent)
                            }
                            GameType.BASE_DEFENDER -> {
                                val intent = Intent(this, BaseDefenderGame::class.java)
                                startActivity(intent)
                            }
                            GameType.MEMORY_MATCH -> {
                                val intent = Intent(this, MemoryMatchGame::class.java)
                                startActivity(intent)
                            }
                            GameType.SNAKES_LADDERS -> {
                                val intent = Intent(this, SnakeLadderActivity::class.java)
                                startActivity(intent)
                            }
                            GameType.RHYTHM_RUNNER -> {
                                val intent = Intent(this, RhythmRunnerGame::class.java)
                                startActivity(intent)
                            }
                            GameType.NUMBER_CRUNCHER -> {
                                val intent = Intent(this, NumberCruncherGame::class.java)
                                startActivity(intent)
                            }
                            GameType.ZOMBIE_SURVIVAL -> {
                                val intent = Intent(this, ZombieGame::class.java)
                                startActivity(intent)
                            }
                            GameType.KEY_READER -> {
                                val intent = Intent(this, KeyReaderActivity::class.java)
                                startActivity(intent)
                            }
                            GameType.BATTLESHIP -> {
                                val intent = Intent(this, BattleShipActivity::class.java)
                                startActivity(intent)
                            }
                            GameType.KNIGHT_SURVIVOR -> {
                                val intent = Intent(this, KnightSurvivorActivity::class.java)
                                startActivity(intent)
                            }
                            GameType.CLASH_DECKS -> {
                                val intent = Intent(this, ClashDecksActivity::class.java)
                                startActivity(intent)
                            }
                            GameType.NUMBER_CORRIDOR -> {
                                val intent = Intent(this, NumberCorridorActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    }
                )
            }
        }
    }
}

enum class GameType {
    PIXEL_COLLECTOR,
    BASE_DEFENDER,
    MEMORY_MATCH,
    RHYTHM_RUNNER,
    NUMBER_CRUNCHER,
    ZOMBIE_SURVIVAL,
    KEY_READER,
    SNAKES_LADDERS,
    BATTLESHIP,
    KNIGHT_SURVIVOR,
    CLASH_DECKS,
    NUMBER_CORRIDOR
}

@Composable
fun GameSelectionScreen(onGameSelected: (GameType) -> Unit) {
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

            // Game selection cards
            SectionHeader(text = "Favorites")

            Favorites(onGameSelected)

            var otherGamesExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { otherGamesExpanded = !otherGamesExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(text = "Other Games")
                ExpandChevron(expanded = otherGamesExpanded)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (otherGamesExpanded) {
                    GameCard(
                        title = "Pixel Collector",
                        description = "Collect green pixels while avoiding red obstacles!",
                        color = Color(0xFF4CAF50),
                        onClick = { onGameSelected(GameType.PIXEL_COLLECTOR) }
                    )

                    GameCard(
                        title = "Battleship",
                        description = "Local 2 players or vs computer — place fleets, then take turns firing.",
                        color = Color(0xFF0277BD),
                        onClick = { onGameSelected(GameType.BATTLESHIP) }
                    )

                    GameCard(
                        title = "Rhythm Runner",
                        description = "Run, jump, and collect to the beat!",
                        color = Color(0xFF9C27B0),
                        onClick = { onGameSelected(GameType.RHYTHM_RUNNER) }
                    )

                    GameCard(
                        title = "Zombie Survival",
                        description = "3D FPS - Survive zombie waves, upgrade weapons!",
                        color = Color(0xFF8B0000),
                        onClick = { onGameSelected(GameType.ZOMBIE_SURVIVAL) }
                    )

                    GameCard(
                        title = "Key Reader",
                        description = "Scan NFC keys and manage saved entries.",
                        color = Color(0xFF2E7D32),
                        onClick = { onGameSelected(GameType.KEY_READER) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

        }
    }

}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        color = Color.Gray,
        fontSize = 18.sp,
        modifier = Modifier.fillMaxWidth()
    )
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

@Composable
fun ColumnScope.Favorites(onGameSelected: (GameType) -> Unit) {
    GameCard(
        title = "Knight Survivor",
        description = "Arena survivor — auto-attack hordes, level up, and outlast the clock!",
        color = Color(0xFF7B1FA2),
        onClick = { onGameSelected(GameType.KNIGHT_SURVIVOR) }
    )

    GameCard(
        title = "Number Corridor",
        description = "Forward-scrolling shooter — blast obstacle walls, grab +/-/x/÷ buff walls for damage, fire rate, and squad size.",
        color = Color(0xFF00ACC1),
        onClick = { onGameSelected(GameType.NUMBER_CORRIDOR) }
    )

    GameCard(
        title = "Clash Decks",
        description = "Card duel vs computer — Attack, Defense, Support, and status-effect tactics.",
        color = Color(0xFF3949AB),
        onClick = { onGameSelected(GameType.CLASH_DECKS) }
    )

    GameCard(
        title = "Memory Match",
        description = "Find matching pairs of gaming cards!",
        color = Color(0xFF2196F3),
        onClick = { onGameSelected(GameType.MEMORY_MATCH) }
    )

    GameCard(
        title = "Number Cruncher",
        description = "Combine numbers to reach the target!",
        color = Color(0xFFFF6B6B),
        onClick = { onGameSelected(GameType.NUMBER_CRUNCHER) }
    )

    GameCard(
        title = "Snakes & Ladders",
        description = "2D board — 2–6 players, each human or CPU.",
        color = Color(0xFF00897B),
        onClick = { onGameSelected(GameType.SNAKES_LADDERS) }
    )

    GameCard(
        title = "Base Defender",
        description = "Defend your base! Aim and shoot at incoming enemies!",
        color = Color(0xFFFF5722),
        onClick = { onGameSelected(GameType.BASE_DEFENDER) }
    )
}