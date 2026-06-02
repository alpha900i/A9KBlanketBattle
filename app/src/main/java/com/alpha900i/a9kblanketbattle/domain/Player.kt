package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState
import kotlinx.coroutines.CompletableDeferred

interface Player {
    val index: Int
    suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    )
    fun submitMove(move: Move) {}
}

class BotPlayerA(override val index: Int) : Player{
    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        applier(formMove(gameState))
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

    override fun submitMove(move: Move) {
        deferredMove?.complete(move)
    }
}