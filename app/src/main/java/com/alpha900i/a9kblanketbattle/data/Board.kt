package com.alpha900i.a9kblanketbattle.data

enum class Cell {
    EMPTY,
    KITTEN,
    CAT
}

data class Board(
    val cells: List<List<Cell>>
) {
    companion object {
        fun emptyBoard(width: Int, height: Int): Board {
            val cells = List(height) {
                List(width) {
                    Cell.EMPTY
                }
            }
            return Board(cells)
        }
    }
}