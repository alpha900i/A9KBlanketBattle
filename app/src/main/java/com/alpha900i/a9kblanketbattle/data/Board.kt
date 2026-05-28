package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType

enum class CellType {
    EMPTY,
    KITTEN,
    CAT
}

data class Cell(
    val type: CellType,
    val owner: Int
)

data class Board(
    val cells: List<List<Cell>>
) {
    fun applyMove(move: Move, playerIndex: Int): Pair<Board, HandChange> {
        val newCells = cells.mapIndexed { r, oldRow ->
            if (r == move.row) {
                oldRow.mapIndexed { c, old ->
                    if (c == move.column) {
                        when (move.moveType) {
                            MoveType.KITTEN -> Cell(
                                type =CellType.KITTEN,
                                owner = playerIndex
                            )
                            MoveType.CAT -> Cell(
                                type = CellType.CAT,
                                owner = playerIndex
                            )
                        }
                    } else
                        old
                }
            } else {
                oldRow
            }
        }

        val handChange = HandChange(
            deltaKitten = if (move.moveType == MoveType.KITTEN) -1 else 0,
            deltaCat = if (move.moveType == MoveType.CAT) -1 else 0,
        )

        return Pair(
            Board(newCells),
            handChange
        )
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