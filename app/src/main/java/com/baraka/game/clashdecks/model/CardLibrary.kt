package com.baraka.game.clashdecks.model

import com.baraka.game.clashdecks.model.EffectTarget.OPPONENT
import com.baraka.game.clashdecks.model.EffectTarget.SELF
import com.baraka.game.knightsurvivor.model.VectorIcon
import kotlin.random.Random

/**
 * The whole card pool for v1 — 20 cards, 7 Attack / 6 Defense / 7 Support. Both duelists
 * draw from an independent shuffled copy of this list (no deck building yet). Every tuning
 * number the game has lives here and in `systems/DuelEngine.kt`.
 */
object CardLibrary {

    val all: List<Card> = listOf(
        // ---------------- Attack ----------------
        Card(
            "strike", "Strike", CardType.ATTACK, cost = 1, rarity = Rarity.COMMON,
            effects = listOf(Effect.Damage(2)),
            icon = VectorIcon.Blade(1.5f), soundHz = 1400.0,
            flavor = "The first thing anyone learns."
        ),
        Card(
            "pommel_strike", "Pommel Strike", CardType.ATTACK, cost = 2, rarity = Rarity.COMMON,
            effects = listOf(Effect.Damage(2), Effect.ApplyStatus(StatusKind.STUN, 0, 1, OPPONENT)),
            icon = VectorIcon.Blade(1.8f), soundHz = 1150.0,
            flavor = "Hilt to the temple. Rude, effective."
        ),
        Card(
            "heavy_blow", "Heavy Blow", CardType.ATTACK, cost = 3, rarity = Rarity.COMMON,
            effects = listOf(Effect.Damage(5)),
            icon = VectorIcon.Axe(1.6f), soundHz = 240.0,
            flavor = "Wind up, commit, follow through."
        ),
        Card(
            "venom_blade", "Venom Blade", CardType.ATTACK, cost = 3, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.Damage(2), Effect.ApplyStatus(StatusKind.POISON, 2, 3, OPPONENT)),
            icon = VectorIcon.Blade(1.4f), soundHz = 900.0,
            flavor = "The cut is the smaller problem."
        ),
        Card(
            "frost_nova", "Frost Nova", CardType.ATTACK, cost = 5, rarity = Rarity.RARE,
            effects = listOf(Effect.Damage(3), Effect.ApplyStatus(StatusKind.FREEZE, 0, 1, OPPONENT)),
            icon = VectorIcon.Diamond(1f), soundHz = 520.0,
            flavor = "A whole turn spent shivering."
        ),
        Card(
            "hamstring", "Hamstring", CardType.ATTACK, cost = 2, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.Damage(1), Effect.ApplyStatus(StatusKind.ROOT, 0, 1, OPPONENT)),
            icon = VectorIcon.Boomerang, soundHz = 700.0,
            flavor = "Can't raise a shield if you can't stand."
        ),
        Card(
            "sunder", "Sunder", CardType.ATTACK, cost = 4, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.Shatter(bonusDamage = 4)),
            icon = VectorIcon.Axe(1.8f), soundHz = 300.0,
            flavor = "Break the wall, then break what's behind it."
        ),

        // ---------------- Defense ----------------
        Card(
            "guard", "Guard", CardType.DEFENSE, cost = 1, rarity = Rarity.COMMON,
            effects = listOf(Effect.Block(3)),
            icon = VectorIcon.Shield, soundHz = 320.0,
            flavor = "Basic, but it keeps you alive."
        ),
        Card(
            "iron_wall", "Iron Wall", CardType.DEFENSE, cost = 3, rarity = Rarity.COMMON,
            effects = listOf(Effect.Block(8)),
            icon = VectorIcon.Shield, soundHz = 290.0,
            flavor = "Nothing gets through today."
        ),
        Card(
            "bulwark", "Bulwark", CardType.DEFENSE, cost = 4, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.Block(12)),
            icon = VectorIcon.TurtleShell, soundHz = 250.0,
            flavor = "Set your feet and wait it out."
        ),
        Card(
            "riposte", "Riposte", CardType.DEFENSE, cost = 2, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.Block(3), Effect.ReflectNextHit(2)),
            icon = VectorIcon.Blade(1.2f), soundHz = 480.0,
            flavor = "Your swing pays for mine."
        ),
        Card(
            "bracing_stance", "Bracing Stance", CardType.DEFENSE, cost = 0, rarity = Rarity.COMMON,
            effects = listOf(Effect.Block(2), Effect.DrawCards(1)),
            icon = VectorIcon.Shield, soundHz = 360.0,
            flavor = "A breath, a look around, a plan."
        ),
        Card(
            "barrier", "Barrier", CardType.DEFENSE, cost = 2, rarity = Rarity.COMMON,
            effects = listOf(Effect.Block(5), Effect.Heal(1)),
            icon = VectorIcon.Shield, soundHz = 340.0,
            flavor = "Hold the line, catch your breath."
        ),

        // ---------------- Support ----------------
        Card(
            "mend", "Mend", CardType.SUPPORT, cost = 2, rarity = Rarity.COMMON,
            effects = listOf(Effect.Heal(4)),
            icon = VectorIcon.Heart, soundHz = 660.0,
            flavor = "Stitch it, spit on it, move on."
        ),
        Card(
            "second_wind", "Second Wind", CardType.SUPPORT, cost = 3, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.Heal(3), Effect.ApplyStatus(StatusKind.REGEN, 2, 2, SELF)),
            icon = VectorIcon.Heart, soundHz = 620.0,
            flavor = "The fight's not over. Neither are you."
        ),
        Card(
            "insight", "Insight", CardType.SUPPORT, cost = 1, rarity = Rarity.COMMON,
            effects = listOf(Effect.DrawCards(2)),
            icon = VectorIcon.Star(6, 0.5f), soundHz = 900.0,
            flavor = "More options is more answers."
        ),
        Card(
            "focus", "Focus", CardType.SUPPORT, cost = 0, rarity = Rarity.COMMON,
            effects = listOf(Effect.GainEnergy(2)),
            icon = VectorIcon.Star(5, 0.5f), soundHz = 1000.0,
            flavor = "Everything slows down for a second."
        ),
        Card(
            "war_horn", "War Horn", CardType.SUPPORT, cost = 1, rarity = Rarity.UNCOMMON,
            effects = listOf(Effect.ApplyStatus(StatusKind.EMPOWER, 3, 3, SELF)),
            icon = VectorIcon.Star(4, 0.5f), soundHz = 480.0,
            flavor = "The sound alone hits harder."
        ),
        Card(
            "curse", "Curse", CardType.SUPPORT, cost = 2, rarity = Rarity.UNCOMMON,
            effects = listOf(
                Effect.ApplyStatus(StatusKind.WEAKEN, 2, 2, OPPONENT),
                Effect.ApplyStatus(StatusKind.VULNERABLE, 2, 2, OPPONENT)
            ),
            icon = VectorIcon.Slime, soundHz = 300.0,
            flavor = "Softer, and easier to hurt."
        ),
        Card(
            "cleanse", "Cleanse", CardType.SUPPORT, cost = 1, rarity = Rarity.COMMON,
            effects = listOf(Effect.Cleanse(SELF)),
            icon = VectorIcon.Heart, soundHz = 820.0,
            flavor = "Shake it off. Literally."
        ),
    )

    init {
        require(all.map { it.id }.toSet().size == all.size) { "duplicate card id in CardLibrary" }
    }

    fun byId(id: String): Card? = all.firstOrNull { it.id == id }

    /** A fresh shuffled draw pile — one per duelist per duel. */
    fun buildDeck(rng: Random): List<Card> = all.shuffled(rng)
}
