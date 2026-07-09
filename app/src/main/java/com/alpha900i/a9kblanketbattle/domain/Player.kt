package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import com.alpha900i.a9kblanketbattle.data.TripletOnBoard
import kotlinx.coroutines.CompletableDeferred

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
}

class BotPlayerA(override val index: Int) : Player{
    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
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
                    return Move(rowIndex, colIndex, MoveType.KITTEN)
                }
            }
        }
        return Move(3, 2, MoveType.KITTEN)
    }
}
class HumanPlayer(override val index: Int): Player {
    private var deferredMove: CompletableDeferred<Move>? = null
    private var deferredRemoval: CompletableDeferred<TripletOnBoard>? = null

    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
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
}