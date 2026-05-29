package com.alpha900i.a9kblanketbattle.domain

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
        applier(formMove())
    }
    private fun formMove(): Move {
        return Move(1, 2, MoveType.CAT)
    }
}
class BotPlayerB(override val index: Int) : Player{
    override suspend fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        applier(formMove())
    }
    private fun formMove(): Move {
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
        applier(deferredMove!!.await())
    }

    override fun submitMove(move: Move) {
        deferredMove?.complete(move)
        deferredMove = null
    }
}