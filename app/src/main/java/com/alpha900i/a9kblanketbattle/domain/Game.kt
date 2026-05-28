package com.alpha900i.a9kblanketbattle.domain

import com.alpha900i.a9kblanketbattle.data.GameState

class Game {
    val PLAYER_COUNT: Int = 2
    fun makeMove(
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
        //modify board. For now it is wrong logic - just set object to cell
        val (newBoard, handChange) = gameState.board.applyMove(move, gameState.activePlayerIndex)
        //modify hand
        val newHand = gameState.hands[gameState.activePlayerIndex].applyChange(handChange)
        val newHands = gameState.hands.mapIndexed { index, hand ->
            if (index == gameState.activePlayerIndex) {
                newHand
            } else {
                hand
            }
        }


        //update state
        val newPlayerIndex = gameState.activePlayerIndex + 1
        val moveIsExpected = newPlayerIndex < PLAYER_COUNT
        val newState = GameState(
            board = newBoard,
            hands = newHands,
            activePlayerIndex = newPlayerIndex % PLAYER_COUNT,
            moveIsExpected = moveIsExpected,
        )
        stateUpdater(newState)
    }
}