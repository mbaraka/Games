package com.baraka.game.clashdecks.model

/** Which combatant a visual effect plays over. */
enum class VfxSide { PLAYER, CPU }

/**
 * A visual beat the UI should animate — emitted by [com.baraka.game.clashdecks.systems.DuelEngine]
 * alongside [DuelSound]s and log lines, drained by the ViewModel, and played by the duel
 * screen's effect layer (floating numbers, hit flashes, status pops, screen tints).
 *
 * Purely cosmetic: nothing here changes the rules, so tests ignore it.
 */
sealed interface DuelVfx {
    val side: VfxSide

    /** [toHp] reached health, [blocked] was absorbed by Block. */
    data class Hit(override val side: VfxSide, val toHp: Int, val blocked: Int) : DuelVfx
    data class Heal(override val side: VfxSide, val amount: Int) : DuelVfx
    data class BlockGain(override val side: VfxSide, val amount: Int) : DuelVfx
    data class StatusApplied(override val side: VfxSide, val kind: StatusKind) : DuelVfx
    data class BlockShattered(override val side: VfxSide, val amount: Int) : DuelVfx
    data class Frozen(override val side: VfxSide) : DuelVfx

    /** [side] is whoever played the card. */
    data class CardPlayed(override val side: VfxSide, val type: CardType) : DuelVfx
}
