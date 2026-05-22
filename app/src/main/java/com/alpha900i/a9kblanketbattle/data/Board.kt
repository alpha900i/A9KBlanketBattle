package com.alpha900i.a9kblanketbattle.data

enum class Cell {
    EMPTY,
    KITTEN,
    CAT
}
class Board (
    private val width: Int,
    private val height: Int
) {
    public var board: Array<Array<Cell>> = Array(height) { Array(width) { Cell.EMPTY } }
    init {
        modify()
    }
    public fun modify() {
        board[0][2] = Cell.CAT
        board[1][1] = Cell.KITTEN
    }
}
