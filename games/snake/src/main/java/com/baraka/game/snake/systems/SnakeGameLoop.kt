package com.baraka.game.snake.systems

import com.baraka.game.snake.model.Cell
import com.baraka.game.snake.model.Difficulty
import com.baraka.game.snake.model.Direction
import com.baraka.game.snake.model.GamePhase
import com.baraka.game.snake.model.SnakeState
import com.baraka.game.snake.model.SnakeState.Companion.GRID_HEIGHT
import com.baraka.game.snake.model.SnakeState.Companion.GRID_WIDTH
import com.baraka.game.snake.model.SnakeState.Companion.SCORE_PER_FOOD
import com.baraka.game.snake.model.SnakeState.Companion.STARTING_LENGTH
import kotlin.random.Random

/**
 * Advances a [SnakeState] by one real-time frame: same "pure `tick()` over immutable state"
 * shape as the other Atari-style games here, except the snake itself only moves once
 * [SnakeState.elapsedSinceMoveSeconds] crosses [SnakeState.moveIntervalSeconds] — `tick()` is
 * a no-op in between, so calling it every 16ms is cheap and the grid-step cadence lives
 * entirely in the state, not the caller.
 */
object SnakeGameLoop {

    fun newGame(difficulty: Difficulty, bestScore: Int, rng: Random): SnakeState {
        val startY = GRID_HEIGHT / 2
        val startX = GRID_WIDTH / 3
        val snake = (0 until STARTING_LENGTH).map { i -> Cell(startX - i, startY) } // head first, tail trailing left
        return SnakeState(
            snake = snake,
            direction = Direction.RIGHT,
            food = randomFreeCell(snake, rng),
            difficulty = difficulty,
            moveIntervalSeconds = difficulty.startIntervalSeconds,
            bestScoreAtStart = bestScore
        )
    }

    /** Buffers the next turn — applied at the next grid step, not instantly (see [SnakeState]'s doc comment). */
    fun setDirection(state: SnakeState, direction: Direction): SnakeState {
        if (state.phase != GamePhase.RUNNING) return state
        return state.copy(pendingDirection = direction)
    }

    fun tick(state: SnakeState, deltaSeconds: Float, rng: Random): SnakeState {
        if (state.phase != GamePhase.RUNNING) return state
        val elapsed = state.elapsedSinceMoveSeconds + deltaSeconds
        if (elapsed < state.moveIntervalSeconds) {
            return state.copy(elapsedSinceMoveSeconds = elapsed, foodEatenThisStep = false)
        }
        return step(state, elapsed - state.moveIntervalSeconds, rng)
    }

    private fun step(state: SnakeState, remainder: Float, rng: Random): SnakeState {
        // Reject a same-step 180° reversal — it would collide with the snake's own neck instantly.
        val direction = state.pendingDirection
            ?.takeUnless { it.isOppositeOf(state.direction) }
            ?: state.direction

        val head = state.snake.first().moved(direction)
        if (head.x !in 0 until GRID_WIDTH || head.y !in 0 until GRID_HEIGHT) {
            return state.copy(phase = GamePhase.GAME_OVER, direction = direction, pendingDirection = null)
        }

        val ateFood = head == state.food
        val body = if (ateFood) state.snake else state.snake.dropLast(1)
        val newSnake = listOf(head) + body

        if (body.contains(head)) {
            return state.copy(phase = GamePhase.GAME_OVER, direction = direction, pendingDirection = null)
        }

        val newScore = state.score + if (ateFood) SCORE_PER_FOOD else 0
        val foodsEaten = newScore / SCORE_PER_FOOD
        val newInterval = (state.difficulty.startIntervalSeconds - foodsEaten * state.difficulty.speedupPerFood)
            .coerceAtLeast(state.difficulty.minIntervalSeconds)

        return state.copy(
            snake = newSnake,
            direction = direction,
            pendingDirection = null,
            food = if (ateFood) randomFreeCell(newSnake, rng) else state.food,
            score = newScore,
            moveIntervalSeconds = newInterval,
            elapsedSinceMoveSeconds = remainder,
            foodEatenThisStep = ateFood
        )
    }

    private fun randomFreeCell(occupied: List<Cell>, rng: Random): Cell {
        val occupiedSet = occupied.toSet()
        // The grid is small relative to the snake for most of a run, so plain rejection
        // sampling is simplest and fast; fall back to a full scan only if the snake has
        // nearly filled the board (practically unreachable, but keeps this total).
        repeat(200) {
            val candidate = Cell(rng.nextInt(GRID_WIDTH), rng.nextInt(GRID_HEIGHT))
            if (candidate !in occupiedSet) return candidate
        }
        val free = (0 until GRID_WIDTH).flatMap { x -> (0 until GRID_HEIGHT).map { y -> Cell(x, y) } }
            .filterNot { it in occupiedSet }
        return free.randomOrNull(rng) ?: Cell(0, 0)
    }
}
