package com.baraka.game.numbercorridor.model

/**
 * The arithmetic a buff wall teaches. Applied to whichever [BuffTarget] the wall carries
 * when the player shoots it down — see systems/CollisionSystem.kt.
 */
enum class MathOp(val symbol: String) {
    ADD("+") {
        override fun apply(current: Float, value: Float) = current + value
    },
    SUBTRACT("-") {
        override fun apply(current: Float, value: Float) = current - value
    },
    MULTIPLY("x") {
        override fun apply(current: Float, value: Float) = current * value
    },
    DIVIDE("/") {
        override fun apply(current: Float, value: Float) = current / value
    };

    abstract fun apply(current: Float, value: Float): Float
}

/** Which of the player's stats a buff wall's [MathOp] is applied to. */
enum class BuffTarget(val displayName: String) {
    DAMAGE("DMG"),
    FIRE_RATE("RATE"),
    SQUAD("SQUAD")
}
