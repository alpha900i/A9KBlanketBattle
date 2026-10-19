package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.ui.Constants
import com.alpha900i.a9kblanketbattle.util.CustomLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class PlayerType(
    val title: String
) {
    HUMAN(
        title = "Human"
    ),
    BOT_FIRST_FIT(
        title = "First fit Bot"
    ),
    BOT_RANDOM(
        title = "Random-choosing Bot"
    );

    fun makePlayer(index: Int): Player {
        return when (this) {
            HUMAN -> HumanPlayer(index)
            BOT_FIRST_FIT -> BotFirstFit(index)
            BOT_RANDOM -> BotRandom(index)
        }
    }

    companion object {
        fun getAllValues(): Array<PlayerType> {
            return enumValues<PlayerType>()
        }

        fun getByName(name: String): PlayerType {
            return enumValues<PlayerType>().firstOrNull { it.name == name } ?: BOT_FIRST_FIT
        }
    }
}

interface Player {
    val index: Int
    suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    )

    suspend fun removeTriplet(
        gameState: GameState,
        applier: (TripletOnBoard) -> Unit
    )
}

class BotFirstFit(override val index: Int) : Player {
    override suspend fun makeMove(
abstract class Bot(override val index: Int) : Player {
    final override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        delay(Constants.BOT_DELAY_TIME)
        applier(formMove(gameState))
    }

    final override suspend fun removeTriplet(
        gameState: GameState,
        applier: (TripletOnBoard) -> Unit
    ) {
        applier(selectTripletForRemoval(gameState))
    }

    protected open fun formMove(gameState: GameState): Move {
        val catCurrent = gameState.hands[index].catCurrent
        val kittenCurrent = gameState.hands[index].kittenCurrent
        //what do we even puting?
        //if we have pieces - put piece of random type on random place
        //if we have no free pieces - play removal action on random board piece
        return if (catCurrent == 0 && kittenCurrent == 0) {
            formReturnMove(gameState = gameState)
        } else {
            formSetMove(gameState = gameState)
        }
    }
    protected abstract fun formSetMove(gameState: GameState): Move
    protected abstract fun formReturnMove(gameState: GameState): Move
    protected abstract fun selectTripletForRemoval(gameState: GameState): TripletOnBoard
}
class BotFirstFit(override val index: Int) : Bot(index = index) {
    override fun formSetMove(gameState: GameState): Move {
        //give we even got here, one of piece counts must be non-zero
        //if we have cats - set cat, otherwise set kitten
        val catCurrent = gameState.hands[index].catCurrent
        val moveType = if (catCurrent != 0) {
            MoveType.SET_CAT
        } else {
            MoveType.SET_KITTEN
        }
        val emptyCells = gameState.board.getEmptyCellCoordinates()
        check(emptyCells.isNotEmpty()) {
            "FirstFitBot-player tries to set while having no empty cells"
        }
        val (rowIndex, colIndex) = emptyCells[0]
        return Move(rowIndex, colIndex, moveType)
    }

    override fun formReturnMove(gameState: GameState): Move {
        val myCells = gameState.board.getPlayerCellCoordinates(index = index)
        check(myCells.isNotEmpty()) {
            "FirstFitBot-player tries to remove while having no active pieces"
        }
        val (rowIndex, colIndex) = myCells[0]
        val moveType = if (gameState.board.getCellType(rowIndex, colIndex) == CellType.KITTEN) {
            MoveType.PROMOTE_KITTEN
        } else {
            MoveType.RETURN_CAT
        }
        return Move(rowIndex, colIndex, moveType)
    }

    override fun selectTripletForRemoval(gameState: GameState): TripletOnBoard {
        return gameState.deletableTriplets.first()
    }
}

class BotRandom(override val index: Int) : Player {
    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        delay(Constants.BOT_DELAY_TIME)
        applier(formMove(gameState))
    }

    override suspend fun removeTriplet(
        gameState: GameState,
        applier: (TripletOnBoard) -> Unit
    ) {
        applier(selectTripletForRemoval(gameState))
    }

    private fun formMove(gameState: GameState): Move {
        val catCurrent = gameState.hands[index].catCurrent
        val kittenCurrent = gameState.hands[index].kittenCurrent
        //what do we even puting?
        //if we have pieces - put piece of random type on random place
        //if we have no free pieces - play removal action on random board piece
        return if (catCurrent == 0 && kittenCurrent == 0) {
            formReturnMove(gameState = gameState)
        } else {
            formSetMove(gameState = gameState)
        }
    }

    private fun formSetMove(gameState: GameState): Move {
class BotRandom(override val index: Int) : Bot(index = index) {
    override fun formSetMove(gameState: GameState): Move {
        //give we even got here, one of piece counts must be non-zero
        //if we have both cats and kitten - select randomly
        //if we have only one type of piece - select it
        val catCurrent = gameState.hands[index].catCurrent
        val kittenCurrent = gameState.hands[index].kittenCurrent
        val moveType = if (kittenCurrent != 0) {
            if (catCurrent != 0) {
                if (Random.nextBoolean()) {
                    MoveType.SET_KITTEN
                } else {
                    MoveType.SET_CAT
                }
            } else {
                MoveType.SET_KITTEN
            }
        } else {
            MoveType.SET_CAT
        }
        val emptyCells = gameState.board.getEmptyCellCoordinates()
        check(emptyCells.isNotEmpty()) {
            "RandomBot-player tries to set while having no empty cells"
        }
        val targetCellNum = Random.nextInt(emptyCells.size)
        val (rowIndex, colIndex) = emptyCells[targetCellNum]
        return Move(rowIndex, colIndex, moveType)
    }

    override fun formReturnMove(gameState: GameState): Move {
        val myCells = gameState.board.getPlayerCellCoordinates(index = index)
        check(myCells.isNotEmpty()) {
            "RandomBot-player tries to remove while having no active pieces"
        }
        val (rowIndex, colIndex) = myCells.random()
        val moveType = if (gameState.board.getCellType(rowIndex, colIndex) == CellType.KITTEN) {
            MoveType.PROMOTE_KITTEN
        } else {
            MoveType.RETURN_CAT
        }
        return Move(rowIndex, colIndex, moveType)
    }

    override fun selectTripletForRemoval(gameState: GameState): TripletOnBoard {
        return gameState.deletableTriplets.random()
    }
}

class HumanPlayer(override val index: Int) : Player {
    private var deferredMove: CompletableDeferred<Move>? = null
    private var deferredRemoval: CompletableDeferred<TripletOnBoard>? = null

    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        CustomLog.d("Player", "Make move")
        deferredMove?.cancel()
        deferredMove = CompletableDeferred()
        try {
            applier(deferredMove!!.await())
        } finally {
            deferredMove = null   // ✅ clear even if applier throws
        }
    }

    override suspend fun removeTriplet(
        gameState: GameState,
        applier: (TripletOnBoard) -> Unit
    ) {
        deferredRemoval?.cancel()
        deferredRemoval = CompletableDeferred()
        try {
            applier(deferredRemoval!!.await())
        } finally {
            deferredRemoval = null   // ✅ clear even if applier throws
        }
    }

    fun submitMove(move: Move) {
        deferredMove?.complete(move)
    }

    fun submitRemoval(tripletOnBoard: TripletOnBoard) {
        deferredRemoval?.complete(tripletOnBoard)
    }
}