package com.alpha900i.a9kblanketbattle.domain

enum class MoveType {
    KITTEN,
    CAT
}
data class Move(
    val row: Int,
    val column: Int,
    val moveType: MoveType
)
