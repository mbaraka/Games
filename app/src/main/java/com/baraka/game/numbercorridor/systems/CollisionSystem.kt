package com.baraka.game.numbercorridor.systems

import com.baraka.game.numbercorridor.model.Bullet
import com.baraka.game.numbercorridor.model.BuffTarget
import com.baraka.game.numbercorridor.model.PlayerState
import com.baraka.game.numbercorridor.model.WallKind
import com.baraka.game.numbercorridor.model.WallRow
import com.baraka.game.numbercorridor.model.WallSegment

/**
 * Bullets travel from the player's line (progress 1) toward the horizon (progress 0); wall
 * rows travel the opposite way. A hit is simply "a bullet's progress has dropped to or below
 * a row's" while they still share a lane with a live segment — no pixel math needed since
 * both live on the same normalized depth axis, see model/GameState.kt.
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
                val segment = segments[lane] ?: continue
                val laneBullets = remainingBulletsByLane[lane] ?: continue
                val hit = laneBullets.firstOrNull { it.progress <= row.progress }
                if (hit != null) {
                    laneBullets.remove(hit)
                    val newHp = segment.hp - updatedPlayer.damage.toInt().coerceAtLeast(1)
                    segments = if (newHp <= 0) {
                        scoreGained += 10
                        if (segment.kind is WallKind.Buff) {
                            updatedPlayer = applyBuff(updatedPlayer, segment.kind)
                            scoreGained += 15
                        }
                        segments.toMutableList().also { it[lane] = null }
                    } else {
                        segments.toMutableList().also { it[lane] = segment.copy(hp = newHp) }
                    }
                }
            }

            if (row.progress >= 1f) {
                val stillBlocked = updatedPlayer.occupiedLanes.any { lane ->
                    lane in segments.indices && segments[lane] != null
                }
                if (stillBlocked) damageTaken += 1
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

    private fun applyBuff(player: PlayerState, buff: WallKind.Buff): PlayerState {
        return when (buff.target) {
            BuffTarget.DAMAGE -> player.copy(
                damage = buff.op.apply(player.damage, buff.value).coerceAtLeast(1f)
            )
            BuffTarget.FIRE_RATE -> player.copy(
                fireRateMultiplier = buff.op.apply(player.fireRateMultiplier, buff.value).coerceIn(0.2f, 6f)
            )
            BuffTarget.SQUAD -> {
                val newCount = buff.op.apply(player.squadCount.toFloat(), buff.value)
                    .toInt()
                    .coerceIn(PlayerState.MIN_SQUAD, PlayerState.MAX_SQUAD)
                val maxStart = WallRow.LANE_COUNT - newCount
                player.copy(
                    squadCount = newCount,
                    laneStart = player.laneStart.coerceIn(0, maxStart)
                )
            }
        }
    }

    data class Resolved(
        val rows: List<WallRow>,
        val bullets: List<Bullet>,
        val player: PlayerState,
        val scoreGained: Int,
        val damageTaken: Int
    )
}
