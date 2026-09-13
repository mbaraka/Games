package com.baraka.game.numbercorridor.systems

import com.baraka.game.numbercorridor.model.Bullet
import com.baraka.game.numbercorridor.model.PlayerState
import com.baraka.game.numbercorridor.model.WallKind
import com.baraka.game.numbercorridor.model.WallRow

/**
 * Bullets travel from the player's line (progress 1) toward the horizon (progress 0); wall
 * rows travel the opposite way. A hit is simply "a bullet's progress has dropped to or below
 * a row's" while they still share a lane with a live segment — no pixel math needed since
 * both live on the same normalized depth axis, see model/GameState.kt.
 *
 * A lane can carry more than one live bullet at once (a big squad fires several bullets a
 * second from the same lane, see GameLoopOrchestrator.fireWeapons), so each segment is
 * whittled down by every bullet that reached it this tick, not just the first.
 */
object CollisionSystem {

    fun resolve(
        rows: List<WallRow>,
        bullets: List<Bullet>,
        player: PlayerState
    ): Resolved {
        var scoreGained = 0
        var damageTaken = 0
        var updatedPlayer = player

        val remainingBulletsByLane = bullets.groupBy { it.lane }.mapValues { it.value.toMutableList() }
        val survivingRows = mutableListOf<WallRow>()

        for (row in rows) {
            var segments = row.segments
            for (lane in segments.indices) {
                var segment = segments[lane] ?: continue
                val laneBullets = remainingBulletsByLane[lane] ?: continue
                val arrivedBullets = laneBullets.filter { it.progress <= row.progress }
                if (arrivedBullets.isEmpty()) continue

                for (bullet in arrivedBullets) {
                    laneBullets.remove(bullet)
                    val newHp = segment.hp - updatedPlayer.damage.toInt().coerceAtLeast(1)
                    if (newHp <= 0) {
                        scoreGained += 10
                        when (val kind = segment.kind) {
                            is WallKind.SquadShift -> {
                                updatedPlayer = applySquadShift(updatedPlayer, kind)
                                scoreGained += 15
                            }
                            is WallKind.WeaponCrate -> {
                                updatedPlayer = updatedPlayer.copy(weaponTier = updatedPlayer.weaponTier.shifted(kind.delta))
                                scoreGained += 15
                            }
                            is WallKind.Obstacle -> Unit
                        }
                        segments = segments.toMutableList().also { it[lane] = null }
                        break // segment gone, remaining bullets this tick just pass through the gap
                    } else {
                        segment = segment.copy(hp = newHp)
                        segments = segments.toMutableList().also { it[lane] = segment }
                    }
                }
            }

            if (row.progress >= 1f) {
                val blocked = updatedPlayer.lane in segments.indices && segments[updatedPlayer.lane] != null
                if (blocked) damageTaken += 1
                // Row reached the player's line: it's resolved either way, don't keep it.
            } else {
                survivingRows += row.copy(segments = segments)
            }
        }

        val survivingBullets = remainingBulletsByLane.values.flatten().filter { it.progress > 0f }

        return Resolved(
            rows = survivingRows,
            bullets = survivingBullets,
            player = updatedPlayer,
            scoreGained = scoreGained,
            damageTaken = damageTaken
        )
    }

    private fun applySquadShift(player: PlayerState, shift: WallKind.SquadShift): PlayerState {
        val newCount = shift.op.apply(player.squadCount.toFloat(), shift.value)
            .toInt()
            .coerceIn(PlayerState.MIN_SQUAD, PlayerState.MAX_SQUAD)
        return player.copy(squadCount = newCount)
    }

    data class Resolved(
        val rows: List<WallRow>,
        val bullets: List<Bullet>,
        val player: PlayerState,
        val scoreGained: Int,
        val damageTaken: Int
    )
}
