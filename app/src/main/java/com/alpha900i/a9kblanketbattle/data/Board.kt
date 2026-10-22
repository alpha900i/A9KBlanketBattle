package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType

/**
 * Type of single cell
 * [isPushable] answers, whether current piece can be pushed by piece of given MoveType
 */
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

/**
 * Single cell has a [type] and number of its [owner]
 * Empty cells have [owner] -1, although it never used for them
 */
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

/**
 * Three "interesting" pieces on a board in a row.
 * In practice they are always of same owner, always non-empty, but can contain different pieces
 * [row] and [column] are row and column of starting cell
 * [rowShift] and [columnShift] show delta to next cell in triplet
 * So, triplet consists of cells
 * (row;column)
 * (row+rowShift;column+columnShift)
 * (row+2*rowShift;column+2*columnShift)
 * [contains] checks whether cell [r]x[c] is in the triplet
 * [getCells] returns Set of all possible cells in triplet
 */
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

/**
 * Result of single move
 * [board] is updated states of board
 * [pendingEffects] is pieces' changes during that move that needs visualization (movements, appearings, disappearings)
 * [handChanges] is 2-element array that describes how contents of player's hand changed after turn
 * [gameOver] is "is game over after this turn"
 * [winnerIndex] is "number of player that won". Makes sense only paired with [gameOver] = true
 * [deletableTriplets] is set of triplets that player can remove from board; player may need to decide, which one to remove
 */
data class MoveResult(
    val board: Board,
    val pendingEffects: List<VisualEffect>,
    val handChanges: List<HandChange>,
    val gameOver: Boolean,
    val winnerIndex: Int,
    val deletableTriplets: Set<TripletOnBoard>
)

/**
 * Rectangular grid of cells
 * [getHeight] and [getWidth] return respective dimension
 * [getEmptyCellCoordinates] returns list of coordinates of indices for empty cells
 * [getPlayerCellCoordinates] returns list of coordinates of indices for cells belonging to given player
 * [getCellType] returns type of cell in given row and column
 * [applyMove] takes move description; returns result of this move application on given board
 * [applyRemoval] takes triplet, selected for removal; returns result of this removal for given board
 */
data class Board(
    val cells: List<List<Cell>>
) {
    fun getHeight(): Int = cells.size
    fun getWidth(): Int = cells[0].size
    fun getEmptyCellCoordinates(): List<Pair<Int, Int>> =
        cells.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, cell ->
                if (cell.type == CellType.EMPTY) {
                    rowIndex to colIndex
                } else {
                    null
                }
            }
        }
    fun getPlayerCellCoordinates(index: Int): List<Pair<Int, Int>> =
        cells.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, cell ->
                if (cell.type != CellType.EMPTY && cell.owner == index) {
                    rowIndex to colIndex
                } else {
                    null
                }
            }
        }
    fun getCellType(rowIndex: Int, colIndex: Int): CellType = cells[rowIndex][colIndex].type

    /**
     * General move application
     * First it implements move, depending on its type
     * Second it removes possible triplets, if no choice required
     * Returns MoveResult - set of data describing new board and other game changes
    */
    fun applyMove(move: Move, playerIndex: Int, maxPieceAmount: Int): MoveResult {
        val handChanges: MutableList<HandChange> = mutableListOf(
            HandChange.emptyHand(),
            HandChange.emptyHand()
        )
        val mutableCells = cells.map { it.toMutableList() }.toMutableList()
        var pendingEffects: List<VisualEffect> = listOf()

        when (move.moveType) {
            MoveType.SET_CAT, MoveType.SET_KITTEN -> {
                pendingEffects = setPieceAndBoop(
                    move = move,
                    mutableCells = mutableCells,
                    handChanges = handChanges,
                    playerIndex = playerIndex
                )
            }

            MoveType.PROMOTE_KITTEN -> {
                pendingEffects = ascendKitten(
                    move = move,
                    mutableCells = mutableCells,
                    handChanges = handChanges,
                    playerIndex = playerIndex
                )
            }

            MoveType.RETURN_CAT -> {
                pendingEffects = returnCat(
                    move = move,
                    mutableCells = mutableCells,
                    handChanges = handChanges,
                    playerIndex = playerIndex
                )
            }
        }


        val (gameOver, winnerIndex) = checkForGameOver(
            mutableCells = mutableCells,
            playerIndex = playerIndex,
            maxPieceAmount = maxPieceAmount
        )

        val deletableTriplets = getDeletableTriplets(
            mutableCells = mutableCells,
            playerIndex = playerIndex
        )

        //if we have one triplet to delete - we autodelete it here
        //only exception - winning triplet. We don't want to delete it
        if (deletableTriplets.size == 1 && !gameOver) {
            val removalEffect: List<VisualEffect> = removeTriplet(
                mutableCells = mutableCells,
                tripletOnBoard = deletableTriplets.first(),
                handChanges = handChanges
            )
            pendingEffects = pendingEffects + removalEffect
            deletableTriplets.clear()
        }
        val immutableCells = mutableCells.map { it.toList() }

        return MoveResult(
            Board(immutableCells),
            pendingEffects = pendingEffects,
            handChanges,
            gameOver,
            winnerIndex,
            deletableTriplets
        )
    }


    /**
     * General removal application
     * First it implements move, depending on its type
     * Second it removes possible triplets, if no choice required
     * Returns MoveResult - set of data describing new board and other game changes
     */
    fun applyRemoval(tripletOnBoard: TripletOnBoard): MoveResult {
        val handChanges: MutableList<HandChange> = mutableListOf(
            HandChange.emptyHand(),
            HandChange.emptyHand()
        )
        val mutableCells = cells.map { it.toMutableList() }.toMutableList()

        val pendingEffects: List<VisualEffect> = removeTriplet(
            mutableCells = mutableCells,
            tripletOnBoard = tripletOnBoard,
            handChanges = handChanges
        )

        val immutableCells = mutableCells.map { it.toList() }
        return MoveResult(
            Board(immutableCells),
            pendingEffects = pendingEffects,
            handChanges,
            false,           //we wouldn't get here if there was an active "game over", and triplet removal can't initiate one
            -1,
            setOf()     //no removal after removal
        )
    }

    /**
     * Setting pieces and booping neighbors
     * for target cell we check all 8 possible directions
     * possible cases are:
     * - next piece isn't pushable (empty or higher value)
     * - two pieces in target direction, no boop happens
     * - one pushable piece in target direction, far from edge - boop happens
     * - one pushable piece in target direction, near the edge - boop happens, piece returns to hand
     */
    private fun setPieceAndBoop(
        move: Move,
        mutableCells: MutableList<MutableList<Cell>>,
        handChanges: MutableList<HandChange>,
        playerIndex: Int
    ): List<VisualEffect> {
        val (moveX, moveY, moveType) = move
        val pendingEffects: MutableList<VisualEffect> = mutableListOf()

        for ((dx, dy) in cellDeltas8directions) {
            val checkX = moveX + dx
            val checkY = moveY + dy
            val receiverX = moveX + 2 * dx
            val receiverY = moveY + 2 * dy
            //is cell in question even in board?
            if (checkX in 0 until getHeight() && checkY in 0 until getWidth()) {
                //is there a pushable piece (so, kitten) in this cell?
                if (mutableCells[checkX][checkY].type.isPushable(moveType)) {
                    //is cell we are getting pushed to on board?
                    if (receiverX in 0 until getHeight() && receiverY in 0 until getWidth()) {
                        //is cell we are getting pushed empty>
                        if (mutableCells[receiverX][receiverY].type == CellType.EMPTY) {
                            mutableCells[receiverX][receiverY] = mutableCells[checkX][checkY]
                            mutableCells[checkX][checkY] = Cell.emptyCell()
                            pendingEffects.add(
                                VisualEffect.MovePiece(
                                    fromRow = checkX,
                                    fromColumn = checkY,
                                    toRow = receiverX,
                                    toColumn = receiverY,
                                    owner = mutableCells[receiverX][receiverY].owner,
                                    type = mutableCells[receiverX][receiverY].type
                                )
                            )
                        }
                    } else {
                        //receiver cell is not on board - time to fall of board
                        val cellOwner = mutableCells[checkX][checkY].owner
                        val cellType = mutableCells[checkX][checkY].type
                        if (cellType == CellType.CAT) {
                            handChanges[cellOwner] =
                                handChanges[cellOwner].copy(deltaCurrentCat = handChanges[cellOwner].deltaCurrentCat + 1)
                        }
                        if (cellType == CellType.KITTEN) {
                            handChanges[cellOwner] =
                                handChanges[cellOwner].copy(deltaCurrentKitten = handChanges[cellOwner].deltaCurrentKitten + 1)
                        }
                        mutableCells[checkX][checkY] = Cell.emptyCell()
                        pendingEffects.add(
                            VisualEffect.RemovePiece(
                                row = checkX,
                                column = checkY,
                                owner = cellOwner,
                                type = cellType
                            )
                        )
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

        pendingEffects.add(
            VisualEffect.AddPiece(
                row = moveX,
                column = moveY,
                owner = playerIndex,
                type = cellType
            )
        )

        return pendingEffects
    }


    /**
     * Removes kitten from the board, adds cat to the hand
     */
    private fun ascendKitten(
        move: Move,
        mutableCells: MutableList<MutableList<Cell>>,
        handChanges: MutableList<HandChange>,
        playerIndex: Int
    ): List<VisualEffect> {
        assert(
            mutableCells[move.row][move.column].type == CellType.KITTEN && mutableCells[move.row][move.column].owner == playerIndex
        )

        mutableCells[move.row][move.column] = Cell.emptyCell()
        handChanges[playerIndex] = handChanges[playerIndex].copy(
            deltaMaxKitten = handChanges[playerIndex].deltaMaxKitten - 1,
            deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat + 1,
            deltaMaxCat = handChanges[playerIndex].deltaMaxCat + 1,
        )
        return listOf(
            VisualEffect.RemovePiece(
                row = move.row,
                column = move.column,
                owner = playerIndex,
                type = CellType.KITTEN
            )
        )
    }


    /**
     * Removes cat from the board to the hand
     */
    private fun returnCat(
        move: Move,
        mutableCells: MutableList<MutableList<Cell>>,
        handChanges: MutableList<HandChange>,
        playerIndex: Int
    ): List<VisualEffect> {
        assert(
            mutableCells[move.row][move.column].type == CellType.CAT && mutableCells[move.row][move.column].owner == playerIndex
        )

        mutableCells[move.row][move.column] = Cell.emptyCell()
        handChanges[playerIndex] = handChanges[playerIndex].copy(
            deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat + 1
        )

        return listOf(
            VisualEffect.RemovePiece(
                row = move.row,
                column = move.column,
                owner = playerIndex,
                type = CellType.CAT
            )
        )
    }

    /**
     * All deletable triplets for current player that exist on board right now
     * Triplet is defined by its top-left or bottom-left corner and "shift", leading from this corner to next item
     */
    private fun getDeletableTriplets(
        mutableCells: MutableList<MutableList<Cell>>,
        playerIndex: Int
    ): MutableSet<TripletOnBoard> {
        val result = mutableSetOf<TripletOnBoard>()
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                cellDeltas4directions.forEach { cellDelta ->
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

    /**
     * Are there three cells from this [rowIndex]x[columnIndex] start in [cellDelta] defined direction
     * that belong to same non-fake player?
     */
    private fun isTriplet(
        mutableCells: MutableList<MutableList<Cell>>,
        rowIndex: Int,
        columnIndex: Int,
        cellDelta: Pair<Int, Int>
    ): Boolean {
        val (dx, dy) = cellDelta
        val furthestRowIndex = rowIndex + 2 * dx
        val furthestColumnIndex = columnIndex + 2 * dy
        if (furthestRowIndex >= mutableCells.size || furthestRowIndex < 0) {
            return false
        }
        if (furthestColumnIndex >= mutableCells[rowIndex].size || furthestColumnIndex < 0) {
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


    /**
     * Normal triplet removal
     * - clean cells
     * - each removed kitten returns to hand as cat
     * - each removed cat returns to hand as cat
     */
    private fun removeTriplet(
        mutableCells: MutableList<MutableList<Cell>>,
        tripletOnBoard: TripletOnBoard,
        handChanges: MutableList<HandChange>
    ): List<VisualEffect> {
        val pendingEffects: MutableList<VisualEffect> = mutableListOf()
        val rowIndex = tripletOnBoard.row
        val columnIndex = tripletOnBoard.column
        val dx = tripletOnBoard.rowShift
        val dy = tripletOnBoard.columnShift
        if (rowIndex + 2 * dx >= mutableCells.size) {
            return pendingEffects
        }
        if (columnIndex + 2 * dy >= mutableCells[rowIndex].size) {
            return pendingEffects
        }

        (0..2).forEach { shift ->
            val targetRow = rowIndex + shift * dx
            val targetColumn = columnIndex + shift * dy
            val cell = mutableCells[targetRow][targetColumn]
            val cellOwner = cell.owner
            val cellType = cell.type

            val kittenTransformed = if (cellType == CellType.KITTEN) 1 else 0
            handChanges[cellOwner] = handChanges[cellOwner].copy(
                deltaMaxKitten = handChanges[cellOwner].deltaMaxKitten - kittenTransformed,
                deltaCurrentCat = handChanges[cellOwner].deltaCurrentCat + 1,
                deltaMaxCat = handChanges[cellOwner].deltaMaxCat + kittenTransformed
            )

            mutableCells[targetRow][targetColumn] = Cell.emptyCell()
            pendingEffects.add(
                VisualEffect.RemovePiece(
                    row = targetRow,
                    column = targetColumn,
                    owner = cellOwner,
                    type = cellType
                )
            )
        }
        return pendingEffects
    }


    /**
     * Is game already over?
     * Game over happens if either of two coniditions is true:
     * - there are three cats of player #[playerIndex] in the line
     * - there are [maxPieceAmount] cats of player #[playerIndex] on the board
     * Winning situation for other player at this point wouldn't stop the game
     */
    private fun checkForGameOver(
        mutableCells: MutableList<MutableList<Cell>>,
        playerIndex: Int,
        maxPieceAmount: Int
    ): Pair<Boolean, Int> {
        var catCount: Int = 0
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                if (mutableCells[rowIndex][columnIndex].owner == playerIndex) {
                    cellDeltas4directions.forEach { cellDelta ->
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
        return if (catCount == maxPieceAmount) {
            Pair(true, playerIndex)
        } else {
            Pair(false, -1)
        }
    }

    /**
     * Are there three cats from this [rowIndex]x[columnIndex] start in [cellDelta] defined direction
     * that belong to same non-fake player?
     */
    private fun isCatTriplet(
        mutableCells: MutableList<MutableList<Cell>>,
        rowIndex: Int,
        columnIndex: Int,
        cellDelta: Pair<Int, Int>
    ): Boolean {
        val (dx, dy) = cellDelta
        val furthestRowIndex = rowIndex + 2 * dx
        val furthestColumnIndex = columnIndex + 2 * dy
        if (furthestRowIndex >= mutableCells.size || furthestRowIndex < 0) {
            return false
        }
        if (furthestColumnIndex >= mutableCells[rowIndex].size || furthestColumnIndex < 0) {
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


    /**
     * Are there any pieces on board whatsoever?
     */
    fun hasPieces(): Boolean {
        return cells.any { line -> line.any { cell -> cell.type != CellType.EMPTY } }
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

        //cell shifts when we need 4 directions - left, down, left-down, left-up
        val cellDeltas4directions = listOf(
            Pair(1, 0),
            Pair(0, 1),
            Pair(1, 1),
            Pair(1, -1)
        )

        //cell shifts when we need check all 8 directions
        val cellDeltas8directions = listOf(
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, 1),
            Pair(0, -1),
            Pair(1, 1),
            Pair(-1, -1),
            Pair(1, -1),
            Pair(-1, 1)
        )
    }
}