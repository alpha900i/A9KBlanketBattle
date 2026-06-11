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
) {
    companion object {
        fun emptyCell() : Cell {
            return Cell(CellType.EMPTY, -1)
        }
    }
}

data class Board(
    val cells: List<List<Cell>>
) {
    //general move application
    //first it sets pieces and processes possible boops
    //second it removes possible triplets
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
        checkForTriplets(
            mutableCells = mutableCells,
            deltaCats = deltaCats
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

    //setting pieces and booping neighbors
    //for target cell we check all 8 possible directions
    //possible cases are:
    //- next piece isn't pushable (empty or higher value)
    //- two pieces in target direction, no boop happens
    //- one pushable piece in target direction, far from edge - boop happens
    //- one pushable piece in target direction, near the edge - boop happens, piece returns to hand
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
                            mutableCells[checkX][checkY] = Cell.emptyCell()
                        }
                    } else {
                        //receiver cell is not on board - time to fall of board
                        if (mutableCells[checkX][checkY].type == CellType.CAT) {
                            deltaCats[mutableCells[checkX][checkY].owner]++
                        }
                        if (mutableCells[checkX][checkY].type == CellType.KITTEN) {
                            deltaKittens[mutableCells[checkX][checkY].owner]++
                        }
                        mutableCells[checkX][checkY] = Cell.emptyCell()
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

    private fun checkForTriplets(
        mutableCells: MutableList<MutableList<Cell>>,
        deltaCats: MutableList<Int>
    ) {
        val cellDeltas = listOf(
            Pair(1, 0),
            Pair(0, 1),
            Pair(1, 1)
        )
        val foundFlags = mutableListOf(
            false,
            false
        )
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        //get at most one triplet for each player
        //cells become empty
        //kittens turn into cats (so, no delta kittens, delta cats)
        //cats just removed (delta cats)
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                cellDeltas.forEach { cellDelta ->
                    val cellOwner = mutableCells[rowIndex][columnIndex].owner
                    if (isTriplet(
                            mutableCells,
                            rowIndex,
                            columnIndex,
                            cellDelta
                        ) && cellOwner != -1 && !foundFlags[cellOwner]
                    ) {
                        removeTriplet(
                            mutableCells,
                            rowIndex,
                            columnIndex,
                            deltaCats,
                            cellDelta
                        )
                        foundFlags[cellOwner] = true
                    }
                }
            }
        }
    }

    private fun isTriplet(
        mutableCells: MutableList<MutableList<Cell>>,
        rowIndex: Int,
        columnIndex: Int,
        cellDelta: Pair<Int, Int>
    ): Boolean {
        val (dx, dy) = cellDelta
        if (rowIndex + 2 * dx >= mutableCells.size) {
            return false
        }
        if (columnIndex + 2 * dy >= mutableCells[rowIndex].size) {
            return false
        }
        val owner = mutableCells[rowIndex][columnIndex].owner
        (0..2).forEach { shift ->
            val cellOwner = mutableCells[rowIndex + shift * dx][columnIndex + shift * dy].owner
            if (cellOwner == -1 || cellOwner != owner) {
                return false
            }
        }
        return true
    }

    private fun removeTriplet(
        mutableCells: MutableList<MutableList<Cell>>,
        rowIndex: Int,
        columnIndex: Int,
        deltaCats: MutableList<Int>,
        cellDelta: Pair<Int, Int>
    ) {
        val (dx, dy) = cellDelta
        if (rowIndex + 2 * dx >= mutableCells.size) {
            return
        }
        if (columnIndex + 2 * dy >= mutableCells[rowIndex].size) {
            return
        }

        (0..2).forEach { shift ->
            val cellOwner = mutableCells[rowIndex + shift * dx][columnIndex + shift * dy].owner
            deltaCats[cellOwner]++
            mutableCells[rowIndex + shift * dx][columnIndex + shift * dy] = Cell.emptyCell()
        }
    }

    companion object {
        fun emptyBoard(width: Int, height: Int): Board {
            val cells = List(height) {
                List(width) {
                    Cell.emptyCell()
                }
            }
            return Board(cells)
        }
    }
}