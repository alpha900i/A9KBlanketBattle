package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.GameState

interface Player {
    val index: Int
    fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    )
}

class BotPlayerA(override val index: Int) : Player{
    override fun makeMove(
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
    override fun makeMove(
        gameState: GameState,
        applier: (Move) -> Unit
    ) {
        applier(formMove())
    }
    private fun formMove(): Move {
        return Move(3, 2, MoveType.KITTEN)
    }
}