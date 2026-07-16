package com.alpha900i.a9kblanketbattle.domain

enum class MoveType {
    SET_KITTEN,
    SET_CAT,
    PROMOTE_KITTEN,
    RETURN_CAT
}
data class Move(
    val row: Int,
    val column: Int,
    val moveType: MoveType
)
