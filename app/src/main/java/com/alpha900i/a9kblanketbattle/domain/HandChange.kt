package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.Board
import com.alpha900i.a9kblanketbattle.data.Cell

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
