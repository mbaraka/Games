package com.baraka.game.clashdecks.model

import com.baraka.game.knightsurvivor.model.VectorIcon

/**
 * A status that sits on a [Duelist] for a number of *turns* (not milliseconds — this game
 * is turn-based). Every persistent tactical effect is one of these; instant effects
 * (raw damage, block, heal, shatter) are [Effect]s that resolve and are gone.
 *
 * [isDebuff] is what `Cleanse` keys off of, and what the UI tints red vs green.
 */
enum class StatusKind(
    val displayName: String,
    val description: String,
    val isDebuff: Boolean,
    val icon: VectorIcon,
    val colorArgb: Long,
    /** A short in-chip label (StatusRow in ClashComponents.kt) — the icon alone wasn't enough to tell statuses apart at a glance (Empower/Vulnerable are both small stars, same size, easy to confuse), so every status chip now also spells out what it is. */
    val shortLabel: String
) {
    STUN(
        "Stun",
        "Skips your next turn's card plays. You still draw and gain energy.",
        isDebuff = true,
        icon = VectorIcon.Bolt,
        colorArgb = 0xFFFFEE58,
        shortLabel = "Stun"
    ),
    FREEZE(
        "Freeze",
        "Skips your entire next turn — no draw, no energy, no plays.",
        isDebuff = true,
        icon = VectorIcon.Diamond(1f),
        colorArgb = 0xFF4FC3F7,
        shortLabel = "Frozen"
    ),
    ROOT(
        "Root",
        "You cannot play Defense cards on your next turn.",
        isDebuff = true,
        icon = VectorIcon.TurtleShell,
        colorArgb = 0xFF8D6E63,
        shortLabel = "Rooted"
    ),
    WEAKEN(
        "Weaken",
        "Your Attack cards deal reduced damage while this lasts.",
        isDebuff = true,
        icon = VectorIcon.Axe(1.2f),
        colorArgb = 0xFF90A4AE,
        shortLabel = "Weak"
    ),
    VULNERABLE(
        "Vulnerable",
        "You take extra damage from every hit while this lasts.",
        isDebuff = true,
        icon = VectorIcon.Star(5, 0.5f),
        colorArgb = 0xFFEF5350,
        shortLabel = "Vuln"
    ),
    POISON(
        "Poison",
        "Lose HP at the start of each of your turns. Ignores Block.",
        isDebuff = true,
        icon = VectorIcon.Slime,
        colorArgb = 0xFF9CCC65,
        shortLabel = "Poison"
    ),
    EMPOWER(
        "Empower",
        "Your Attack cards deal bonus damage while this lasts.",
        isDebuff = false,
        icon = VectorIcon.Star(4, 0.5f),
        colorArgb = 0xFFFFB300,
        shortLabel = "Power+"
    ),
    REGEN(
        "Regen",
        "Heal HP at the start of each of your turns while this lasts.",
        isDebuff = false,
        icon = VectorIcon.Heart,
        colorArgb = 0xFF66BB6A,
        shortLabel = "Regen"
    ),
    /** Consumed automatically (see DuelEngine's `Effect.ApplyStatus` branch) the next time Stun, Freeze or Root would land on its holder — a shield against losing a turn, not against damage. */
    WARDED(
        "Warded",
        "Blocks the next Stun, Freeze or Root applied to you, then is used up.",
        isDebuff = false,
        icon = VectorIcon.Shield,
        colorArgb = 0xFF4DD0E1,
        shortLabel = "Ward"
    );
}
