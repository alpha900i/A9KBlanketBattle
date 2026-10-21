package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange

/**
 * Player's pieces
 * [kittenCurrent], [catCurrent] - how much of those pieces player has in hand right now
 * [kittenMax], [catMax] - how much of those pieces player has in hand and on the board together
 */
data class Hand(
    val kittenCurrent: Int,
    val kittenMax: Int,
    val catCurrent: Int,
    val catMax: Int
) {
    /**
     * As a result of a move hand changed; taking change object, applying changes
     */
    fun applyChange(handChange: HandChange): Hand {
        return Hand(
            kittenCurrent = kittenCurrent + handChange.deltaCurrentKitten,
            kittenMax = kittenMax + handChange.deltaMaxKitten,
            catCurrent = catCurrent + handChange.deltaCurrentCat,
            catMax = catMax + handChange.deltaMaxCat,
        )
    }

    /**
     * Hand considered to be in changed state when at least one piece is on the board
     */
    fun changed(): Boolean {
        return kittenCurrent != kittenMax || catCurrent != catMax
    }
}