package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType

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
        fun emptyCell(): Cell {
            return Cell(CellType.EMPTY, -1)
        }
    }
}

data class TripletOnBoard(
    val row: Int,
    val column: Int,
    val rowShift: Int,
    val columnShift: Int
) {
    fun contains(
        r: Int,
        c: Int
    ): Boolean {
        for (i in 0..2) {
            if (row + i * rowShift == r && column + i * columnShift == c) {
                return true
            }
        }
        return false
    }

    fun getCells(): Set<Pair<Int, Int>> =
        (0..2).map { shift ->
            Pair(row + shift * rowShift, column + shift * columnShift)
        }.toSet()
}

data class MoveResult(
    val board: Board,
    val handChanges: List<HandChange>,
    val gameOver: Boolean,
    val winnerIndex: Int,
    val deletableTriplets: Set<TripletOnBoard>
)

data class Board(
    val cells: List<List<Cell>>
) {
    //general move application
    //first it sets pieces and processes possible boops
    //second it removes possible triplets
    fun applyMove(move: Move, playerIndex: Int): MoveResult {
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
        val (gameOver, winnerIndex) = checkForGameOver(
            mutableCells = mutableCells,
        )

        val deletableTriplets = getTriplets(
            mutableCells = mutableCells,
            playerIndex = playerIndex
        )


        val immutableCells = mutableCells.map { it.toList() }
        val handChanges = deltaKittens.zip(deltaCats) { deltaKitten, deltaCat ->
            HandChange(
                deltaKitten = deltaKitten,
                deltaCat = deltaCat
            )
        }
        return MoveResult(
            Board(immutableCells),
            handChanges,
            gameOver,
            winnerIndex,
            deletableTriplets
        )
    }

    fun applyRemoval(tripletOnBoard: TripletOnBoard): MoveResult {
        val deltaCats = mutableListOf(0, 0)
        val deltaKittens = mutableListOf(0, 0)
        val mutableCells = cells.map { it.toMutableList() }.toMutableList()

        removeTriplet(
            mutableCells = mutableCells,
            tripletOnBoard = tripletOnBoard,
            deltaCats = deltaCats
        )

        val immutableCells = mutableCells.map { it.toList() }
        val handChanges = deltaKittens.zip(deltaCats) { deltaKitten, deltaCat ->
            HandChange(
                deltaKitten = deltaKitten,
                deltaCat = deltaCat
            )
        }
        return MoveResult(
            Board(immutableCells),
            handChanges,
            false,           //we wouldn't get here if there was an active gameover, and triplet removal can't initiate one
            -1,
            setOf()     //no removal after removal
        )
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

    //this method gets us all deletable triplets for current player that exist on board right now
    //triplets are grouped by players
    //triplet is defined by its top-left corner and "shift", leading from this corner to next item
    private fun getTriplets(
        mutableCells: MutableList<MutableList<Cell>>,
        playerIndex: Int
    ): MutableSet<TripletOnBoard> {
        val result = mutableSetOf<TripletOnBoard>()
        val cellDeltas = listOf(
            Pair(1, 0),
            Pair(0, 1),
            Pair(1, 1)
        )
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                cellDeltas.forEach { cellDelta ->
                    val cellOwner = mutableCells[rowIndex][columnIndex].owner
                    if (isTriplet(
                            mutableCells,
                            rowIndex,
                            columnIndex,
                            cellDelta
                        ) && cellOwner == playerIndex
                    ) {
                        result.add(
                            TripletOnBoard(
                                row = rowIndex,
                                column = columnIndex,
                                rowShift = cellDelta.first,
                                columnShift = cellDelta.second
                            )
                        )
                    }
                }
            }
        }
        return result
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


    //cells become empty
    //kittens turn into cats (so, no delta kittens, delta cats)
    //cats just removed (delta cats)
    private fun removeTriplet(
        mutableCells: MutableList<MutableList<Cell>>,
        tripletOnBoard: TripletOnBoard?,
        deltaCats: MutableList<Int>
    ) {
        if (tripletOnBoard == null) {
            return
        }
        val rowIndex = tripletOnBoard.row
        val columnIndex = tripletOnBoard.column
        val dx = tripletOnBoard.rowShift
        val dy = tripletOnBoard.columnShift
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

    private fun checkForGameOver(
        mutableCells: MutableList<MutableList<Cell>>,
    ): Pair<Boolean, Int> {
        val cellDeltas = listOf(
            Pair(1, 0),
            Pair(0, 1),
            Pair(1, 1)
        )
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                cellDeltas.forEach { cellDelta ->
                    val cellOwner = mutableCells[rowIndex][columnIndex].owner
                    if (isCatTriplet(
                            mutableCells,
                            rowIndex,
                            columnIndex,
                            cellDelta
                        )
                    ) {
                        return Pair(true, cellOwner)
                    }
                }
            }
        }
        return Pair(false, -1)
    }

    private fun isCatTriplet(
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
            val cellType = mutableCells[rowIndex + shift * dx][columnIndex + shift * dy].type
            if (cellOwner == -1 || cellOwner != owner || cellType != CellType.CAT) {
                return false
            }
        }
        return true
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