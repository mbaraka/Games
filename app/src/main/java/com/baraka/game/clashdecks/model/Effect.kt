package com.baraka.game.clashdecks.model

/** Who an [Effect] lands on, relative to the duelist playing the card. */
enum class EffectTarget { SELF, OPPONENT }

/** What a [Effect.ScalingDamage] card scales its bonus damage off of — read at resolution time, since none of these are known when the card is printed. */
enum class ScalingSource(val describeSuffix: String) {
    /** Rewards stacking debuffs onto the enemy before finishing them off. */
    ENEMY_DEBUFFS("per enemy debuff"),
    /** A desperation finisher: the lower your own HP, the harder it hits. */
    SELF_MISSING_HP_TENTHS("per 10% HP you're missing"),
    /** A slow-burn bomb: weak early, strong if the duel drags on. */
    ROUND_NUMBER("per round")
}

/**
 * A single atomic thing a card does. A [Card] carries a list of these and
 * [com.baraka.game.clashdecks.systems.DuelEngine] resolves them in order. Adding a new
 * kind of behavior = one entry here + one branch in `DuelEngine.applyEffect`.
 *
 * Implicit targeting: [Damage]/[ScalingDamage] always hit the opponent, [Block]/[Heal]/
 * [DrawCards]/[GainEnergy]/[ReflectNextHit] always affect the caster. [ApplyStatus] and
 * [Cleanse] spell out their target since they go both ways.
 */
sealed interface Effect {
    data class Damage(val amount: Int) : Effect

    /** [base] always applies; [perStack] is added once per unit of [source], computed when the card resolves. */
    data class ScalingDamage(val base: Int, val perStack: Int, val source: ScalingSource) : Effect

    data class Block(val amount: Int) : Effect
    data class Heal(val amount: Int) : Effect
    data class DrawCards(val count: Int) : Effect
    data class GainEnergy(val amount: Int) : Effect
    data class ApplyStatus(
        val kind: StatusKind,
        val amount: Int,
        val turns: Int,
        val target: EffectTarget
    ) : Effect

    /** Remove every debuff from [target]. */
    data class Cleanse(val target: EffectTarget = EffectTarget.SELF) : Effect

    /** Destroy the opponent's current Block, then deal [bonusDamage] on top. */
    data class Shatter(val bonusDamage: Int) : Effect

    /** The next hit that reaches the caster's HP also deals [amount] back to the attacker. */
    data class ReflectNextHit(val amount: Int) : Effect
}

/** One-line human-readable description, used in the hand and the Codex. */
fun Effect.describe(): String = when (this) {
    is Effect.Damage -> "Deal $amount"
    is Effect.ScalingDamage -> {
        val baseText = if (base > 0) "$base + " else ""
        "Deal $baseText$perStack ${source.describeSuffix}"
    }
    is Effect.Block -> "Block $amount"
    is Effect.Heal -> "Heal $amount"
    is Effect.DrawCards -> "Draw $count"
    is Effect.GainEnergy -> "+$amount energy"
    is Effect.ApplyStatus -> {
        val who = if (target == EffectTarget.SELF) "self" else "enemy"
        val mag = if (kind.showsAmount) " $amount" else ""
        "${kind.displayName}$mag ($turns) → $who"
    }
    is Effect.Cleanse -> "Cleanse ${if (target == EffectTarget.SELF) "self" else "enemy"}"
    is Effect.Shatter -> "Shatter enemy Block + $bonusDamage"
    is Effect.ReflectNextHit -> "Reflect $amount"
}

/** Whether a status's numeric magnitude is meaningful to show (poison "2" yes, stun no). */
val StatusKind.showsAmount: Boolean
    get() = when (this) {
        StatusKind.STUN, StatusKind.FREEZE, StatusKind.ROOT, StatusKind.WARDED -> false
        StatusKind.WEAKEN, StatusKind.VULNERABLE, StatusKind.POISON,
        StatusKind.EMPOWER, StatusKind.REGEN -> true
    }
