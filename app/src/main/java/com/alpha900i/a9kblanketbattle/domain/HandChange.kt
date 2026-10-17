package com.alpha900i.a9kblanketbattle.domain

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
