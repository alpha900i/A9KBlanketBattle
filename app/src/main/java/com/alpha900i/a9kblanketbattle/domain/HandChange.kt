package com.alpha900i.a9kblanketbattle.domain

/**
 * Change in player's hand, happens as result of move
 * each of fields shows what value should be added to respective field in Hand
 */
data class HandChange(
    val deltaCurrentKitten: Int,
    val deltaMaxKitten: Int,
    val deltaCurrentCat: Int,
    val deltaMaxCat: Int
) {
    companion object {
        fun emptyHand(): HandChange {
            return HandChange(
                deltaCurrentKitten = 0,
                deltaMaxKitten = 0,
                deltaCurrentCat = 0,
                deltaMaxCat = 0
            )
        }
    }
}
