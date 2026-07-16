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
            MoveType.SET_CAT -> (this == CAT || this == KITTEN)
            MoveType.SET_KITTEN -> (this == KITTEN)
            else -> false
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
        val handChanges: MutableList<HandChange> = mutableListOf(
            HandChange.emptyHand(),
            HandChange.emptyHand()
        )
        val mutableCells = cells.map { it.toMutableList() }.toMutableList()

        if (move.moveType == MoveType.SET_CAT || move.moveType == MoveType.SET_KITTEN) {
            setPieceAndBoop(
                move = move,
                mutableCells = mutableCells,
                handChanges = handChanges,
                playerIndex = playerIndex
            )
        } else if (move.moveType == MoveType.PROMOTE_KITTEN) {
            ascendKitten(
                move = move,
                mutableCells = mutableCells,
                handChanges = handChanges,
                playerIndex = playerIndex
            )
        } else if (move.moveType == MoveType.RETURN_CAT) {
            returnCat(
                move = move,
                mutableCells = mutableCells,
                handChanges = handChanges,
                playerIndex = playerIndex
            )
        }


        val (gameOver, winnerIndex) = checkForGameOver(
            mutableCells = mutableCells,
            playerIndex = playerIndex
        )

        val deletableTriplets = getDeletableTriplets(
            mutableCells = mutableCells,
            playerIndex = playerIndex
        )


        val immutableCells = mutableCells.map { it.toList() }

        return MoveResult(
            Board(immutableCells),
            handChanges,
            gameOver,
            winnerIndex,
            deletableTriplets
        )
    }

    fun applyRemoval(tripletOnBoard: TripletOnBoard): MoveResult {
        val handChanges: MutableList<HandChange> = mutableListOf(
            HandChange.emptyHand(),
            HandChange.emptyHand()
        )
        val mutableCells = cells.map { it.toMutableList() }.toMutableList()

        removeTriplet(
            mutableCells = mutableCells,
            tripletOnBoard = tripletOnBoard,
            handChanges = handChanges
        )

        val immutableCells = mutableCells.map { it.toList() }
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
        handChanges: MutableList<HandChange>,
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
                        val owner = mutableCells[checkX][checkY].owner
                        if (mutableCells[checkX][checkY].type == CellType.CAT) {
                            handChanges[owner] =
                                handChanges[owner].copy(deltaCurrentCat = handChanges[owner].deltaCurrentCat + 1)
                        }
                        if (mutableCells[checkX][checkY].type == CellType.KITTEN) {
                            handChanges[owner] =
                                handChanges[owner].copy(deltaCurrentKitten = handChanges[owner].deltaCurrentKitten + 1)
                        }
                        mutableCells[checkX][checkY] = Cell.emptyCell()
                    }
                }
            }
        }
        val cellType = if (moveType == MoveType.SET_CAT) {
            CellType.CAT
        } else {
            CellType.KITTEN
        }
        mutableCells[moveX][moveY] = Cell(
            type = cellType,
            owner = playerIndex
        )
        if (cellType == CellType.CAT) {
            handChanges[playerIndex] =
                handChanges[playerIndex].copy(deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat - 1)
        }
        if (cellType == CellType.KITTEN) {
            handChanges[playerIndex] =
                handChanges[playerIndex].copy(deltaCurrentKitten = handChanges[playerIndex].deltaCurrentKitten - 1)
        }
    }


    private fun ascendKitten(
        move: Move,
        mutableCells: MutableList<MutableList<Cell>>,
        handChanges: MutableList<HandChange>,
        playerIndex: Int
    ) {
        assert(
            mutableCells[move.row][move.column].type == CellType.KITTEN && mutableCells[move.row][move.column].owner == playerIndex
        )

        mutableCells[move.row][move.column] = Cell.emptyCell()
        handChanges[playerIndex] = handChanges[playerIndex].copy(
            deltaMaxKitten = handChanges[playerIndex].deltaMaxKitten - 1,
            deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat + 1,
            deltaMaxCat = handChanges[playerIndex].deltaMaxCat + 1,
        )
    }

    private fun returnCat(
        move: Move,
        mutableCells: MutableList<MutableList<Cell>>,
        handChanges: MutableList<HandChange>,
        playerIndex: Int
    ) {
        assert(
            mutableCells[move.row][move.column].type == CellType.CAT && mutableCells[move.row][move.column].owner == playerIndex
        )

        mutableCells[move.row][move.column] = Cell.emptyCell()
        handChanges[playerIndex] = handChanges[playerIndex].copy(
            deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat + 1
        )
    }

    //this method gets us all deletable triplets for current player that exist on board right now
    //triplets are grouped by players
    //triplet is defined by its top-left or bottom-left corner and "shift", leading from this corner to next item
    private fun getDeletableTriplets(
        mutableCells: MutableList<MutableList<Cell>>,
        playerIndex: Int
    ): MutableSet<TripletOnBoard> {
        val result = mutableSetOf<TripletOnBoard>()
        val cellDeltas = listOf(
            Pair(1, 0),
            Pair(0, 1),
            Pair(1, 1),
            Pair(1, -1)
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
        if (rowIndex + 2 * dx >= mutableCells.size || rowIndex + 2 * dx < 0) {
            return false
        }
        if (columnIndex + 2 * dy >= mutableCells[rowIndex].size || columnIndex + 2 * dy < 0) {
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
        handChanges: MutableList<HandChange>
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
            val cell = mutableCells[rowIndex + shift * dx][columnIndex + shift * dy]
            val cellOwner = cell.owner
            val cellType = cell.type

            val kittenTransformed = if (cellType == CellType.KITTEN) 1 else 0
            handChanges[cellOwner] = handChanges[cellOwner].copy(
                deltaMaxKitten = handChanges[cellOwner].deltaMaxKitten - kittenTransformed,
                deltaCurrentCat = handChanges[cellOwner].deltaCurrentCat + 1,
                deltaMaxCat = handChanges[cellOwner].deltaMaxCat + kittenTransformed
            )

            mutableCells[rowIndex + shift * dx][columnIndex + shift * dy] = Cell.emptyCell()
        }
    }

    private fun checkForGameOver(
        mutableCells: MutableList<MutableList<Cell>>,
        playerIndex: Int
    ): Pair<Boolean, Int> {
        val cellDeltas = listOf(
            Pair(1, 0),
            Pair(0, 1),
            Pair(1, 1)
        )
        var catCount: Int = 0
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                if (mutableCells[rowIndex][columnIndex].owner == playerIndex) {
                    cellDeltas.forEach { cellDelta ->
                        if (isCatTriplet(
                                mutableCells,
                                rowIndex,
                                columnIndex,
                                cellDelta
                            )
                        ) {
                            return Pair(true, mutableCells[rowIndex][columnIndex].owner)
                        }
                    }
                    if (mutableCells[rowIndex][columnIndex].type == CellType.CAT) {
                        catCount++
                    }
                }
            }
        }
        return if (catCount == 8) {
            Pair(true, playerIndex)
        } else {
            Pair(false, -1)
        }
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