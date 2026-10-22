package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.ui.Constants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Player type; used for dropdowns in "Settings" screen
 */
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

/**
 * Basic interface for all players, human or bots
 * [index] is number of player in current session
 * [makeMove] takes active gameState and applier-callback, that takes move; it should generate new move based on this state and pass it to callback
 * [removeTriplet] takes active gameState and applier-callback, that takes triplet; it should select one of removable triplets to actually remove and pass it to callback
 */
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



/**
 * General bot rules
 * Moves are done with delay - otherwise UI wouldn't be able to work properly on bot-vs-bot matches
 * [formMove] processes hand and selects, whether to make "put piece move" or "remove piece" move. In theory can be overridden, so open
 * [formSetMove] makes "put piece" move
 * [formReturnMove] makes "remove piece" move
 * [selectTripletForRemoval] selects one of removable triplets for removal
 */
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
/**
 * "First-fit" bot
 * When asked to make a move, puts cat (if possible) or kitten (if no cats are here) on first empty cell. Cells are checked from top-left corner, row by row
 * When asked to remove one of triplets, always removes first
 */
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


/**
 * "Random" bot
 * When asked to make a move, puts randomly selected (among available) piece on randomly selected (among empty) cell
 * When asked to remove one of triplets, select randomly among candidates
 * Moves, as with all bots, are done with delay - otherwise UI wouldn't be able to work properly on bot-vs-bot matches
 */
class BotRandom(
    override val index: Int,
    private val random: Random = Random.Default,   // ← added
) : Bot(index = index) {
    override fun formSetMove(gameState: GameState): Move {
        //give we even got here, one of piece counts must be non-zero
        //if we have both cats and kitten - select randomly
        //if we have only one type of piece - select it
        val catCurrent = gameState.hands[index].catCurrent
        val kittenCurrent = gameState.hands[index].kittenCurrent
        val moveType = if (kittenCurrent != 0) {
            if (catCurrent != 0) {
                if (random.nextBoolean()) {
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
        val targetCellNum = random.nextInt(emptyCells.size)
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


/**
 * Human player class
 * Actions here are based on UI interactions and happen asynchronously; that's why Deffereds are used
 * [deferredMove] keeps track on move itself
 * [deferredRemoval] keeps track on "remove one of possible triplets" action
 * [submitMove] - called from UI whenever next move is formed
 * [submitRemoval] - called from UI whenever one of removable triplets is selected for removal
 */
class HumanPlayer(override val index: Int) : Player {
    private var deferredMove: CompletableDeferred<Move>? = null
    private var deferredRemoval: CompletableDeferred<TripletOnBoard>? = null

    /**
     * User doesn't actually need gameState to make a move, but we have interface to fulfill
     * Notice how deferredMove and deferred are used - while we save deferred into deferredMove, we await
     * on deferred, otherwise some race condition can really cause issues.
     * For same reason, we can't null value unless we sure it is still our value
     */
    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        deferredMove?.cancel()
        val deferred = CompletableDeferred<Move>()
        deferredMove = deferred
        try {
            applier(deferred.await())
        } finally {
            if (deferredMove === deferred) {
                deferredMove = null
            }
        }
    }

    override suspend fun removeTriplet(
        gameState: GameState,
        applier: (TripletOnBoard) -> Unit
    ) {
        deferredRemoval?.cancel()
        val deferred = CompletableDeferred<TripletOnBoard>()
        deferredRemoval = deferred
        try {
            applier(deferred.await())
        } finally {
            if (deferredRemoval === deferred) {
                deferredRemoval = null
            }
        }
    }

    fun submitMove(move: Move) {
        deferredMove?.complete(move)
    }

    fun submitRemoval(tripletOnBoard: TripletOnBoard) {
        deferredRemoval?.complete(tripletOnBoard)
    }
}