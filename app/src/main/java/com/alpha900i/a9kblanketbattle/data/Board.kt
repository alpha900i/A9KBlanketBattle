package com.alpha900i.a9kblanketbattle.data

import com.alpha900i.a9kblanketbattle.data.Cell.Empty
import com.alpha900i.a9kblanketbattle.data.Cell.Piece
import com.alpha900i.a9kblanketbattle.data.PieceType.CAT
import com.alpha900i.a9kblanketbattle.data.PieceType.KITTEN
import com.alpha900i.a9kblanketbattle.domain.HandChange
import com.alpha900i.a9kblanketbattle.domain.Move
import com.alpha900i.a9kblanketbattle.domain.MoveType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Type of single cell
 */
enum class PieceType {
    KITTEN,
    CAT;
}

/**
 * Cell can be [Empty] or [Piece]
 * Piece has a [type] and number of its [owner]
 */
sealed interface Cell {
    object Empty: Cell
    data class Piece(val type: PieceType, val owner: Int) : Cell
}

/**
 * [isPushable] answers, whether current cell can be pushed by piece of given MoveType
 * We can't have it inside of interface, because we want contract and they don't work on open functions
 * and interface has only opens
 */
@OptIn(ExperimentalContracts::class)
fun Cell.isPushable(moveType: MoveType): Boolean {
    contract {
        returns(true) implies (this@isPushable is Piece)
    }
    return when (this) {
        is Empty -> false
        is Piece -> when (moveType) {
            MoveType.SET_CAT -> (type == CAT || type == KITTEN)
            MoveType.SET_KITTEN -> (type == KITTEN)
            else -> false
        }
    }
}
val Cell.typeOrNull: PieceType? get() = (this as? Cell.Piece)?.type
val Cell.ownerOrNull: Int? get() = (this as? Cell.Piece)?.owner

/**
 * Three "interesting" pieces on a board in a row.
 * In practice, they are always of same owner, always non-empty, but can contain different pieces
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
 * [winnerIndex] is "number of player that won". Null if game is not over yet
 * [deletableTriplets] is set of triplets that player can remove from board; player may need to decide, which one to remove
 */
data class MoveResult(
    val board: Board,
    val pendingEffects: List<VisualEffect>,
    val handChanges: List<HandChange>,
    val winnerIndex: Int?,
    val deletableTriplets: Set<TripletOnBoard>
)

/**
 * Rectangular grid of cells
 * [getHeight] and [getWidth] return respective dimension
 * [getEmptyCellCoordinates] returns list of coordinates of indices for empty cells
 * [getPlayerPieceCoordinates] returns list of coordinates of indices for cells belonging to given player
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
                if (cell is Empty) {
                    rowIndex to colIndex
                } else {
                    null
                }
            }
        }
    fun getPlayerPieceCoordinates(index: Int): List<Pair<Int, Int>> =
        cells.flatMapIndexed { rowIndex, row ->
            row.mapIndexedNotNull { colIndex, cell ->
                if (cell is Piece && cell.owner == index) {
                    rowIndex to colIndex
                } else {
                    null
                }
            }
        }
    fun getCellType(rowIndex: Int, colIndex: Int): PieceType? = cells[rowIndex][colIndex].typeOrNull

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

        val winnerIndex = checkForGameOver(
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
        if (deletableTriplets.size == 1 && winnerIndex == null) {
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
            handChanges,      //we wouldn't get here if there was an active "game over", and triplet removal can't initiate one
            null,
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
                val checkCell = mutableCells[checkX][checkY]
                if (checkCell.isPushable(moveType)) {
                    //is cell we are getting pushed to on board?
                    if (receiverX in 0 until getHeight() && receiverY in 0 until getWidth()) {
                        //is cell we are getting pushed empty>
                        if (mutableCells[receiverX][receiverY] is Empty) {
                            mutableCells[receiverX][receiverY] = checkCell
                            mutableCells[checkX][checkY] = Empty
                            pendingEffects.add(
                                VisualEffect.MovePiece(
                                    fromRow = checkX,
                                    fromColumn = checkY,
                                    toRow = receiverX,
                                    toColumn = receiverY,
                                    owner = checkCell.owner,
                                    type = checkCell.type
                                )
                            )
                        }
                    } else {
                        //receiver cell is not on board - time to fall of board
                        val cellOwner = checkCell.owner
                        val cellType = checkCell.type
                        if (cellType == CAT) {
                            handChanges[cellOwner] =
                                handChanges[cellOwner].copy(deltaCurrentCat = handChanges[cellOwner].deltaCurrentCat + 1)
                        }
                        if (cellType == KITTEN) {
                            handChanges[cellOwner] =
                                handChanges[cellOwner].copy(deltaCurrentKitten = handChanges[cellOwner].deltaCurrentKitten + 1)
                        }
                        mutableCells[checkX][checkY] = Empty
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
        val pieceType = if (moveType == MoveType.SET_CAT) {
            CAT
        } else {
            KITTEN
        }
        mutableCells[moveX][moveY] = Piece(
            type = pieceType,
            owner = playerIndex
        )
        if (pieceType == CAT) {
            handChanges[playerIndex] =
                handChanges[playerIndex].copy(deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat - 1)
        }
        if (pieceType == KITTEN) {
            handChanges[playerIndex] =
                handChanges[playerIndex].copy(deltaCurrentKitten = handChanges[playerIndex].deltaCurrentKitten - 1)
        }

        pendingEffects.add(
            VisualEffect.AddPiece(
                row = moveX,
                column = moveY,
                owner = playerIndex,
                type = pieceType
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
        val movedCell = mutableCells[move.row][move.column]
        assert(
            movedCell is Piece && movedCell.type == KITTEN && movedCell.owner == playerIndex
        )

        mutableCells[move.row][move.column] = Empty
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
                type = KITTEN
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
        val movedCell = mutableCells[move.row][move.column]
        assert(
            movedCell is Piece && movedCell.type == CAT && movedCell.owner == playerIndex
        )

        mutableCells[move.row][move.column] = Empty
        handChanges[playerIndex] = handChanges[playerIndex].copy(
            deltaCurrentCat = handChanges[playerIndex].deltaCurrentCat + 1
        )

        return listOf(
            VisualEffect.RemovePiece(
                row = move.row,
                column = move.column,
                owner = playerIndex,
                type = CAT
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
                    val cell = mutableCells[rowIndex][columnIndex]
                    if (
                        isTriplet(
                            mutableCells,
                            rowIndex,
                            columnIndex,
                            cellDelta
                        )
                        && cell is Piece
                        && cell.owner == playerIndex
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

        val firstCell = mutableCells[rowIndex][columnIndex]
        if (firstCell !is Piece) {
            return false
        }
        val firstOwner = firstCell.owner
        (0..2).forEach { shift ->
            val cell = mutableCells[rowIndex + shift * dx][columnIndex + shift * dy]
            if (cell !is Piece) {
                return false
            }
            val cellOwner = cell.owner
            if (cellOwner != firstOwner) {
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
            //this condition can't be true, but inside of this method we are not sure about this
            if (cell is Piece) {
                val cellOwner = cell.owner
                val cellType = cell.type

                val kittenTransformed = if (cellType == KITTEN) 1 else 0
                handChanges[cellOwner] = handChanges[cellOwner].copy(
                    deltaMaxKitten = handChanges[cellOwner].deltaMaxKitten - kittenTransformed,
                    deltaCurrentCat = handChanges[cellOwner].deltaCurrentCat + 1,
                    deltaMaxCat = handChanges[cellOwner].deltaMaxCat + kittenTransformed
                )

                mutableCells[targetRow][targetColumn] = Empty
                pendingEffects.add(
                    VisualEffect.RemovePiece(
                        row = targetRow,
                        column = targetColumn,
                        owner = cellOwner,
                        type = cellType
                    )
                )
            }
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
    ): Int? {
        var catCount: Int = 0
        //for each cell
        //check horizontal/vertical/diagonal triplet to down/right direction
        (0..<mutableCells.size).forEach { rowIndex ->
            (0..<mutableCells[rowIndex].size).forEach { columnIndex ->
                val cell = mutableCells[rowIndex][columnIndex]
                if (cell is Piece && cell.owner == playerIndex) {
                    cellDeltas4directions.forEach { cellDelta ->
                        if (isCatTriplet(
                                mutableCells,
                                rowIndex,
                                columnIndex,
                                cellDelta
                            )
                        ) {
                            return cell.owner
                        }
                    }
                    if (cell.type == CAT) {
                        catCount++
                    }
                }
            }
        }
        return if (catCount == maxPieceAmount) {
            playerIndex
        } else {
            null
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
        val firstCell = mutableCells[rowIndex][columnIndex]
        if (firstCell !is Piece) {
            return false
        }
        val firstOwner = firstCell.owner
        (0..2).forEach { shift ->
            val cell = mutableCells[rowIndex + shift * dx][columnIndex + shift * dy]
            if (cell !is Piece) {
                return false
            }
            val cellOwner = cell.owner
            val cellType = cell.type
            if (cellOwner != firstOwner || cellType != CAT) {
                return false
            }
        }
        return true
    }


    /**
     * Are there any pieces on board whatsoever?
     */
    fun hasPieces(): Boolean {
        return cells.any { line -> line.any { cell -> cell is Piece } }
    }

    companion object {
        fun emptyBoard(width: Int, height: Int): Board {
            val cells = List(height) {
                List(width) {
                    Empty
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