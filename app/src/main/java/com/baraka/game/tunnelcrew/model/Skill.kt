package com.baraka.game.tunnelcrew.model

import androidx.compose.ui.graphics.Color

/**
 * Everything the player can hand to a crew member. Add an entry here, give it a case in
 * `CrewSim.canAssign` / `CrewSim.assign`, and (for a new activity) a behaviour in `CrewSim`;
 * the skill bar, levels and debug tools pick it up from [entries].
 */
enum class Skill(val label: String, val description: String, val color: Color) {
    CLIMBER("Climb", "Climbs sheer walls (permanent)", Color(0xFF42A5F5)),
    FLOATER("Float", "Survives any fall (permanent)", Color(0xFFEC407A)),
    BOMBER("Bomb", "Blows up after 5 seconds", Color(0xFFEF5350)),
    BLOCKER("Block", "Stands still and turns the crew around", Color(0xFFFFA726)),
    BUILDER("Build", "Lays a staircase of 12 bricks", Color(0xFFFFCA28)),
    BASHER("Bash", "Tunnels straight ahead", Color(0xFF9CCC65)),
    MINER("Mine", "Tunnels diagonally down", Color(0xFF26C6DA)),
    DIGGER("Dig", "Digs straight down", Color(0xFFAB47BC))
}
