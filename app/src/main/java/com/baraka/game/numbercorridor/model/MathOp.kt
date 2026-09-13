package com.baraka.game.numbercorridor.model

/**
 * The arithmetic a squad-shift wall (left lane) teaches, applied to squad size once
 * destroyed — see systems/CollisionSystem.kt.
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
