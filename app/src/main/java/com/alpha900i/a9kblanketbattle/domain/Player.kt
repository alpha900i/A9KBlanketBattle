package com.alpha900i.a9kblanketbattle.domain

import android.util.Log
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import com.alpha900i.a9kblanketbattle.ui.Constants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

enum class PlayerType(
    val title: String
) {
    HUMAN(
        title = "Human"
    ),
    BOT_A(
        title = "Bot A"
    );

    fun makePlayer(index: Int): Player {
        return when (this) {
            HUMAN -> HumanPlayer(index)
            BOT_A -> BotPlayerA(index)
        }
    }

    companion object {
        fun getAllValues(): Array<PlayerType> {
            return enumValues<PlayerType>()
        }
        fun getByName(name: String): PlayerType {
            return enumValues<PlayerType>().firstOrNull { it.name == name } ?: BOT_A
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

    fun submitMove(move: Move) {}
    fun submitRemoval(tripletOnBoard: TripletOnBoard) {}
    fun reset() {}
}

class BotPlayerA(override val index: Int) : Player {
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
        applier(gameState.deletableTriplets.first())
    }

    private fun formMove(gameState: GameState): Move {
        gameState.board.cells.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                if (cell.type == CellType.EMPTY) {
                    val moveType = if (gameState.hands[index].catCurrent > 0) {
                        MoveType.SET_CAT
                    } else {
                        MoveType.SET_KITTEN
                    }
                    Log.d("Player","Bot-player $index goes to $rowIndex x $colIndex with $moveType; hand ${gameState.hands[index]}")
                    return Move(rowIndex, colIndex, moveType)
                }
            }
        }
        return Move(3, 2, MoveType.SET_KITTEN)
    }
}

class HumanPlayer(override val index: Int) : Player {
    private var deferredMove: CompletableDeferred<Move>? = null
    private var deferredRemoval: CompletableDeferred<TripletOnBoard>? = null

    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        Log.d("Player", "Make move")
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

    override fun submitMove(move: Move) {
        deferredMove?.complete(move)
    }

    override fun submitRemoval(removal: TripletOnBoard) {
        deferredRemoval?.complete(removal)
    }

    override fun reset() {
        Log.d("Player", "Reset")
        deferredMove?.cancel()
        deferredMove = null
        deferredRemoval?.cancel()
        deferredRemoval = null
    }
}