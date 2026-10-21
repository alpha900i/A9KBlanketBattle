package com.alpha900i.a9kblanketbattle.domain

/**
 * Type of move
 * [SET_KITTEN], [SET_CAT] - new piece is put on a board
 * [PROMOTE_KITTEN] - kitten removed from board and out of game, cat is added to the hand
 * [RETURN_CAT] - cat returns from board to hand
 */
enum class MoveType {
    SET_KITTEN,
    SET_CAT,
    PROMOTE_KITTEN,
    RETURN_CAT
}

/**
 * Move is defined by [moveType] and [row]x[column] cell it was made to
 */
data class Move(
    val row: Int,
    val column: Int,
    val moveType: MoveType
)
