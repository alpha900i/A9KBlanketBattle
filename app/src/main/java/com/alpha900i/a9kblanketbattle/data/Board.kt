package com.alpha900i.a9kblanketbattle.data

import androidx.compose.foundation.layout.PaddingValues
import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import com.alpha900i.a9kblanketbattle.domain.Player

enum class CellType {
    EMPTY,
    KITTEN,
    CAT;

    fun isPushable(moveType: MoveType): Boolean {
        return when (moveType) {
            MoveType.CAT -> (this == CAT || this == KITTEN)
            MoveType.KITTEN -> (this == KITTEN)
        }
    }
}

data class Cell(
    val type: CellType,
    val owner: Int
)

data class Board(
    val cells: List<List<Cell>>
) {
    fun applyMove(move: Move, playerIndex: Int): Pair<Board, List<HandChange>> {
        val deltaCats = mutableListOf(0, 0)
        val deltaKittens = mutableListOf(0, 0)
        val mutableCells = cells.map { it.toMutableList() }.toMutableList()

        setPieceAndBoop(
            move = move,
            mutableCells = mutableCells,
            deltaCats = deltaCats,
            deltaKittens = deltaKittens,
            playerIndex = playerIndex
        )

        val immutableCells = mutableCells.map { it.toList() }
        val handChanges = deltaKittens.zip(deltaCats) { deltaKitten, deltaCat ->
            HandChange(
                deltaKitten = deltaKitten,
                deltaCat = deltaCat
            )
        }
        return Pair(Board(immutableCells), handChanges)
    }
    private fun setPieceAndBoop(
        move: Move,
        mutableCells: MutableList<MutableList<Cell>>,
        deltaCats: MutableList<Int>,
        deltaKittens: MutableList<Int>,
        playerIndex: Int
    ) {
        val (moveX, moveY, moveType) = move
        val cellDeltas = listOf(
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, 1),
            Pair(0, -1),
            Pair(1, 1),
            Pair(-1, -1),
            Pair(1, -1),
            Pair(-1, 1)
        )

        for ((dx, dy) in cellDeltas) {
            val checkX = moveX + dx
            val checkY = moveY + dy
            val receiverX = moveX + 2 * dx;
            val receiverY = moveY + 2 * dy;
            //is cell in question even in board?
            if (checkX in 0 until mutableCells[0].size && checkY in 0 until mutableCells.size) {
                //is there a pushable piece (so, kitten) in this cell?
                if (mutableCells[checkX][checkY].type.isPushable(moveType)) {
                    //is cell we are getting pushed to on board?
                    if (receiverX in 0 until mutableCells[0].size && receiverY in 0 until mutableCells.size) {
                        //is cell we are getting pushed empty>
                        if (mutableCells[receiverX][receiverY].type == CellType.EMPTY) {
                            mutableCells[receiverX][receiverY] = mutableCells[checkX][checkY];
                            mutableCells[checkX][checkY] = Cell(type = CellType.EMPTY, owner = -1)
                        }
                    } else {
                        //receiver cell is not on board - time to fall
                        if (mutableCells[checkX][checkY].type == CellType.CAT) {
                            deltaCats[mutableCells[checkX][checkY].owner]++
                        }
                        if (mutableCells[checkX][checkY].type == CellType.KITTEN) {
                            deltaKittens[mutableCells[checkX][checkY].owner]++
                        }
                        mutableCells[checkX][checkY] = Cell(type = CellType.EMPTY, owner = -1)
                    }
                }
            }
        }
        val cellType = if (moveType == MoveType.CAT) {
            CellType.CAT
        } else {
            CellType.KITTEN
        }
        mutableCells[moveX][moveY] = Cell(
            type = cellType,
            owner = playerIndex
        )
        if (cellType == CellType.CAT) {
            deltaCats[playerIndex]--
        }
        if (cellType == CellType.KITTEN) {
            deltaKittens[playerIndex]--
        }
    }

    companion object {
        fun emptyBoard(width: Int, height: Int): Board {
            val cells = List(height) {
                List(width) {
                    Cell(CellType.EMPTY, -1)
                }
            }
            return Board(cells)
        }
    }
}