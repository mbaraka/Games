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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.baraka.game.arcshot.ArcShotActivity
import com.baraka.game.asteroids.AsteroidsActivity
import com.baraka.game.basedefender.BaseDefenderGame
import com.baraka.game.battleship.BattleShipActivity
import com.baraka.game.breakout.BreakoutActivity
import com.baraka.game.clashdecks.ClashDecksActivity
import com.baraka.game.cratecrew.CrateCrewActivity
import com.baraka.game.dreadnought.DreadnoughtActivity
import com.baraka.game.keyreader.KeyReaderActivity
import com.baraka.game.knightsurvivor.KnightSurvivorActivity
import com.baraka.game.memorymatch.MemoryMatchGame
import com.baraka.game.minesweeper.MinesweeperActivity
import com.baraka.game.missilecommand.MissileCommandActivity
import com.baraka.game.numbercorridor.NumberCorridorActivity
import com.baraka.game.numbercruncher.NumberCruncherGame
import com.baraka.game.roadrush.RoadRushActivity
import com.baraka.game.snake.SnakeActivity
import com.baraka.game.snakeladder.SnakeLadderActivity
import com.baraka.game.spaceinvaders.SpaceInvadersActivity
import com.baraka.game.ui.components.Section
import com.baraka.game.ui.theme.GameColors
import com.baraka.game.ui.theme.GameTheme
import com.baraka.game.trainsurvivor.TrainSurvivorActivity
import com.baraka.game.tunnelcrew.TunnelCrewActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameTheme {
                GameSelectionScreen(
                    onGameSelected = { entry ->
                        GamePlayStats.recordPlay(this, entry.id)
                        ParentalControls.startSession(this)
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
    /** The icon drawn on the hub's grid tile — see GameGrid.kt. Pick something that reads as this game's subject, not its genre. */
    val icon: ImageVector,
    val activityClass: Class<out ComponentActivity>,
    /** Shows a multiplayer badge on the game's card (co-op, or several local players). */
    val multiplayer: Boolean = false,
    /** Broad categories this game belongs to, used only to filter the hub by genre. */
    val genres: Set<GameGenre> = emptySet()
)

val GAME_CATALOG: List<GameCatalogEntry> = listOf(
    GameCatalogEntry(
        id = "road_rush",
        title = "Road Rush",
        description = "Retro arcade-style vertical racer — shift lanes to dodge oncoming traffic before you run out of lives.",
        color = Color(0xFFE53935),
        icon = Icons.Default.DirectionsCar,
        activityClass = RoadRushActivity::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.RACING)
    ),
    GameCatalogEntry(
        id = "breakout",
        title = "Brick Blitz",
        description = "Retro arcade-style paddle & ball — smash every brick, catch power-ups, climb the levels.",
        color = Color(0xFF4FC3F7),
        icon = Icons.Default.GridView,
        activityClass = BreakoutActivity::class.java,
        genres = setOf(GameGenre.ARCADE)
    ),
    GameCatalogEntry(
        id = "space_invaders",
        title = "Star Invasion",
        description = "Retro arcade-style alien shooter — hold the line behind your bunkers as the formation marches down.",
        color = Color(0xFF66BB6A),
        icon = Icons.Default.Android,
        activityClass = SpaceInvadersActivity::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.SHOOTER)
    ),
    GameCatalogEntry(
        id = "missile_command",
        title = "Silo Defense",
        description = "Retro arcade-style defense — tap to detonate interceptors and save your cities from the falling missiles.",
        color = Color(0xFFFF7043),
        icon = Icons.Default.Shield,
        activityClass = MissileCommandActivity::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.STRATEGY)
    ),
    GameCatalogEntry(
        id = "snake",
        title = "Snake",
        description = "Retro arcade-style classic — swipe to turn, eat fruit, don't hit the walls or yourself.",
        color = Color(0xFF66BB6A),
        icon = Icons.Default.Route,
        activityClass = SnakeActivity::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.PUZZLE)
    ),
    GameCatalogEntry(
        id = "asteroids",
        title = "Rock Blaster",
        description = "Retro arcade-style vector shooter — rotate, thrust and blast drifting rocks that split as you hit them.",
        color = Color(0xFF9E9E9E),
        icon = Icons.Default.BlurCircular,
        activityClass = AsteroidsActivity::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.SHOOTER)
    ),
    GameCatalogEntry(
        id = "knight_survivor",
        title = "Knight Survivor",
        description = "Arena survivor — auto-attack hordes, level up, and outlast the clock!",
        color = Color(0xFF7B1FA2),
        icon = Icons.Default.Castle,
        activityClass = KnightSurvivorActivity::class.java,
        genres = setOf(GameGenre.SURVIVAL, GameGenre.ACTION)
    ),
    GameCatalogEntry(
        id = "number_corridor",
        title = "Number Corridor",
        description = "Forward-scrolling shooter — blast obstacle walls, grab +/-/x/÷ buff walls for damage, fire rate, and squad size.",
        color = Color(0xFF00ACC1),
        icon = Icons.Default.Functions,
        activityClass = NumberCorridorActivity::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.SHOOTER)
    ),
    GameCatalogEntry(
        id = "clash_decks",
        title = "Clash Decks",
        description = "Card duel vs computer — Attack, Defense, Support, and status-effect tactics.",
        color = Color(0xFF3949AB),
        icon = Icons.Default.Style,
        activityClass = ClashDecksActivity::class.java,
        multiplayer = true,
        genres = setOf(GameGenre.CARD, GameGenre.STRATEGY)
    ),
    GameCatalogEntry(
        id = "memory_match",
        title = "Memory Match",
        description = "Find matching pairs of gaming cards!",
        color = Color(0xFF2196F3),
        icon = Icons.Default.Psychology,
        activityClass = MemoryMatchGame::class.java,
        genres = setOf(GameGenre.PUZZLE)
    ),
    GameCatalogEntry(
        id = "number_cruncher",
        title = "Number Cruncher",
        description = "Combine numbers to reach the target!",
        color = Color(0xFFFF6B6B),
        icon = Icons.Default.Calculate,
        activityClass = NumberCruncherGame::class.java,
        genres = setOf(GameGenre.PUZZLE)
    ),
    GameCatalogEntry(
        id = "snakes_ladders",
        title = "Snakes & Ladders",
        description = "2D board — 2–6 players, each human or CPU.",
        color = Color(0xFF00897B),
        icon = Icons.Default.Casino,
        activityClass = SnakeLadderActivity::class.java,
        multiplayer = true,
        genres = setOf(GameGenre.BOARD)
    ),
    GameCatalogEntry(
        id = "base_defender",
        title = "Base Defender",
        description = "Defend your base! Aim and shoot at incoming enemies!",
        color = Color(0xFFFF5722),
        icon = Icons.Default.Security,
        activityClass = BaseDefenderGame::class.java,
        genres = setOf(GameGenre.ARCADE, GameGenre.STRATEGY)
    ),
    GameCatalogEntry(
        id = "battleship",
        title = "Fleet Duel",
        description = "Local 2 players or vs computer — place fleets, then take turns firing.",
        color = Color(0xFF0277BD),
        icon = Icons.Default.DirectionsBoat,
        activityClass = BattleShipActivity::class.java,
        multiplayer = true,
        genres = setOf(GameGenre.BOARD, GameGenre.STRATEGY)
    ),
    GameCatalogEntry(
        id = "train_survivor",
        title = "Train Survivor",
        description = "Ride an armored train through zombie hordes - earn coins, upgrade the train and its gunners.",
        color = Color(0xFF8B0000),
        icon = Icons.Default.Train,
        activityClass = TrainSurvivorActivity::class.java,
        multiplayer = true,
        genres = setOf(GameGenre.SURVIVAL, GameGenre.ACTION)
    ),
    GameCatalogEntry(
        id = "dreadnought",
        title = "Dreadnought",
        description = "Command a battleship - steer, aim your turrets, lead your shots and sink waves of destroyers, submarines, cruisers and carriers.",
        color = Color(0xFF2E5E7E),
        icon = Icons.Default.Anchor,
        activityClass = DreadnoughtActivity::class.java,
        genres = setOf(GameGenre.ACTION, GameGenre.STRATEGY)
    ),
    GameCatalogEntry(
        id = "tunnel_crew",
        title = "Tunnel Crew",
        description = "Lemmings-style puzzler - give your tiny crew skills (dig, build, bash, climb...) to guide them safely to the exit.",
        color = Color(0xFF8D6E63),
        icon = Icons.Default.Construction,
        activityClass = TunnelCrewActivity::class.java,
        genres = setOf(GameGenre.PUZZLE, GameGenre.STRATEGY)
    ),
    GameCatalogEntry(
        id = "minesweeper",
        title = "Minesweeper",
        description = "Classic solo Minesweeper, plus two-phone Bluetooth play - clear the board together or race for cells.",
        color = Color(0xFF4DD0E1),
        icon = Icons.Default.Flag,
        activityClass = MinesweeperActivity::class.java,
        multiplayer = true,
        genres = setOf(GameGenre.PUZZLE)
    ),
    GameCatalogEntry(
        id = "crate_crew",
        title = "Crate Crew",
        description = "Classic box-pushing puzzler - push every crate onto its target across 13 hand-verified levels. Undo anytime.",
        color = Color(0xFFC9892F),
        icon = Icons.Default.Inventory2,
        activityClass = CrateCrewActivity::class.java,
        genres = setOf(GameGenre.PUZZLE)
    ),
    GameCatalogEntry(
        id = "arc_shot",
        title = "Arc Shot",
        description = "Slingshot physics puzzler - drag back to aim, arc your shot over obstacles and land it on the target in as few tries as possible.",
        color = Color(0xFFE05353),
        icon = Icons.Default.TrackChanges,
        activityClass = ArcShotActivity::class.java,
        genres = setOf(GameGenre.PUZZLE, GameGenre.ARCADE)
    ),
    GameCatalogEntry(
        id = "key_reader",
        title = "Key Reader",
        description = "Scan NFC keys and manage saved entries.",
        color = Color(0xFF2E7D32),
        icon = Icons.Default.VpnKey,
        activityClass = KeyReaderActivity::class.java,
        genres = setOf(GameGenre.UTILITY)
    )
).filter { entry -> entry.id != "key_reader" || BuildConfig.DEBUG }

private const val MOST_PLAYED_COUNT = 5

@Composable
fun GameSelectionScreen(onGameSelected: (GameCatalogEntry) -> Unit) {
    val context = LocalContext.current
    val gameIds = remember(GAME_CATALOG) { GAME_CATALOG.map { it.id } }
    var playCounts by remember { mutableStateOf(GamePlayStats.getCounts(context, gameIds)) }
    var remainingMinutes by remember { mutableStateOf(ParentalControls.getRemainingMinutes(context)) }
    var showPinPrompt by remember { mutableStateOf(false) }
    var showParentSettings by remember { mutableStateOf(false) }

    // Re-read play counts and fold the just-finished game's playtime into today's usage
    // whenever the screen resumes (e.g. after returning from a game).
    DisposableEffect(Unit) {
        val lifecycleOwner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                ParentalControls.endSession(context)
                playCounts = GamePlayStats.getCounts(context, gameIds)
                remainingMinutes = ParentalControls.getRemainingMinutes(context)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    val timeUp = remainingMinutes == 0

    if (showPinPrompt) {
        PinPromptDialog(
            onDismiss = { showPinPrompt = false },
            onUnlocked = {
                showPinPrompt = false
                showParentSettings = true
            }
        )
    }
    if (showParentSettings) {
        ParentSettingsDialog(onDismiss = {
            showParentSettings = false
            remainingMinutes = ParentalControls.getRemainingMinutes(context)
        })
    }

    var selectedGenre by remember { mutableStateOf<GameGenre?>(null) }
    val genreFiltered = selectedGenre?.let { genre -> GAME_CATALOG.filter { genre in it.genres }.sortedBy { it.title } }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    text = "Game Hub",
                    color = GameColors.TextPrimary,
                    fontSize = 36.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Parent Settings",
                    tint = GameColors.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(28.dp)
                        .clickable { showPinPrompt = true }
                )
            }
        }
    ) { padding->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Choose Your Adventure",
                color = GameColors.TextSecondary,
                fontSize = 18.sp,
                modifier = Modifier
            )

            GenreFilterRow(selectedGenre = selectedGenre, onSelect = { selectedGenre = it })

            if (timeUp) {
                Text(
                    text = "Time's up for today! Ask a parent for more time.",
                    color = GameColors.Warning,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            } else {
                remainingMinutes?.let { minutes ->
                    Text(
                        text = "$minutes min left today",
                        color = GameColors.TextMuted,
                        fontSize = 14.sp
                    )
                }
            }

            if (genreFiltered != null) {
                Section(
                    text = "${selectedGenre?.label} (${genreFiltered.size})",
                    textColor = GameColors.TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal
                )
                if (genreFiltered.isEmpty()) {
                    Text("No games in this category yet.", color = GameColors.TextMuted, fontSize = 14.sp)
                } else {
                    GameGrid(entries = genreFiltered, enabled = !timeUp, onGameSelected = onGameSelected)
                }
            } else {
                if (mostPlayed.isNotEmpty()) {
                    Section(text = "Most Played", textColor = GameColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Normal)
                    GameGrid(entries = mostPlayed, enabled = !timeUp, onGameSelected = onGameSelected)
                }

                var allGamesExpanded by remember(mostPlayed.isEmpty()) {
                    mutableStateOf(mostPlayed.isEmpty())
                }

                Section(
                    text = if (mostPlayed.isEmpty()) "All Games" else "Other Games",
                    textColor = GameColors.TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.clickable { allGamesExpanded = !allGamesExpanded },
                    trailingContent = { ExpandChevron(expanded = allGamesExpanded) }
                )

                Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    if (allGamesExpanded) {
                        GameGrid(entries = remainingGames, enabled = !timeUp, onGameSelected = onGameSelected)
                    }
                }
            }

            Text(
                text = "Open Source Licenses",
                color = GameColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable {
                        context.startActivity(Intent(context, com.baraka.game.licenses.OpenSourceLicensesActivity::class.java))
                    }
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

        }
    }

}

/** A horizontally scrolling row of genre chips ("All" plus every [GameGenre]) that drives the hub's filter. */
@Composable
private fun GenreFilterRow(selectedGenre: GameGenre?, onSelect: (GameGenre?) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedGenre == null,
                onClick = { onSelect(null) },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        items(GameGenre.entries) { genre ->
            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onSelect(if (selectedGenre == genre) null else genre) },
                label = { Text(genre.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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
    val strokeColor = GameColors.TextPrimary
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
            color = strokeColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
