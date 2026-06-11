package com.alpha900i.a9kblanketbattle.domain

import android.util.Log
import com.alpha900i.a9kblanketbattle.data.CellType
import com.alpha900i.a9kblanketbattle.data.GameState

class Game {
    suspend fun makeMove(
        players: List<Player>,
        gameState: GameState,
        stateUpdater: (GameState) -> Unit
    ) {
        players[gameState.activePlayerIndex].makeMove(gameState) { move ->
            applyMove(gameState, move, stateUpdater)
        }
    }

    private fun applyMove(
        gameState: GameState,
        move: Move,
        stateUpdater: (GameState) -> Unit
    ) {
        val (newBoard, handChanges, gameOver) = gameState.board.applyMove(move, gameState.activePlayerIndex)
        //modify hands
        val newHands = gameState.hands.mapIndexed { index, hand ->
            hand.applyChange(handChanges[index])
        }

        val emptyCount = gameState.board.cells.sumOf { row -> row.count { it.type == CellType.EMPTY } }

        //update state
        val newPlayerIndex = (gameState.activePlayerIndex + 1) % PLAYER_COUNT
        val moveIsExpected = emptyCount > 0 && !gameOver
        val newState = GameState(
            board = newBoard,
            hands = newHands,
            activePlayerIndex = newPlayerIndex,
            gameIsActive = moveIsExpected,
        )
        stateUpdater(newState)
    }

    companion object {
        const val PLAYER_COUNT: Int = 2
    }
}