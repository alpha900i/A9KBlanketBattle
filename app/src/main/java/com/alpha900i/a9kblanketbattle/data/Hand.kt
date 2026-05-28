package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange

data class Hand(
    val kittenCurrent: Int,
    val kittenMax: Int,
    val catCurrent: Int,
    val catMax: Int
) {
    fun applyChange(handChange: HandChange): Hand {
        return Hand(
            kittenCurrent = kittenCurrent + handChange.deltaKitten,
            kittenMax = kittenMax,
            catCurrent = catCurrent + handChange.deltaCat,
            catMax = catMax,
        )
    }
}